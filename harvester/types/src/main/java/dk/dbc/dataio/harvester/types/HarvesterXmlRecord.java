package dk.dbc.dataio.harvester.types;

import java.nio.charset.Charset;

public interface HarvesterXmlRecord {
    byte[] asBytes() throws HarvesterException;

    Charset getCharset();
}
