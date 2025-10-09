package dk.dbc.dataio.harvester.types;

import java.nio.charset.Charset;

public interface HarvesterRecord {
    byte[] asBytes() throws HarvesterException;

    Charset getCharset();
}
