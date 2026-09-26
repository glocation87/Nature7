package io.github.glocation87.nature7.engine;

public final class StateManager {
    private States previousState;
    private States currentState;

    StateManager() {
        previousState = States.DISPOSED;
        currentState = States.WAITING;
    }

    public States nextState() {
        // DISPOSED is the last state, indexing past it would throw ArrayIndexOutOfBoundsException
        if (currentState == States.DISPOSED) {
            throw new IllegalStateException("Cannot jump from DISPOSED state");
        }
        return jumpTo(States.values()[currentState.ordinal() + 1]);
    }

    public States getCurrentState() {
        return currentState;
    }

    public States getPreviousState() {
        return previousState;
    }

    public boolean canTransitionTo(States newState) {
        if (this.getCurrentState() == States.DISPOSED) {
            return false;
        }
        if (newState == States.DISPOSED && this.getCurrentState() != States.ENDING) {
            return false;
        }
        return canJumpTo(newState);
    }

    private boolean canJumpTo(States newState) {
        switch (newState) {
            case States.WAITING:
                return this.getCurrentState() == States.ENDING;
            case States.STARTING:
                return this.getCurrentState() == States.WAITING;
            case States.DISPOSED:
                return true;
            case States.ACTIVE:
                return this.getCurrentState() == States.STARTING;
            case States.ENDING:
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
            previousState = currentState;
            currentState = newState;
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
        previousState = currentState;
        currentState = newState;
    }
}
