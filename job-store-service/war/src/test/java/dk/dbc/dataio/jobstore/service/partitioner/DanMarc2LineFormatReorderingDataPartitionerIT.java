package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.ResultSummary;
import dk.dbc.dataio.commons.partioner.DanMarc2LineFormatReorderingDataPartitioner;
import dk.dbc.dataio.commons.partioner.DataPartitionerResult;
import dk.dbc.dataio.commons.partioner.JobItemReorderer;
import dk.dbc.dataio.commons.partioner.VolumeAfterParents;
import dk.dbc.dataio.commons.partioner.VolumeIncludeParents;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DanMarc2LineFormatReorderingDataPartitionerIT extends AbstractJobStoreIT {

    @Test
    public void volumeAfterParentsReordering() {
        LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 4, 8, 7, 5, 1, 9, 6, 3, 0));

        InputStream resourceAsStream = DanMarc2LineFormatReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.lin");
        JobItemReorderer reorderer = new VolumeAfterParents(42, entityManager);
        MatcherAssert.assertThat("add collection wrapper flag",
                reorderer.addCollectionWrapper(), CoreMatchers.is(false));

        List<ResultSummary> expectedResults = new ArrayList<>(10);
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standalone")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.FAILURE)
                .withIds(Collections.emptyList()));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standaloneWithout004")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("head")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("section")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("volume")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("volumeParentNotFound")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("volumeDeleted")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("sectionDeleted")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("headDeleted")));

        List<ResultSummary> results = new ArrayList<>(10);
        persistenceContext.run(() -> {
            DanMarc2LineFormatReorderingDataPartitioner partitioner = DanMarc2LineFormatReorderingDataPartitioner
                    .newInstance(resourceAsStream, "latin1", reorderer);
            int itemNo = 0;
            for (DataPartitionerResult result : partitioner) {
                MatcherAssert.assertThat("result " + (itemNo++) + " position in datafile",
                        result.getPositionInDatafile(), CoreMatchers.is(expectedPositions.remove()));
                ResultSummary.of(result)
                        .ifPresent(results::add);
            }
        });
        MatcherAssert.assertThat("results", results, CoreMatchers.is(expectedResults));
    }

    @Test
    public void volumeIncludeParentsReordering() {
        LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 4, 8, 1, 6, 9, 3, 5, 0, 7));

        InputStream resourceAsStream = DanMarc2LineFormatReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.lin");
        JobItemReorderer reorderer = new VolumeIncludeParents(42, entityManager);
        MatcherAssert.assertThat("add collection wrapper flag",
                reorderer.addCollectionWrapper(), CoreMatchers.is(true));

        List<ResultSummary> expectedResults = new ArrayList<>(10);
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standalone")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.FAILURE)
                .withIds(Collections.emptyList()));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standaloneWithout004")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Arrays.asList("volume", "section", "head")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Arrays.asList("volumeDeleted", "headDeleted")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("volumeParentNotFound")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("sectionDeleted")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("section")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("headDeleted")));
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("head")));

        List<ResultSummary> results = new ArrayList<>(10);
        persistenceContext.run(() -> {
            DanMarc2LineFormatReorderingDataPartitioner partitioner = DanMarc2LineFormatReorderingDataPartitioner
                    .newInstance(resourceAsStream, "latin1", reorderer);
            int itemNo = 0;
            for (DataPartitionerResult result : partitioner) {
                MatcherAssert.assertThat("result " + (itemNo++) + " position in datafile",
                        result.getPositionInDatafile(), CoreMatchers.is(expectedPositions.remove()));
                ResultSummary.of(result)
                        .ifPresent(results::add);
            }
        });
        MatcherAssert.assertThat("results", results, CoreMatchers.is(expectedResults));
    }
}
