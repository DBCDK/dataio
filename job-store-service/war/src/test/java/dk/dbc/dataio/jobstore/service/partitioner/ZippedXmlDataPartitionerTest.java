package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.marc.reader.MarcReaderException;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippedXmlDataPartitionerTest extends AbstractPartitionerTestBase {

    @Test(timeout = 5000)
    public void partitioning() {

        final DataPartitioner partitioner = ZippedXmlDataPartitioner.newInstance(
                getResourceAsStream("test-records-ebsco-zipped.zip"), "UTF-8");

        final List<DataPartitionerResult> ebscoRecords = new ArrayList<>(5);
        int numberOfIterations = 0;
        for (DataPartitionerResult result : partitioner) {
            if (!result.isEmpty()) {
                ebscoRecords.add(result);
            }
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(5));
    }

    // Todo: Add test that checks for record id in partitioned data
}
