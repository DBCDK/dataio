package dk.dbc.dataio.harvester.types;

public class HarvestTaskSelector {
    public static HarvestTaskSelector of(String expression) {
        try {
            final String[] parts = expression.split("=", 2);
            return new HarvestTaskSelector(parts[0].trim(), parts[1].trim());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Illegal selector expression <" + expression + ">", e);
        }
    }

    private final String field;
    private final String value;

    public HarvestTaskSelector(String field, String value) {
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return field + " = " + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HarvestTaskSelector that = (HarvestTaskSelector) o;

        if (field != null ? !field.equals(that.field) : that.field != null) {
            return false;
        }
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = field != null ? field.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
