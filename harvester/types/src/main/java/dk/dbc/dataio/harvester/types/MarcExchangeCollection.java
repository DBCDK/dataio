package dk.dbc.dataio.harvester.types;

import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.MarcXchangeV1Writer;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class represents a MARC Exchange Collection as a harvester XML record.
 * <p>
 * This class is not thread safe.
 */
public class MarcExchangeCollection implements HarvesterXmlRecord {
    private final Charset charset = StandardCharsets.UTF_8;
    private final ArrayList<MarcRecord> records;

    public MarcExchangeCollection() {
        this.records = new ArrayList<>();
    }

    /**
     * @return this MARC Exchange Collections XML representation as byte array
     * @throws HarvesterInvalidRecordException if collection contains no record members
     */
    @Override
    public byte[] asBytes() throws HarvesterException {
        if (records.isEmpty()) {
            throw new HarvesterInvalidRecordException("Empty marcXchange collection");
        }
        return new MarcXchangeV1Writer().writeCollection(records, charset);
    }

    /**
     * @return an empty collection, only containing the outer wrapper elements for the collection
     */
    public byte[] emptyCollection() {
        return new MarcXchangeV1Writer().writeCollection(Collections.emptyList(), charset);
    }

    /**
     * @return MARC Exchange Collection XML encoding, currently always UTF-8
     */
    @Override
    public Charset getCharset() {
        return charset;
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
            final MarcXchangeV1Reader marcReader = new MarcXchangeV1Reader(
                    new BufferedInputStream(
                            new ByteArrayInputStream(memberData)), charset);
            final MarcRecord record = marcReader.read();
            if (record == null) {
                throw new HarvesterInvalidRecordException("No marcXchange record found");
            }
            records.add(record);
            if (marcReader.read() != null) {
                throw new HarvesterInvalidRecordException("Given collection contains more than one record");
            }
        } catch (MarcReaderException e) {
            throw new HarvesterInvalidRecordException("member data can not be parsed as marcXchange", e);
        }
    }

    public ArrayList<MarcRecord> getRecords() {
        return records;
    }
}
