package dk.dbc.dataio.commons.types;

public enum Priority {
    HIGH(7),
    NORMAL(4),
    LOW(1);

    final int value;

    Priority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Priority of(int priority) {
        if (priority < 2) {
            return LOW;
        }
        if (priority > 6) {
            return HIGH;
        }
        return NORMAL;
    }
}
