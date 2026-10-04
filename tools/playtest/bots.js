// Scripted bots that play real matches against the dev server.
// node bots.js <laststanding|spleef|skywars|ctf|stats>
// Needs online-mode=false, ViaVersion + ViaBackwards on the server (mineflayer speaks 26.1) and BotAlpha/BotBravo opped.
const mineflayer = require("mineflayer");
const { Vec3 } = require("vec3");

const scenario = process.argv[2];
const PORT = Number(process.env.PORT ?? 25566);
const log = (who, msg) => console.log(`[${new Date().toISOString().slice(11, 19)}] ${who}: ${msg}`);
const sleep = ms => new Promise(r => setTimeout(r, ms));

function makeBot(name) {
    const bot = mineflayer.createBot({ host: "localhost", port: PORT, username: name, version: "26.1", auth: "offline" });
    bot.chatLog = [];
    bot.on("messagestr", msg => {
        if (msg.trim()) {
            bot.chatLog.push(msg);
            log(name, "chat> " + msg);
        }
    });
    bot.on("title", (text, type) => log(name, `title(${type})> ${JSON.stringify(text).slice(0, 160)}`));
    bot.on("kicked", reason => log(name, "KICKED " + JSON.stringify(reason)));
    bot.on("error", err => log(name, "ERROR " + err.message));
    bot.on("death", () => log(name, "VANILLA DEATH SCREEN, the death module should have stopped this"));
    // mineflayer still divides velocity by 8000 like the pre 1.21.9 packet, put the real value back
    bot.once("login", () => bot._client.on("entity_velocity", packet => {
        const entity = bot.entities[packet.entityId];
        if (entity) entity.velocity.set(packet.velocity.x, packet.velocity.y, packet.velocity.z);
    }));
    return bot;
}

function waitFor(bot, pattern, timeoutMs) {
    return new Promise((resolve, reject) => {
        if (bot.chatLog.some(m => pattern.test(m))) return resolve();
        const timer = setTimeout(() => {
            bot.removeListener("messagestr", check);
            reject(new Error("timeout waiting for " + pattern));
        }, timeoutMs);
        function check(msg) {
            if (pattern.test(msg)) {
                clearTimeout(timer);
                bot.removeListener("messagestr", check);
                resolve();
            }
        }
        bot.on("messagestr", check);
    });
}

const inv = bot => bot.inventory.items().map(i => `${i.name}x${i.count}`).join(", ") || "(empty)";

// mineflayer can't read 26.x enchantment data when it times a dig, so send the dig packets directly
let digSequence = 1000;
async function rawDig(bot, block) {
    for (const status of [0, 2]) {
        bot._client.write("block_dig", { status, location: block.position, face: 1, sequence: digSequence++ });
        await sleep(100);
    }
}

async function spawned(bot) {
    await new Promise(r => bot.once("spawn", r));
    await sleep(1500);
}

async function start(bots, id) {
    for (const bot of bots) {
        bot.chatLog = [];
        bot.chat(`/n7 join ${id}`);
        await sleep(1200);
    }
    log(bots[0].username, "waiting inventory: " + inv(bots[0]));
    bots[0].chat("/n7 forcestart");
    await waitFor(bots[0], /^Go!/, 40000);
    await sleep(1500);
}

// a win (every zombie dies), then a loss (the player dies and becomes a spectator)
async function lastStanding(a) {
    await start([a], "last_standing");
    await waitFor(a, /Survive 10 zombies/, 5000);
    log(a.username, `game inventory: ${inv(a)}, health ${a.health}`);
    a.chat("/kill @e[type=minecraft:zombie]");
    await sleep(7500);
    log(a.username, "after win inventory: " + inv(a));

    await start([a], "last_standing");
    a.chat("/kill @s");
    await waitFor(a, /eliminated/, 5000);
    await sleep(500);
    log(a.username, `eliminated: inventory ${inv(a)}, gamemode ${a.game.gameMode}`);
    await sleep(7000);
    log(a.username, "after loss inventory: " + inv(a));
}

