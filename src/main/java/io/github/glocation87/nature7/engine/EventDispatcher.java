package io.github.glocation87.nature7.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.event.Event;

final class EventDispatcher {
    private final Map<Class<? extends Event>, List<Consumer<Event>>> listeners = new HashMap<>();
    private final Consumer<Class<? extends Event>> registrar_callback;

    EventDispatcher(Consumer<Class<? extends Event>> registrar_callback) {
        this.registrar_callback = registrar_callback;
    }

    // update listener registry if event is absent
    // populate listener with type -> List<Consumer<Event>>>, each type maps to a list of callbacks
    <E extends Event> void listen(Class<? extends Event> event_type, Consumer<? super E> event_handler) {
        //invoke registrar callback on every .listen call
        registrar_callback.accept(event_type);
        listeners.computeIfAbsent(event_type, key -> new ArrayList<>()).add(event-> event_handler.accept(typeof(event)));
    }

    void dispatch(Class<? extends Event> event_type, Event event) {
        List<Consumer<Event>> list = listeners.get(event_type);
        if (list == null) return;
        for (Consumer<Event> callback : list) {
            callback.accept(event);
        }
    }

    void clear() {
        listeners.clear();
    }
}
