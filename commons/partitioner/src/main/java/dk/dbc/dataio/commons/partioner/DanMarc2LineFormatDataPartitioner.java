package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.MarcRecordInfoBuilder;
import dk.dbc.dataio.commons.encoding.CharacterEncodingScheme;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.DanMarc2LineFormatReader;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcReaderInvalidRecordException;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

public class DanMarc2LineFormatDataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanMarc2LineFormatDataPartitioner.class);

    protected final DanMarc2LineFormatReader marcReader;
    protected int positionInDatafile = 0;

    private final ByteCountingInputStream inputStream;
    private final MarcRecordInfoBuilder marcRecordInfoBuilder;
    final MarcXchangeV1Writer marcWriter;
    private DanMarc2Charset danMarc2Charset;

    /**
     * Creates new instance of DanMarc2 LineFormat DataPartitioner
     *
     * @param inputStream       stream from which data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @return new instance of DanMarc2 LineFormat DataPartitioner
     * @throws NullPointerException        if given null-valued argument
     * @throws IllegalArgumentException    if given empty valued encoding argument
     * @throws PrematureEndOfDataException if unable to read sufficient data from inputStream
     */
    public static DanMarc2LineFormatDataPartitioner newInstance(InputStream inputStream, String specifiedEncoding)
            throws NullPointerException, IllegalArgumentException, PrematureEndOfDataException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(specifiedEncoding, "specifiedEncoding");
        return new DanMarc2LineFormatDataPartitioner(inputStream, specifiedEncoding);
    }

    protected DanMarc2LineFormatDataPartitioner(InputStream inputStream, String specifiedEncoding) throws PrematureEndOfDataException {
        this.inputStream = new ByteCountingInputStream(inputStream);
        Charset charset = CharacterEncodingScheme.charsetOf(specifiedEncoding);
        if(StandardCharsets.ISO_8859_1.equals(charset)) {
            this.danMarc2Charset = new DanMarc2Charset(DanMarc2Charset.Variant.LINE_FORMAT);
            try {
                marcReader = new DanMarc2LineFormatReader(this.inputStream, danMarc2Charset);
            } catch (IllegalStateException e) {
                throw new PrematureEndOfDataException(e.getCause());
            }
        } else {
            marcReader = new DanMarc2LineFormatReader(this.inputStream, charset);
        }
        marcWriter = new MarcXchangeV1Writer();
        marcRecordInfoBuilder = new MarcRecordInfoBuilder();
    }

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return StandardCharsets.UTF_8;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getBytesRead();
    }

    @Override
    public Iterator<DataPartitionerResult> iterator() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return hasNextDataPartitionerResult();
            }

            @Override
            public DataPartitionerResult next() {
                return nextDataPartitionerResult();
            }
        };
    }

    protected boolean hasNextDataPartitionerResult() throws PrematureEndOfDataException {
        try {
            return marcReader.hasNext();
        } catch (MarcReaderException e) {
            throw new PrematureEndOfDataException(e);
        }
    }

    protected DataPartitionerResult nextDataPartitionerResult() throws PrematureEndOfDataException {
        DataPartitionerResult result;
        try {
            final MarcRecord marcRecord = marcReader.read();
            if (marcRecord == null) {
                result = DataPartitionerResult.EMPTY;
            } else {
                result = processMarcRecord(marcRecord, marcWriter);
            }
        } catch (MarcReaderException e) {
            if (e instanceof MarcReaderInvalidRecordException) {
                result = new DataPartitionerResult(
                        ChunkItem.failedChunkItem()
                                .withType(ChunkItem.Type.BYTES)
                                .withData(((MarcReaderInvalidRecordException) e).getBytesRead())
                                .withDiagnostics(new Diagnostic(
                                        Diagnostic.Level.ERROR, e.getMessage(), e)),
                        null, positionInDatafile++);
            } else {
                throw new PrematureEndOfDataException(e);
            }
        }
        return result;
    }

    /**
     * Process the MarcRecord obtained from the input stream
     * If the marcRecord contains no fields (special case), a result with a chunk item with status IGNORE is returned
     * If the marcRecord can be written, a result containing chunk item with status SUCCESS and record info is returned
     * If an error occurs while writing the record, a result with a chunk item with status FAILURE is returned
     *
     * @param marcRecord the MarcRecord result from the marcReader.read() method
     * @param marcWriter the MarcWriter implementation used to write the marc record
     * @return data partitioner result
     */
    private DataPartitionerResult processMarcRecord(MarcRecord marcRecord, MarcWriter marcWriter) {
        ChunkItem chunkItem;
        Optional<MarcRecordInfo> recordInfo = Optional.empty();
        try {
            if (marcRecord.getFields().isEmpty()) {
                chunkItem = ChunkItem.ignoredChunkItem()
                        .withData("Empty Record");
            } else {
                chunkItem = ChunkItem.successfulChunkItem()
                        .withType(ChunkItem.Type.MARCXCHANGE)
                        .withData(marcWriter.write(marcRecord, getEncoding()));
                recordInfo = marcRecordInfoBuilder.parse(marcRecord);
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
}
