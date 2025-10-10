package dk.dbc.dataio.harvester.types;

import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.JsonReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.writer.JsonWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MarcJSonCollection implements HarvesterRecord {
    private final List<MarcRecord> records = new ArrayList<>();

    /**
     * @return this MARC Exchange Collections XML representation as byte array
     * @throws HarvesterInvalidRecordException if collection contains no record members
     */
    @Override
    public byte[] asBytes() throws HarvesterException {
        if (records.isEmpty()) {
            throw new HarvesterInvalidRecordException("Empty marcXchange collection");
        }
        try {
            return new JsonWriter().writeCollection(records, getCharset());
        } catch (MarcWriterException e) {
            throw new HarvesterException(e);
        }
    }

    /**
     * @return an empty collection, only containing the outer wrapper elements for the collection
     */
    public byte[] emptyCollection() throws HarvesterException {
        try {
            return new JsonWriter().writeCollection(List.of(), getCharset());
        } catch (MarcWriterException e) {
            throw new HarvesterException(e);
        }
    }

    /**
     * @return MARC Exchange Collection XML encoding, currently always UTF-8
     */
    @Override
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    /**
     * Extracts record from given MARC Exchange document (either as collection or
     * standalone record) and adds it to this collection
     *
     * @param memberData MARC Exchange document as byte array
     * @throws HarvesterInvalidRecordException If given null-valued memberData argument,
     *                                         if given byte array can not be parsed as marcXchange,
     *                                         if given memberData is itself a collection with more than one record.
     */
    public void addMember(byte[] memberData) throws HarvesterException {
        if (memberData == null) {
            throw new HarvesterInvalidRecordException("member data can not be null");
        }
        try {
            JsonReader reader = new JsonReader(new ByteArrayInputStream(memberData));

            MarcRecord record = reader.read();
            if (record == null) throw new HarvesterInvalidRecordException("No marcXchange record found");
            records.add(record);
            if (reader.read() != null) throw new HarvesterInvalidRecordException("Given collection contains more than one record");
        } catch (MarcReaderException e) {
            throw new HarvesterInvalidRecordException("member data can not be parsed as marcXchange", e);
        }
    }

    public List<MarcRecord> getRecords() {
        return records;
    }
}
