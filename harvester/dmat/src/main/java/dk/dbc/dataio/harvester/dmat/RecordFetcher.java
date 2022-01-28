package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.rawrepo.record.RecordServiceConnector;

public class RecordFetcher {

    public static String getRecordFor(RecordServiceConnector recordServiceConnector, DMatRecord dMatRecord) {
        // Todo: Get the right record here, just return some pseudo-mock-marcxml-ish data for now
        return "<marcxml><field><subfield>subfielddata</subfield></field></marcxml>";
    }

}
