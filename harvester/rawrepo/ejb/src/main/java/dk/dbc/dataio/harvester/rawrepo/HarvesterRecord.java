package dk.dbc.dataio.harvester.rawrepo;

import java.nio.charset.Charset;

public interface HarvesterRecord {
    byte[] getData();
    Charset getCharset();
}
