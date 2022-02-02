/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Predicate;

import static dk.dbc.marc.binding.MarcRecord.hasTag;

public class ViafDataPartitioner extends Iso2709DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViafDataPartitioner.class);

    private int skippedCount = 0;

    /**
     * Creates new instance of a Iso2709 DataPartitioner for VIAF records
     * @param inputStream stream from which Iso2709 data to be partitioned can be read
     * @param inputEncoding encoding from job specification
     * @return new instance of VIAF data partitioner
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if given invalid input encoding name
     */
    public static ViafDataPartitioner newInstance(InputStream inputStream, String inputEncoding)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(inputEncoding, "inputEncoding");
        return new ViafDataPartitioner(inputStream, inputEncoding);
    }

    private ViafDataPartitioner(InputStream inputStream, String inputEncoding) {
        super(inputStream, inputEncoding);
    }

    @Override
    public int getAndResetSkippedCount() {
        final int valueBeforeReset = skippedCount;
        skippedCount = 0;
        return valueBeforeReset;
    }

    /**
     * Process the MarcRecord obtained from the input stream.
     * If the MARC record is relevant for DBC and can be written, a result
     * containing chunk item with status SUCCESS and record info is returned.
     * If the MARC record is irrelevant for DBC, an empty result is returned.
     * If an error occurs while writing the record, a result with a chunk
     * item with status FAILURE is returned.
     * @param marcRecord the MarcRecord result from the marcReader.read() method
     * @param marcWriter the MarcWriter implementation used to write the marc record
     * @return data partitioner result
     */
    @Override
    DataPartitionerResult processMarcRecord(MarcRecord marcRecord, MarcWriter marcWriter) {
        ChunkItem chunkItem = null;
        Optional<MarcRecordInfo> recordInfo = Optional.empty();
        try {
            if (marcRecord.getFields(hasTag("700") .and(hasSubFieldValueStartingWith('0', "(DBC)"))) .size() > 0 ||
                    marcRecord.getFields(hasTag("710") .and(hasSubFieldValueStartingWith('0', "(DBC)"))) .size() > 0 ) {
                chunkItem = ChunkItem.successfulChunkItem()
                        .withType(ChunkItem.Type.MARCXCHANGE)
                        .withData(marcWriter.write(marcRecord, encoding));
                recordInfo = marcRecordInfoBuilder.parse(marcRecord);
            } else {
                skippedCount++;
            }
        } catch (MarcWriterException e) {
            LOGGER.error("Exception caught while processing MarcRecord", e);
            chunkItem = ChunkItem.failedChunkItem()
                    .withType(ChunkItem.Type.STRING)
                    .withData(marcRecord.toString())
                    .withDiagnostics(new Diagnostic(
                            Diagnostic.Level.FATAL, e.getMessage(), e));
        }
        return new DataPartitionerResult(chunkItem, recordInfo.orElse(null), positionInDatafile++);
    }

    private static HasSubFieldValueStartingWith hasSubFieldValueStartingWith(Character code, String prefix) {
        return new HasSubFieldValueStartingWith(code, prefix);
    }

    private static class HasSubFieldValueStartingWith implements Predicate<Field> {
        private final Character code;
        private final String prefix;

        private HasSubFieldValueStartingWith(Character code, String prefix) {
            this.code = code;
            this.prefix = prefix;
        }

        @Override
        public boolean test(Field field) {
            if (!(field instanceof DataField)) {
                return false;
            }
            for (SubField subField : ((DataField) field).getSubfields()) {
                if (subField.getCode() == code && subField.getData().startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
