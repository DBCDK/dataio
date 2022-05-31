package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.marc.reader.XmlParser;
import dk.dbc.marc.writer.LineFormatWriter;
import dk.dbc.marc.writer.MarcWriterException;

import java.nio.charset.Charset;
import java.util.List;

public class MarcXchangeToMarc21LineFormatConverter extends AbstractToLineFormatConverter {
    private final LineFormatWriter writer;

    public MarcXchangeToMarc21LineFormatConverter() {
        writer = new LineFormatWriter();
        writer.setProperty(LineFormatWriter.Property.INCLUDE_LEADER, false);
    }

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
            addDiagnosticsToMarcRecord(diagnostics, record);
        }

        try {
            return writer.write(record, encodedAs);
        } catch (MarcWriterException e) {
            throw new JobStoreException("Error writing chunk item data as DanMarc2 line format", e);
        }
    }
}
