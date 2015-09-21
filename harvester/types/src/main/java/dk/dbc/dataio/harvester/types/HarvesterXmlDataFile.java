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

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * This class represents a harvester data file in XML format.
 * <p>
 * The generated file will be enclosed in
 * {@code <dataio-harvester-datafile>...</dataio-harvester-datafile>} tags.
 * </p>
 */
public class HarvesterXmlDataFile implements AutoCloseable {
    private final byte[] header;
    private final byte[] footer;

    final Charset charset;
    final OutputStream outputStream;

    /**
     * Class constructor
     * @param charset character set of data file
     * @param outputStream output stream to which the data file will be written
     * @throws NullPointerException if given null-valued charset or outputStream arguments
     * @throws HarvesterException if unable to write data file header to output stream
     */
    public HarvesterXmlDataFile(Charset charset, OutputStream outputStream) throws NullPointerException, HarvesterException {
        this.charset = InvariantUtil.checkNotNullOrThrow(charset, "charset");
        this.outputStream = InvariantUtil.checkNotNullOrThrow(outputStream, "outputStream");
        header = "<dataio-harvester-datafile>".getBytes(this.charset);
        footer = "</dataio-harvester-datafile>".getBytes(this.charset);
        try {
            outputStream.write(header, 0, header.length);
        } catch (IOException e) {
            throw new HarvesterException("Unable to add header to OutputStream", e);
        }
    }

    /**
     * Adds given record to this data file
     * @param record record representation whose data content will be written to the output stream
     * @throws NullPointerException if given null-valued record argument
     * @throws HarvesterInvalidRecordException if charset of given record does not match charset
     * of data file
     * @throws HarvesterException if unable to write record data to the output stream
     */
    public void addRecord(HarvesterXmlRecord record) throws NullPointerException, HarvesterException {
        InvariantUtil.checkNotNullOrThrow(record, "record");
        if (charset.compareTo(record.getCharset()) != 0) {
            throw new HarvesterInvalidRecordException(String.format("Invalid record - charset mismatch %s != %s",
                    record.getCharset().displayName(), charset.displayName()));
        }
        try {
            final byte[] bytes = record.asBytes();
            outputStream.write(bytes, 0, bytes.length);
        } catch (IOException e) {
            throw new HarvesterException("Unable to add record to OutputStream", e);
        }
    }

    /**
     * Closes this data file by writing footer to output stream
     * @throws HarvesterException if unable to write footer to output stream
     */
    @Override
    public void close() throws HarvesterException {
        try {
            outputStream.write(footer, 0, footer.length);
        } catch (IOException e) {
            throw new HarvesterException("Unable to add footer to OutputStream", e);
        }
    }
}
