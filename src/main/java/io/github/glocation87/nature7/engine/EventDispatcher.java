package io.github.glocation87.nature7.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.event.Event;

final class EventDispatcher {
    private final Map<Class<? extends Event>, List<Consumer<Event>>> listeners = new HashMap<>();
    private final Consumer<Class<? extends Event>> registrarCallback;

    EventDispatcher(Consumer<Class<? extends Event>> registrarCallback) {
        this.registrarCallback = registrarCallback;
    }

    // update listener registry if event is absent
    // populate listener with type -> List<Consumer<Event>>>, each type maps to a list of callbacks
    <E extends Event> void listen(Class<E> eventType, Consumer<? super E> eventHandler) {
        //invoke registrar callback on every .listen call
        registrarCallback.accept(eventType);
        listeners.computeIfAbsent(eventType, key -> new ArrayList<>()).add(event -> eventHandler.accept(eventType.cast(event)));
    }

    void dispatch(Class<? extends Event> eventType, Event event) {
        List<Consumer<Event>> list = listeners.get(eventType);
        if (list == null) return;
        for (Consumer<Event> callback : list) {
            callback.accept(event);
        }
    }

    void clear() {
        listeners.clear();
    }
}
