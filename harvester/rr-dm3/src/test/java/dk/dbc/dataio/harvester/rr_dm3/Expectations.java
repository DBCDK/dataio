package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.harvester.utils.datafileverifier.DM3CollectionExpectation;
import dk.dbc.dataio.harvester.utils.datafileverifier.MarcExchangeRecordExpectation;
import dk.dbc.rawrepo.dto.RecordEntryDTO;
import dk.dbc.rawrepo.dto.RecordIdDTO;

import java.util.stream.Stream;

public class Expectations {
    public static DM3CollectionExpectation of(RecordEntryDTO... records) {
        return recordIds(Stream.of(records).map(RecordEntryDTO::getRecordId));
    }

    public static DM3CollectionExpectation of(RecordIdDTO... records) {
        return recordIds(Stream.of(records));
    }

    public static DM3CollectionExpectation recordIds(Stream<RecordIdDTO> recordIds) {
        MarcExchangeRecordExpectation[] expectations = recordIds.map(r -> new MarcExchangeRecordExpectation(r.getBibliographicRecordId(), r.getAgencyId()))
                .toArray(MarcExchangeRecordExpectation[]::new);
        return new DM3CollectionExpectation(expectations);
    }
}
