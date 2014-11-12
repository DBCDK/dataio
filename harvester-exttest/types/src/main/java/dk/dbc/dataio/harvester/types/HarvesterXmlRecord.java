package dk.dbc.dataio.harvester.types;

import org.w3c.dom.Document;

import java.nio.charset.Charset;

public interface HarvesterXmlRecord {
    byte[] asBytes() throws HarvesterException;
    Document asDocument() throws HarvesterException;
    Charset getCharset();
}
