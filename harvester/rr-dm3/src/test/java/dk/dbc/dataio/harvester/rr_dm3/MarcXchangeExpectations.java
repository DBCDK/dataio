package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.utils.datafileverifier.MarcXchangeCollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcXchangeRecordExpectation;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;

import java.util.stream.Stream;

/**
 * Factory methods for creating marcXchange expectations.
 */
public class MarcXchangeExpectations {
    private MarcXchangeExpectations() {}

    public static MarcXchangeCollectionExpectation of(RecordEntryDTO... records) {
        return recordIds(Stream.of(records).map(RecordEntryDTO::getRecordId));
    }

    public static MarcXchangeCollectionExpectation of(RecordIdDTO... records) {
        return recordIds(Stream.of(records));
    }

    public static MarcXchangeCollectionExpectation recordIds(Stream<RecordIdDTO> recordIds) {
        MarcXchangeRecordExpectation[] expectations = recordIds.map(r -> new MarcXchangeRecordExpectation(r.getBibliographicRecordId(), r.getAgencyId()))
                .toArray(MarcXchangeRecordExpectation[]::new);
        return new MarcXchangeCollectionExpectation(expectations);
    }
}
