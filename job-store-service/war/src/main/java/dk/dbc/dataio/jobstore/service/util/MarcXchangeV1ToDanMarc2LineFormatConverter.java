package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.reader.MarcReaderException;
import dk.dbc.dataio.marc.reader.MarcXchangeV1Reader;
import dk.dbc.dataio.marc.writer.DanMarc2LineFormatWriter;
import dk.dbc.dataio.marc.writer.MarcWriterException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

public class MarcXchangeV1ToDanMarc2LineFormatConverter implements ChunkItemConverter {
    final DanMarc2LineFormatWriter writer = new DanMarc2LineFormatWriter();

    @Override
    public byte[] convert(ChunkItem chunkItem, Charset encodedAs) throws JobStoreException {
        final MarcRecord record;
        try {
            record = new MarcXchangeV1Reader(getChunkItemInputStream(chunkItem), getChunkItemEncoding(chunkItem)).read();
        } catch (MarcReaderException e) {
            throw new JobStoreException("Error reading chunk item data as MarcXchange", e);
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

    private Charset getChunkItemEncoding(ChunkItem chunkItem) throws JobStoreException {
        try {
            return Charset.forName(chunkItem.getEncoding());
        } catch (Exception e) {
            throw new JobStoreException("Illegal encoding", e);
        }
    }
}
