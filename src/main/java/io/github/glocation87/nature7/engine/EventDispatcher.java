package io.github.glocation87.nature7.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

final class EventDispatcher {
    private record Listener(EventPriority priority, Consumer<Event> callback) {
    }

    private final Map<Class<? extends Event>, List<Listener>> listeners = new HashMap<>();
    private final Consumer<Class<? extends Event>> registrarCallback;

    EventDispatcher(Consumer<Class<? extends Event>> registrarCallback) {
        this.registrarCallback = registrarCallback;
    }

    // each type keeps its callbacks sorted by priority, same priority stays in the order it was added
    <E extends Event> void listen(Class<E> eventType, EventPriority priority, Consumer<? super E> eventHandler) {
        registrarCallback.accept(eventType);
        List<Listener> list = listeners.computeIfAbsent(eventType, key -> new ArrayList<>());
        int index = 0;
        while (index < list.size() && list.get(index).priority().getSlot() <= priority.getSlot()) {
            index++;
        }
        list.add(index, new Listener(priority, event -> eventHandler.accept(eventType.cast(event))));
    }

    void dispatch(Class<? extends Event> eventType, Event event) {
        List<Listener> list = listeners.get(eventType);
        if (list == null) return;
        for (Listener listener : list) {
            listener.callback().accept(event);
        }
    }

    void clear() {
        listeners.clear();
    }
}
