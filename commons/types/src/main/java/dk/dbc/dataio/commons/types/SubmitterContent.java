package dk.dbc.dataio.commons.types;

import java.io.Serializable;

/**
 * SubmitterContent DTO class.
 *
 * In all essence objects of this class are immutable, but due to GWT serialization
 * issues we cannot have final fields and need a default no-arg constructor.
 */
public class SubmitterContent implements Serializable {
    private static final long serialVersionUID = -2754982619041504537L;

    private /* final */ long number;
    private /* final */ String name;
    private /* final */ String description;

    private SubmitterContent() { }

    public SubmitterContent(long number, String name, String description) {
        this.number = number;
        this.name = name;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public long getNumber() {
        return number;
    }
}
