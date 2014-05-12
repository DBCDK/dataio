package dk.dbc.dataio.harvester.rawrepo;

import java.nio.charset.Charset;

public interface HarvesterXmlRecord {
    byte[] getData() throws HarvesterException;
    Charset getCharset();
}
