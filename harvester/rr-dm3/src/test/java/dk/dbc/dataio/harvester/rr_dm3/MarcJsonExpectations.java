package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.utils.datafileverifier.MarcBindingExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcJsonCollectionExpectation;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;

import java.util.stream.Stream;

/**
 * Factory methods for creating marcJson expectations.
 */
public class MarcJsonExpectations {
    private MarcJsonExpectations() {}

    public static MarcJsonCollectionExpectation of(RecordEntryDTO... records) {
        return recordIds(Stream.of(records).map(RecordEntryDTO::getRecordId));
    }

    public static MarcJsonCollectionExpectation of(RecordIdDTO... records) {
        return recordIds(Stream.of(records));
    }

    public static MarcJsonCollectionExpectation recordIds(Stream<RecordIdDTO> recordIds) {
        MarcBindingExpectation[] expectations = recordIds.map(r -> new MarcBindingExpectation(r.getBibliographicRecordId(), r.getAgencyId()))
                .toArray(MarcBindingExpectation[]::new);
        return new MarcJsonCollectionExpectation(expectations);
    }
}
