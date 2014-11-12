package dk.dbc.dataio.harvester.utils.datafileverifier;

/**
 * Verifier helper class for MARC exchange collection record members
 */
public class MarcExchangeRecord {
    private final String id;
    private final int number;

    public MarcExchangeRecord(String id, int number) {
        this.id = id;
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MarcExchangeRecord that = (MarcExchangeRecord) o;

        if (number != that.number) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + number;
        return result;
    }

    @Override
    public String toString() {
        return "MarcExchangeRecordExpectation{" +
                "id='" + id + '\'' +
                ", number=" + number +
                '}';
    }
}
