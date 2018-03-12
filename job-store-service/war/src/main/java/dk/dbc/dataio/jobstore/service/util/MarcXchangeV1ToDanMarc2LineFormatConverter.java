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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class MarcXchangeV1ToDanMarc2LineFormatConverter implements ChunkItemConverter {
    private final DanMarc2LineFormatWriter writer = new DanMarc2LineFormatWriter();

    @Override
    public byte[] convert(ChunkItem chunkItem, Charset encodedAs, List<Diagnostic> diagnostics) throws JobStoreException {
       final MarcRecord record;
        try {
            record = new MarcXchangeV1Reader(getChunkItemInputStream(chunkItem), chunkItem.getEncoding()).read();
        } catch (MarcReaderException e) {
            throw new JobStoreException("Error reading chunk item data as MarcXchange", e);
        }

        if (record != null) {
            replaceControlFields(record);
            addDiagnosticsToMarcRecord(diagnostics, record);
        }

        try {
            return writer.write(record, encodedAs);
        } catch (MarcWriterException e) {
            throw new JobStoreException("Error writing chunk item data as DanMarc2 line format", e);
        }
    }

    private BufferedInputStream getChunkItemInputStream(ChunkItem chunkItem) {
        return new BufferedInputStream(new ByteArrayInputStream(chunkItem.getData()));
    }

    private void addDiagnosticsToMarcRecord(List<Diagnostic> diagnostics, MarcRecord record) {
        if (diagnostics != null) {
            for (Diagnostic diagnostic : diagnostics) {
                DataField dataField = new DataField().setTag("e01").setInd1('0').setInd2('0');
                if (diagnostic.getTag() != null) {
                    dataField.addSubfield(new SubField().setCode('b').setData(diagnostic.getTag()));
                }
                if (diagnostic.getAttribute() != null) {
                    dataField.addSubfield(new SubField().setCode('c').setData(diagnostic.getAttribute()));
                }
                dataField.addSubfield(new SubField().setCode('a').setData(diagnostic.getMessage()));
                record.addField(dataField);
            }
        }
    }

    private void replaceControlFields(MarcRecord record) {
        final List<ControlField> controlFields = record.getFields().stream()
                .filter(ControlField.class::isInstance)
                .map(ControlField.class::cast)
                .collect(Collectors.toList());

        for (ControlField controlField : controlFields) {
            record.addField(new DataField().setTag("e01").setInd1('0').setInd2('0')
                    .addSubfield(new SubField().setCode('b').setData("felt '" + controlField.getTag() + "'"))
                    .addSubfield(new SubField().setCode('a').setData("felt '" + controlField.getTag() + "' mangler delfelter")));
        }

        record.getFields().removeAll(controlFields);
    }
}
