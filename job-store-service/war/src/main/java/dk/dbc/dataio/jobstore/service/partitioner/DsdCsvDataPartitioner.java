package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.jsoup.Jsoup;

import javax.xml.stream.events.Characters;
import java.io.InputStream;

/**
 * Data partitioner for "Den Store Danske" encyclopedia CSV format
 */
public class DsdCsvDataPartitioner extends CsvDataPartitioner {
    /**
     * Creates new instance of DataPartitioner for CSV data
     * @param inputStream stream from which csv records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     * @return new instance of DsdCsvDataPartitioner
     */
    public static DsdCsvDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new DsdCsvDataPartitioner(inputStream, encodingName);
    }

    private DsdCsvDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        super(inputStream, encodingName);
    }

    @Override
    Characters newCharactersEvent(String value) {
        return xmlEventFactory.createCharacters(Jsoup.parse(value).text());
    }
}
