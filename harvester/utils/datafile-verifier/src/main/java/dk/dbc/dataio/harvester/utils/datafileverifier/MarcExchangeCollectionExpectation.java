package dk.dbc.dataio.harvester.utils.datafileverifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Verifier helper class for MARC exchange collection expectations
 */
public class MarcExchangeCollectionExpectation {
    public List<MarcExchangeRecordExpectation> records;

    public MarcExchangeCollectionExpectation() {
        this.records = new ArrayList<>();
    }
}
