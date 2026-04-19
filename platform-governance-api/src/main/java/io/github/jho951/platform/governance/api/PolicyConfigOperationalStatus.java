package io.github.jho951.platform.governance.api;

public record PolicyConfigOperationalStatus(State state, String reason) {
    public PolicyConfigOperationalStatus {
        state = state == null ? State.UNKNOWN : state;
        reason = reason == null ? "" : reason.trim();
    }

    public boolean isOperational() {
        return state.isOperational();
    }

    public static PolicyConfigOperationalStatus operational(String reason) {
        return new PolicyConfigOperationalStatus(State.OPERATIONAL, reason);
    }

    public static PolicyConfigOperationalStatus notConfigured(String reason) {
        return new PolicyConfigOperationalStatus(State.NOT_CONFIGURED, reason);
    }

    public static PolicyConfigOperationalStatus unavailable(String reason) {
        return new PolicyConfigOperationalStatus(State.UNAVAILABLE, reason);
    }

    public static PolicyConfigOperationalStatus unknown(String reason) {
        return new PolicyConfigOperationalStatus(State.UNKNOWN, reason);
    }

    @Override
    public String toString() {
        if (reason.isBlank()) {
            return state.name();
        }
        return state.name() + "(" + reason + ")";
    }

    public enum State {
        OPERATIONAL(true),
        NOT_CONFIGURED(false),
        UNAVAILABLE(false),
        UNKNOWN(false);

        private final boolean operational;

        State(boolean operational) {
            this.operational = operational;
        }

        public boolean isOperational() {
            return operational;
        }
    }
}
