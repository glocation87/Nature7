package io.github.glocation87.nature7.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

class EventDispatcherTest {

    private static final class TestEvent extends Event {

        private static final HandlerList HANDLERS = new HandlerList();

        @Override
        public HandlerList getHandlers() {
            return HANDLERS;
        }
    }

    private final List<Class<? extends Event>> registered = new ArrayList<>();
    private final EventDispatcher dispatcher = new EventDispatcher(registered::add);

    @Test
    void handsTheEventToItsHandlers() {
        List<TestEvent> received = new ArrayList<>();
        dispatcher.listen(TestEvent.class, received::add);
        TestEvent event = new TestEvent();

        dispatcher.dispatch(TestEvent.class, event);

        assertEquals(1, received.size());
        assertSame(event, received.getFirst());
    }

    @Test
    void runsHandlersInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        dispatcher.listen(TestEvent.class, event -> calls.add("first"));
        dispatcher.listen(TestEvent.class, event -> calls.add("second"));

        dispatcher.dispatch(TestEvent.class, new TestEvent());

        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void tellsTheRouterWhichEventTypesAreNeeded() {
        dispatcher.listen(TestEvent.class, event -> {
        });

        assertTrue(registered.contains(TestEvent.class));
    }

    @Test
    void clearRemovesEveryHandler() {
        List<String> calls = new ArrayList<>();
        dispatcher.listen(TestEvent.class, event -> calls.add("called"));
        dispatcher.clear();

        dispatcher.dispatch(TestEvent.class, new TestEvent());

        assertTrue(calls.isEmpty());
    }
}
