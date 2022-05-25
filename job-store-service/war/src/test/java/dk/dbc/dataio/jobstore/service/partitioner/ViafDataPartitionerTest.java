package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.marc.binding.ControlField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static dk.dbc.marc.binding.MarcRecord.hasTag;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ViafDataPartitionerTest extends AbstractPartitionerTestBase {
    @Test(timeout = 5000)
    public void partitioning() throws MarcReaderException {
        final DataPartitioner partitioner = ViafDataPartitioner.newInstance(
                getResourceAsStream("test-records-100-viaf.iso"), "UTF-8");

        final List<DataPartitionerResult> dbcRecords = new ArrayList<>(3);
        int numberOfIterations = 0;
        for (DataPartitionerResult result : partitioner) {
            if (!result.isEmpty()) {
                dbcRecords.add(result);
            }
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(100));
        assertThat("Number of DBC records", dbcRecords.size(), is(3));

        final String firstRecordId = getRecordId(dbcRecords.get(0).getChunkItem());
        assertThat("1st record ID", firstRecordId, is("viaf10150380569313372563"));
        assertThat("1st record pos", dbcRecords.get(0).getPositionInDatafile(), is(58));
        assertThat("1st record info", dbcRecords.get(0).getRecordInfo().getId(), is(firstRecordId));

        final String secondRecordId = getRecordId(dbcRecords.get(1).getChunkItem());
        assertThat("2nd record ID", secondRecordId, is("viaf10151963558500310419"));
        assertThat("2nd record pos", dbcRecords.get(1).getPositionInDatafile(), is(83));
        assertThat("2nd record info", dbcRecords.get(1).getRecordInfo().getId(), is(secondRecordId));

        final String thirdRecordId = getRecordId(dbcRecords.get(2).getChunkItem());
        assertThat("3rd record ID", thirdRecordId, is("viaf10152138513110981276"));
        assertThat("3rd record pos", dbcRecords.get(2).getPositionInDatafile(), is(86));
        assertThat("3rd record info", dbcRecords.get(2).getRecordInfo().getId(), is(thirdRecordId));
    }

    @Test(timeout = 5000)
    public void drain() {
        final DataPartitioner partitioner = ViafDataPartitioner.newInstance(
                getResourceAsStream("test-records-100-viaf.iso"), "UTF-8");

        int numberOfIterations = 60;
        partitioner.drainItems(numberOfIterations);

        final List<DataPartitionerResult> dbcRecords = new ArrayList<>(3);
        for (DataPartitionerResult result : partitioner) {
            if (!result.isEmpty()) {
                dbcRecords.add(result);
            }
            numberOfIterations++;
        }
        assertThat("Number of iterations", numberOfIterations, is(100));
        assertThat("Number of DBC records", dbcRecords.size(), is(2));

        assertThat("1st record pos", dbcRecords.get(0).getPositionInDatafile(), is(83));
        assertThat("2nd record pos", dbcRecords.get(1).getPositionInDatafile(), is(86));
    }

    @Test(timeout = 5000)
    public void skippedCount() {
        final DataPartitioner partitioner = ViafDataPartitioner.newInstance(
                getResourceAsStream("test-records-100-viaf.iso"), "UTF-8");

        DataPartitionerResult firstResult = null;
        for (DataPartitionerResult result : partitioner) {
            if (!result.isEmpty()) {
                firstResult = result;
                break;
            }
        }
        assertThat("record pos", firstResult.getPositionInDatafile(), is(58));
        // Remember that position is zero indexed!
        assertThat("skipped count", partitioner.getAndResetSkippedCount(), is(58));
        assertThat("skipped count after reset", partitioner.getAndResetSkippedCount(), is(0));
    }

    private String getRecordId(ChunkItem chunkItem) throws MarcReaderException {
        final MarcXchangeV1Reader reader = new MarcXchangeV1Reader(
                new ByteArrayInputStream(chunkItem.getData()), StandardCharsets.UTF_8);
        final MarcRecord marcRecord = reader.read();
        return ((ControlField) marcRecord.getField(hasTag("001"))
                .orElse(new ControlField().setData("001 not found"))).getData();
    }
}
