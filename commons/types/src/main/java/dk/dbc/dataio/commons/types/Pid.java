package dk.dbc.dataio.commons.types;

import dk.dbc.invariant.InvariantUtil;

public class Pid {
    public static Pid of(String value) throws NullPointerException, IllegalArgumentException {
        return new Pid(value);
    }

    public enum Type {BIBLIOGRAPHIC_OBJECT, UNIT, WORK}

    private final String value;
    private Type type;
    private Integer agencyId;
    private String bibliographicRecordId;
    private String format;

    private Pid(String value) throws NullPointerException, IllegalArgumentException {
        this.value = InvariantUtil.checkNotNullNotEmptyOrThrow(value, "value");
        parse(value);
    }

    public Type getType() {
        return type;
    }

    public Integer getAgencyId() {
        return agencyId;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Pid pid = (Pid) o;

        return value.equals(pid.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    private void parse(String value) throws IllegalArgumentException {
        final String[] parts = value.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid pid '" + value + "'");
        }
        parseType(parts[0]);
        parseBibliographicRecordId(parts[1]);
    }

    private void parseType(String value) {
        try {
            final String[] parts = notEmpty(value, "type").split("-");
            switch (parts[0].toLowerCase()) {
                case "unit":
                    type = Type.UNIT;
                    break;
                case "work":
                    type = Type.WORK;
                    break;
                default:
                    type = Type.BIBLIOGRAPHIC_OBJECT;
            }
            if (type == Type.BIBLIOGRAPHIC_OBJECT) {
                if (parts.length != 2) {
                    throw new IllegalArgumentException("illegal number of type parts");
                }
                agencyId = parseAgencyId(parts[0]);
                format = notEmpty(parts[1], "format");
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("unable to parse type", e);
        }
    }

    private Integer parseAgencyId(String value) throws IllegalArgumentException {
        agencyId = Integer.parseInt(value);
        if (agencyId < 100000 || agencyId > 999999) {
            throw new IllegalArgumentException("invalid agencyId: " + agencyId);
        }
        return agencyId;
    }

    private void parseBibliographicRecordId(String value) {
        bibliographicRecordId = notEmpty(value.split("_")[0], "bibliographicRecordId");
    }

    private String notEmpty(String value, String field) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("can not be empty: " + field);
        }
        return value;
    }
}
