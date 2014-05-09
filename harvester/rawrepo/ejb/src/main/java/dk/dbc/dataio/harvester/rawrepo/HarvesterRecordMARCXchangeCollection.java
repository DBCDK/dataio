package dk.dbc.dataio.harvester.rawrepo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HarvesterRecordMARCXchangeCollection implements HarvesterRecord {
    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }
}
