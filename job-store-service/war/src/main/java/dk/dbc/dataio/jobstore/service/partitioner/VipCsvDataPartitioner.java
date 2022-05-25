package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.apache.commons.csv.CSVFormat;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Data partitioner for "Kulturstyrelsen VIP" data CSV format
 * withDelimiter('|')
 * withQuote('"')
 * withRecordSeparator("\r\n")
 * withIgnoreEmptyLines(true)
 */
public class VipCsvDataPartitioner extends CsvDataPartitioner {
    private static final Set<String> HEADERS = new HashSet<>();

    static {
        HEADERS.add("Feltnavn|Kodevaerdi|Kodetekst");
    }

    /**
     * Creates new instance of DataPartitioner for VIP CSV data
     *
     * @param inputStream  stream from which csv records can be read
     * @param encodingName encoding specified in job specification
     * @return new instance of VipCsvDataPartitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     */
    public static VipCsvDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new VipCsvDataPartitioner(inputStream, encodingName);
    }

    private VipCsvDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        super(inputStream, encodingName);
        csvFormat = CSVFormat.DEFAULT.withDelimiter('|');
    }

    @Override
    DataPartitionerResult getResultFromCsvRecord(String csvLine) {
        if (HEADERS.contains(csvLine)) {
            return DataPartitionerResult.EMPTY;
        }
        return super.getResultFromCsvRecord(csvLine);
    }
}
