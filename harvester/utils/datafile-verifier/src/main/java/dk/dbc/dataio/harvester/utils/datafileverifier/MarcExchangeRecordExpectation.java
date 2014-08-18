package dk.dbc.dataio.harvester.utils.datafileverifier;

/**
 * Verifier helper class for MARC exchange collection record member expectations
 */
public class MarcExchangeRecordExpectation {
    private final String id;
    private final int number;

    public MarcExchangeRecordExpectation(String id, int number) {
        this.id = id;
        this.number = number;
    }

    public String getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }
}
