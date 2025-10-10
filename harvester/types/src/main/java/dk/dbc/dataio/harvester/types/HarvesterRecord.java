package dk.dbc.dataio.harvester.types;

import java.nio.charset.Charset;
import java.util.List;

public interface HarvesterRecord<T> {
    byte[] asBytes() throws HarvesterException;
    Charset getCharset();
    void addMember(byte[] memberData) throws HarvesterException;
    List<T> getRecords();
}
