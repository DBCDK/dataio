/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
     * @return MARC Exchange Collection XML encoding, currently always UTF-8
     */
    @Override
    public Charset getCharset() {
        return charset;
    }

    /**
     * Extracts record from given MARC Exchange document (either as collection or
     * standalone record) and adds it to this collection
     * @param memberData  MARC Exchange document as byte array
     * @throws HarvesterInvalidRecordException If given null-valued memberData argument,
     * if given byte array can not be parsed as marcXchange,
     * if given memberData is itself a collection with more than one record.
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