// digging is blocked during the grace period, works after it, and dropping into the pit eliminates you
async function spleef(a, b) {
    await start([a, b], "spleef");
    log(a.username, "game inventory: " + inv(a));
    const nextTo = () => a.blockAt(a.entity.position.offset(1, -1, 0));
    const early = nextTo();
    await rawDig(a, early);
    await sleep(500);
    log(a.username, "dig during grace: block is " + a.blockAt(early.position).name);
    await sleep(3000);
    const later = nextTo();
    await rawDig(a, later);
    await sleep(500);
    log(a.username, "dig after grace: block is " + a.blockAt(later.position).name + ", inventory " + inv(a));
    a.chat(`/execute as ${b.username} at @s run tp @s ~ ~-6 ~`);
    await waitFor(a, new RegExp(b.username + " was eliminated"), 8000);
    await sleep(7000);
    log(a.username, "after match inventory: " + inv(a));
}

// kits at the start, loot in the island chest, a player leaving hands the win to the other
async function skywars(a, b) {
    await start([a, b], "skywars");
    log(a.username, "game inventory: " + inv(a));
    const chest = a.findBlock({ matching: block => block.name === "chest", maxDistance: 6 });
    if (chest) {
        const window = await a.openContainer(chest);
        log(a.username, `island chest: ${window.containerItems().map(i => i.name + "x" + i.count).join(", ")}`);
        window.close();
    } else {
        log(a.username, "no chest within 6 blocks");
    }
    b.chat("/n7 leave");
    await sleep(8000);
    log(a.username, "after match inventory: " + inv(a));
}

// teleporting doesn't fire a move event, so land a few blocks short and walk the rest
async function walkTo(bot, x, z, fromZ) {
    bot.chat(`/tp @s ${x} 65 ${fromZ}`);
    await sleep(1200);
    await bot.lookAt(new Vec3(x, 65.5, z), true);
    bot.setControlState("forward", true);
    await sleep(900);
    bot.setControlState("forward", false);
    await sleep(400);
}

// three flag runs for a team win, flags at z = +28 (red) and -28 (blue) on lms-maps ctf maps
async function ctf(a, b) {
    await start([a, b], "capture_the_flag");
    const red = a.chatLog.some(m => /You are on the Red team/.test(m));
    log(a.username, "team " + (red ? "red" : "blue") + ", inventory: " + inv(a));
    const own = red ? 28 : -28;
    const enemy = -own;
    for (let round = 1; round <= 3; round++) {
        await walkTo(a, 0.5, enemy, enemy + Math.sign(own) * 3);
        await walkTo(a, 0.5, own, own - Math.sign(own) * 3);
    }
    await sleep(8000);
    log(a.username, "after match inventory: " + inv(a));
}

async function stats(a) {
    a.chat("/n7 stats");
    const window = await new Promise(r => a.once("windowOpen", r));
    for (const item of window.slots.filter(s => s && s.slot < window.inventoryStart)) {
        const lore = (item.components ?? []).filter(c => c.type === "lore").flatMap(c => c.data)
            .map(line => line.value.text.value).join(" | ");
        log(a.username, `stats ${item.name}: ${lore}`);
    }
}

(async () => {
    const a = makeBot("BotAlpha");
    await spawned(a);
    let b = null;
    if (["spleef", "skywars", "ctf"].includes(scenario)) {
        b = makeBot("BotBravo");
        await spawned(b);
    }
    try {
        if (scenario === "laststanding") await lastStanding(a);
        else if (scenario === "spleef") await spleef(a, b);
        else if (scenario === "skywars") await skywars(a, b);
        else if (scenario === "ctf") await ctf(a, b);
        else await stats(a);
    } catch (e) {
        log("script", "FAILED " + e.message);
    }
    a.quit();
    if (b) b.quit();
    await sleep(1000);
    process.exit(0);
})();
