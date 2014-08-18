package dk.dbc.dataio.harvester.utils.datafileverifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Verifier helper class for MARC exchange collection expectations
 */
public class MarcExchangeCollectionExpectation {
    public Set<MarcExchangeRecordExpectation> records;

    public MarcExchangeCollectionExpectation() {
        this.records = new HashSet<>();
    }
}
