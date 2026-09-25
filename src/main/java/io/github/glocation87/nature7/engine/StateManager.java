package io.github.glocation87.nature7.engine;

enum States {
    WAITING,
    STARTING,
    ACTIVE,
    ENDING,
    DISPOSED;
}

public final class StateManager {
    StateManager() {
        previous_state = States.DISPOSED;
        current_state = States.WAITING;
    }
    private States previous_state;
    private States current_state;

    public States nextState() {
        States _nextState;
        if (current_state.ordinal() >= States.values().length) {
            _nextState = States.DISPOSED;
        }
        _nextState = States.values()[current_state.ordinal() + 1];
        return jumpTo(_nextState);
    }

    public States getCurrentState() {
        return current_state;
    }

    public States getPreviousState() {
        return previous_state;
    }

    public boolean canTransitionTo(States newState) {
        if (this.getCurrentState() == States.DISPOSED) {
            return false;
        }
        if (newState == States.DISPOSED && this.getCurrentState() != States.ENDING) {
            return false;
        }
        return canJumpTo(newState);
    };

    private boolean canJumpTo(States newState) {
        switch (newState) {
            case (States.WAITING):
                return this.getCurrentState() == States.ENDING;
            case (States.STARTING):
                return this.getCurrentState() == States.WAITING;
            case (States.DISPOSED):
                return true;
            case (States.ACTIVE):
                return this.getCurrentState() == States.STARTING;
            case (States.ENDING):
                return this.getCurrentState() == States.ACTIVE;
            default:
                return false;
        }
    }

    private States jumpTo(States newState) {
        if (this.getCurrentState() == States.DISPOSED) {
            throw new IllegalStateException("Cannot jump from DISPOSED state");
        }
        if (newState == States.DISPOSED && this.getCurrentState() != States.ENDING) {
            throw new IllegalStateException("Can only jump to DISPOSED from ENDING state");
        }

        if (canJumpTo(newState)) {
            current_state = newState;
            return newState;
        }
        throw new IllegalStateException("Cannot jump from " + this.getCurrentState().name() + " to " + newState.name());
    }

    //TODO: Expose conditional illegal jumps to a new state from any state without following the ruleset
    public void setState(States newState) {
        if (this.getCurrentState() == States.DISPOSED) {
            throw new IllegalStateException("Cannot jump from DISPOSED state");
        }
        if (newState == States.DISPOSED && this.getCurrentState() != States.ENDING) {
            throw new IllegalStateException("Can only jump to DISPOSED from ENDING state");
        }
        previous_state = current_state;
        current_state = newState;
    }
}
