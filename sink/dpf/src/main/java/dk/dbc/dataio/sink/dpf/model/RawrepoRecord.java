package dk.dbc.dataio.sink.dpf.model;

import dk.dbc.marc.binding.MarcRecord;

public class RawrepoRecord extends AbstractMarcRecord {

    public RawrepoRecord(MarcRecord marcRecord) {
        this.body = marcRecord;
    }

}
