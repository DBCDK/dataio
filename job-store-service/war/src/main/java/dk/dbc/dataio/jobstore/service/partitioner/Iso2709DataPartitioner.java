package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.jobstore.service.util.CharacterEncodingScheme;
import dk.dbc.dataio.jobstore.service.util.MarcRecordInfoBuilder;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Iso2709Iterator;
import dk.dbc.marc.Iso2709IteratorReadError;
import dk.dbc.marc.Iso2709Unpacker;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.writer.MarcWriter;
import dk.dbc.marc.writer.MarcWriterException;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

public class Iso2709DataPartitioner implements DataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Iso2709DataPartitioner.class);

    protected final Iso2709Iterator inputStream;
    final MarcXchangeV1Writer marcWriter;
    final MarcRecordInfoBuilder marcRecordInfoBuilder;

    Charset encoding;
    Charset inputEncoding;

    int positionInDatafile = 0;

    /**
     * Creates new instance of Iso2709 DataPartitioner
     *
     * @param inputStream   stream from which Iso2709 data to be partitioned can be read
     * @param inputEncoding encoding from job specification (latin 1 will be interpreted as danmarc2).
     * @return new instance of default Iso2709 DataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if given invalid input encoding name
     */
    public static Iso2709DataPartitioner newInstance(InputStream inputStream, String inputEncoding)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(inputEncoding, "inputEncoding");
        return new Iso2709DataPartitioner(inputStream, inputEncoding);
    }

    protected Iso2709DataPartitioner(InputStream inputStream, String inputEncoding) {
        final BufferedInputStream bufferedInputStream = getInputStreamAsBufferedInputStream(inputStream);
        this.inputStream = new Iso2709Iterator(bufferedInputStream);
        this.encoding = StandardCharsets.UTF_8;
        this.inputEncoding = CharacterEncodingScheme.charsetOf(inputEncoding);
        if (StandardCharsets.ISO_8859_1.name().equals(this.inputEncoding.name())) {
            this.inputEncoding = new DanMarc2Charset();
        }
        marcWriter = new MarcXchangeV1Writer();
        marcRecordInfoBuilder = new MarcRecordInfoBuilder();
    }

    @Override
    public void drainItems(int itemsToRemove) throws PrematureEndOfDataException {
        if (itemsToRemove < 0) throw new IllegalArgumentException("Unable to drain a negative number of items");
        while (--itemsToRemove >= 0) {
            try {
                inputStream.next();
            } catch (RuntimeException e) {
                final Optional<RuntimeException> prematureEndOfDataException = asPrematureEndOfDataException(e);
                if (prematureEndOfDataException.isPresent()) {
                    throw prematureEndOfDataException.get();
                }
                // we simply swallow non-IOExceptions as they have already been handled in chunk items
            } finally {
                positionInDatafile++;
            }
        }
    }

    @Override
    public Charset getEncoding() {
        return encoding;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getTotalBytesRead();
    }

    @Override
    public Iterator<DataPartitionerResult> iterator() throws UnrecoverableDataException, PrematureEndOfDataException {
        return new Iterator<DataPartitionerResult>() {
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

    /**
     * This method deciphers if the input stream is empty
     *
     * @return true if the input stream is not empty, otherwise false
     * @throws InvalidDataException        if data exists but can not be decoded as 2709
     * @throws PrematureEndOfDataException if reading of data terminated abruptly
     */
    protected boolean hasNextDataPartitionerResult() throws InvalidDataException, PrematureEndOfDataException {
        return !hasEmptyInputStream();
    }

    /**
     * This method builds a new data partitioner result.
     * The data partitioner result contains the chunk item (can be null if the marc record could not be retrieved)
     * and the record result (can be null if a failure occurs while parsing the marc record or if data is deemed invalid).
     *
     * @return data partitioner result
     * @throws InvalidDataException        if unable to retrieve the marc record
     * @throws PrematureEndOfDataException if reading of data terminated abruptly
     */
    protected DataPartitionerResult nextDataPartitionerResult() throws InvalidDataException, PrematureEndOfDataException {
        DataPartitionerResult result;
        if (hasEmptyInputStream()) {
            return DataPartitionerResult.EMPTY;
        }
        final byte[] recordAsBytes = getRecordAsBytes();
        try {
            final MarcRecord marcRecord = getMarcRecord(recordAsBytes);
            if (marcRecord == null) {
                result = DataPartitionerResult.EMPTY;
            } else {
                result = processMarcRecord(marcRecord, marcWriter);
            }
        } catch (Iso2709IteratorReadError e) {
            result = new DataPartitionerResult(ChunkItem.failedChunkItem()
                    .withType(ChunkItem.Type.BYTES)
                    .withData(recordAsBytes)
                    .withDiagnostics(new Diagnostic(
                            Diagnostic.Level.ERROR, e.getMessage(), e)),
                    null, positionInDatafile++);
        }
        return result;
    }

    /**
     * This method checks if the Iso2709Iterator inputStream is empty
     *
     * @return true if the input stream is empty, otherwise false
     * @throws InvalidDataException        if data exists but can not be decoded as 2709
     * @throws PrematureEndOfDataException if reading of data terminated abruptly
     */
    private boolean hasEmptyInputStream() {
        try {
            return !inputStream.hasNext();
        } catch (RuntimeException e) {
            throw asPrematureEndOfDataException(e).orElse(new InvalidDataException(e));
        }
    }

    /**
     * This method evaluates input stream given as input.
     * If the input stream is a BufferedInputStream, the input stream is type cast and returned.
     * If the input stream is not a BufferedInputStream, a new BufferedInputStream is created and returned.
     *
     * @param inputStream to convert
     * @return bufferedInputStream
     */
    private BufferedInputStream getInputStreamAsBufferedInputStream(InputStream inputStream) {
        if (inputStream instanceof BufferedInputStream) {
            return (BufferedInputStream) inputStream;
        }
        return new BufferedInputStream(inputStream);
    }

    /**
     * This method reads the next record from the input stream
     *
     * @return the next record as byte array
     * @throws InvalidDataException        if data exists but can not be decoded as 2709
     * @throws PrematureEndOfDataException if reading of data terminated abruptly
     */
    private byte[] getRecordAsBytes() throws InvalidDataException, PrematureEndOfDataException {
        try {
            return inputStream.next();
        } catch (RuntimeException e) {
            throw asPrematureEndOfDataException(e).orElse(new InvalidDataException(e));
        }
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
    DataPartitionerResult processMarcRecord(MarcRecord marcRecord, MarcWriter marcWriter) {
        ChunkItem chunkItem;
        Optional<MarcRecordInfo> recordInfo = Optional.empty();
        try {
            if (marcRecord.getFields().isEmpty()) {
                chunkItem = ChunkItem.ignoredChunkItem().withData("Empty Record");
            } else {
                chunkItem = ChunkItem.successfulChunkItem()
                        .withType(ChunkItem.Type.MARCXCHANGE)
                        .withData(marcWriter.write(marcRecord, encoding));
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

    /**
     * This method converts a byte array representation of the record into a marc record.
     *
     * @param recordAsBytes byte array representation of the record
     * @return marc record or null if a document could not be created
     * @throws Iso2709IteratorReadError if any error occurs while decoding 2709
     */
    private MarcRecord getMarcRecord(byte[] recordAsBytes) throws Iso2709IteratorReadError {
        try {
            return Iso2709Unpacker.createMarcRecord(recordAsBytes, inputEncoding);
        } catch (Exception e) {
            throw new Iso2709IteratorReadError("Exception caught while decoding 2709: " + e.getMessage(), e);
        }
    }

    private Optional<RuntimeException> asPrematureEndOfDataException(Throwable e) {
        final Throwable cause = e.getCause();
        if (cause != null && cause instanceof IOException) {
            return Optional.of(new PrematureEndOfDataException(cause));
        }
        return Optional.empty();
    }
}
