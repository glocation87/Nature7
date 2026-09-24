package io.github.glocation87.nature7.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StateManagerTest {
    private StateManager machine = new StateManager();

    @Test
    void followsTheNormalLifecycle() {
        assertTrue(machine.canTransitionTo(States.STARTING));
        machine.nextState();
        assertTrue(machine.canTransitionTo(States.ACTIVE));
        machine.nextState();
        assertTrue(machine.canTransitionTo(States.ENDING));
        machine.nextState();
        assertTrue(machine.canTransitionTo(States.WAITING));
        machine.nextState();

    }

    @Test
    void cannotSkipTheCountdown() {
        machine.setState(States.WAITING);
        assertFalse(machine.canTransitionTo(States.ACTIVE));
    }
}
