package dk.dbc.dataio.commons.partioner;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.MarcRecordInfoBuilder;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.InvalidRecordException;
import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import dk.dbc.dataio.jobstore.types.UnrecoverableDataException;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcReaderInvalidRecordException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

/**
 * A partitioner of Addi records read from an {@link InputStream} containing MarcXchange documents
 * <pre>
 * {@code
 *
 * final DataPartitioner dataPartitioner = MarcXchangeAddiDataPartitioner.newInstance(inputStream, encoding);
 * for(DataPartitionerResult recordWrapper : dataPartitioner) {
 *     // do something with record.
 * }
 * }
 * </pre>
 * As can be seen in the above example, the MarcXchangeAddiDataPartitioner.newInstance() method returns
 * a {@link DataPartitioner}, enabling you to step through the results one at a time.
 * Also note, that if a fatal error occurs while reading the input stream, a {@link UnrecoverableDataException} or
 * sub type thereof is thrown. {@link UnrecoverableDataException} is a {@link RuntimeException} since the
 * {@link Iterable} interface all DataPartitioner implementations must implement does not allow checked exceptions
 * to be thrown.
 */
public class MarcXchangeAddiDataPartitioner extends AddiDataPartitioner {
    /**
     * Creates new instance of DataPartitioner for Addi records containing marcXchange content
     *
     * @param inputStream  stream from which addi records can be read
     * @param encodingName encoding specified in job specification
     * @return new instance of MarcXchangeAddiDataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument or if given stream is incompatible with AddiReader
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     */
    public static MarcXchangeAddiDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new MarcXchangeAddiDataPartitioner(inputStream, encodingName);
    }

    private MarcXchangeAddiDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        super(inputStream, encodingName);
    }

    @Override
    ChunkItem.Type[] getChunkItemType() {
        return new ChunkItem.Type[]{ChunkItem.Type.ADDI, ChunkItem.Type.MARCXCHANGE};
    }

    @Override
    Optional<RecordInfo> getRecordInfo(AddiMetaData addiMetaData, AddiRecord addiRecord)
            throws InvalidRecordException {
        if (addiMetaData.diagnostic() == null && addiRecord.getContentData() != null) {
            try {
                final MarcXchangeV1Reader marcReader = new MarcXchangeV1Reader(getInputStream(addiRecord.getContentData()), getEncoding());
                final MarcRecordInfoBuilder marcRecordInfoBuilder = new MarcRecordInfoBuilder();
                final Optional<MarcRecordInfo> marcRecordInfo = marcRecordInfoBuilder.parse(marcReader.read());
                if (marcRecordInfo.isPresent()) {
                    return Optional.of(marcRecordInfo.get());
                }
            } catch (MarcReaderInvalidRecordException e) {
                throw new InvalidRecordException(e.getMessage(), e);
            } catch (MarcReaderException e) {
                throw new IllegalArgumentException("MARC record info could not be created. ", e);
            }
        }
        return super.getRecordInfo(addiMetaData, addiRecord);
    }

    private BufferedInputStream getInputStream(byte[] data) {
        return new BufferedInputStream(new ByteArrayInputStream(data));
    }
}
