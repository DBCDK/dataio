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
import dk.dbc.marc.reader.XmlParser;
import dk.dbc.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class MarcXchangeToDanMarc2LineFormatConverter extends AbstractToLineFormatConverter {
    private final DanMarc2LineFormatWriter writer = new DanMarc2LineFormatWriter();

    @Override
    public byte[] convert(ChunkItem chunkItem, Charset encodedAs, List<Diagnostic> diagnostics) throws JobStoreException {
       final MarcRecord record;
        try {
            record = new MarcXchangeV1Reader(getChunkItemInputStream(chunkItem), chunkItem.getEncoding())
                    .setProperty(XmlParser.Property.ALLOW_EMPTY_SUBFIELD_CODE, true)
                    .read();
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

    private void replaceControlFields(MarcRecord record) {
        final List<ControlField> controlFields = record.getFields().stream()
                .filter(ControlField.class::isInstance)
                .map(ControlField.class::cast)
                .collect(Collectors.toList());

        for (ControlField controlField : controlFields) {
            record.addField(new DataField().setTag("e01").setInd1('0').setInd2('0')
                    .addSubfield(new SubField()
                            .setCode('b')
                            .setData("felt '" + controlField.getTag() + "'"))
                    .addSubfield(new SubField()
                            .setCode('a')
                            .setData("felt '" + controlField.getTag() + "' mangler delfelter")));
        }
        record.getFields().removeAll(controlFields);
    }
}
