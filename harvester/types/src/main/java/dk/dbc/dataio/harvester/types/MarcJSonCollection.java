package dk.dbc.dataio.harvester.types;

import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.reader.JsonReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.writer.JsonWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MarcJSonCollection implements HarvesterRecord<MarcBinding> {
    private final List<MarcBinding> records = new ArrayList<>();

    /**
     * @return this marcjson collection as a byte array
     * @throws HarvesterInvalidRecordException if this collection contains no record members
     */
    @Override
    public byte[] asBytes() throws HarvesterException {
        if (records.isEmpty()) {
            throw new HarvesterInvalidRecordException("Empty marcjson collection");
        }
        try {
            return new JsonWriter().writeBindingCollection(records, getCharset());
        } catch (MarcWriterException e) {
            throw new HarvesterException(e);
        }
    }

    /**
     * @return an empty collection
     */
    public byte[] emptyCollection() throws HarvesterException {
        try {
            return new JsonWriter().writeBindingCollection(List.of(), getCharset());
        } catch (MarcWriterException e) {
            throw new HarvesterException(e);
        }
    }

    /**
     * @return marcjson collection encoding, always UTF-8
     */
    @Override
    public Charset getCharset() {
        return StandardCharsets.UTF_8;
    }

    /**
     * Extracts record from the given marcjson document (either as a collection or
     * standalone record) and adds it to this collection
     *
     * @param memberData marcjson document as byte array
     * @throws HarvesterInvalidRecordException If given null-valued memberData argument,
     *                                         if given byte array can not be parsed as marcjson,
     *                                         if given memberData is itself a collection with more than one record.
     */
    public void addMember(byte[] memberData) throws HarvesterException {
        if (memberData == null) {
            throw new HarvesterInvalidRecordException("member data can not be null");
        }
        try {
            JsonReader reader = new JsonReader(new ByteArrayInputStream(memberData));

            MarcBinding record = reader.readBinding();
            if (record == null) throw new HarvesterInvalidRecordException("No marcjson record found");
            records.add(record);
            if (reader.readBinding() != null) throw new HarvesterInvalidRecordException("Given collection contains more than one record");
        } catch (MarcReaderException e) {
            throw new HarvesterInvalidRecordException("member data can not be parsed as marcjson", e);
        }
    }

    public List<MarcBinding> getRecords() {
        return records;
    }
}
