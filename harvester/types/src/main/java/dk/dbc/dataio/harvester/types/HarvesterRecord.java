package dk.dbc.dataio.harvester.types;

import dk.dbc.marc.binding.MarcRecord;

import java.nio.charset.Charset;
import java.util.List;

public interface HarvesterRecord {
    byte[] asBytes() throws HarvesterException;
    Charset getCharset();
    void addMember(byte[] memberData) throws HarvesterException;
    List<MarcRecord> getRecords();
}
