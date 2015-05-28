package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Factory interface for creation of instances of DataPartitioner
 */
public interface DataPartitionerFactory {
    /**
     * Creates new DataPartitioner for given input data
     * @param inputStream stream from which data to be partitioned can be read
     * @param encoding encoding of data to be partitioned
     * @return DataPartitioner instance
     */
    DataPartitioner createDataPartitioner(InputStream inputStream, String encoding);

    interface DataPartitioner extends Iterable<String> {
        Charset getEncoding() throws InvalidEncodingException;
        long getBytesRead();
    }
}
