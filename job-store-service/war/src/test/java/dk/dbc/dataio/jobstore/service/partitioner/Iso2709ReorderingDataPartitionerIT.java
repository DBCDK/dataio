package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.ResultSummary;
import dk.dbc.dataio.commons.partioner.DataPartitionerResult;
import dk.dbc.dataio.commons.partioner.Iso2709ReorderingDataPartitioner;
import dk.dbc.dataio.commons.partioner.JobItemReorderer;
import dk.dbc.dataio.commons.partioner.VolumeAfterParents;
import dk.dbc.dataio.commons.partioner.VolumeIncludeParents;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Iso2709ReorderingDataPartitionerIT  extends AbstractJobStoreIT {

    @org.junit.Test
    public void volumeAfterParentsReordering() {
        final LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 7, 6, 4, 1, 8, 5, 3, 0));

        final InputStream resourceAsStream = Iso2709ReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.iso");
        final JobItemReorderer reorderer = new VolumeAfterParents(42, entityManager);
        assertThat("add collection wrapper flag",
                reorderer.addCollectionWrapper(), is(false));

        final List<ResultSummary> expectedResults = new ArrayList<>(9);
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standalone")));
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

        final List<ResultSummary> results = new ArrayList<>(9);
        persistenceContext.run(() -> {
            final Iso2709ReorderingDataPartitioner partitioner = Iso2709ReorderingDataPartitioner
                    .newInstance(resourceAsStream, "latin1", reorderer);
            int itemNo = 0;
            for (DataPartitionerResult result : partitioner) {
                assertThat("result " + (itemNo++) + " position in datafile",
                        result.getPositionInDatafile(), is(expectedPositions.remove()));
                ResultSummary.of(result)
                        .ifPresent(results::add);
            }
        });
        assertThat("results", results, is(expectedResults));
    }

    @org.junit.Test
    public void volumeIncludeParentsReordering() {
        final LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 7, 1, 5, 8, 3, 4, 0, 6));

        final InputStream resourceAsStream = Iso2709ReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.iso");
        final JobItemReorderer reorderer = new VolumeIncludeParents(42, entityManager);
        assertThat("add collection wrapper flag",
                reorderer.addCollectionWrapper(), is(true));

        final List<ResultSummary> expectedResults = new ArrayList<>(9);
        expectedResults.add(new ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standalone")));
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

        final List<ResultSummary> results = new ArrayList<>(9);
        persistenceContext.run(() -> {
            final Iso2709ReorderingDataPartitioner partitioner = Iso2709ReorderingDataPartitioner
                    .newInstance(resourceAsStream, "latin1", reorderer);
            int itemNo = 0;
            for (DataPartitionerResult result : partitioner) {
                assertThat("result " + (itemNo++) + " position in datafile",
                        result.getPositionInDatafile(), is(expectedPositions.remove()));
                ResultSummary.of(result)
                        .ifPresent(results::add);
            }
        });
        assertThat("results", results, is(expectedResults));
    }
}
