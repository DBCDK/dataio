package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.jpa.TransactionScopedPersistenceContext;
import dk.dbc.dataio.jobstore.service.ejb.DatabaseMigrator;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DanMarc2LineFormatReorderingDataPartitionerIT {
    private EntityManager entityManager;
    private TransactionScopedPersistenceContext persistenceContext;

    @Before
    public void setupDatabase() throws SQLException {
        final DatabaseMigrator migrator = new DatabaseMigrator()
                .withDataSource(JPATestUtils.getIntegrationTestDataSource());
        migrator.onStartup();
    }

    @Before
    public void setupEntityManager() {
        entityManager = JPATestUtils.getIntegrationTestEntityManager();
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        JPATestUtils.clearDatabase(entityManager);
    }

    @Test
    public void volumeAfterParentsReordering() {
        final LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 4, 8, 7, 5, 1, 9, 6, 3, 0));

        final InputStream resourceAsStream = DanMarc2LineFormatReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.lin");
        final JobItemReorderer reorderer = new VolumeAfterParents(42, entityManager);
        assertThat("add collection wrapper flag",
                reorderer.addCollectionWrapper(), is(false));

        final List<ResultSummary> expectedResults = new ArrayList<>(10);
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

        final List<ResultSummary> results = new ArrayList<>(10);
        persistenceContext.run(() -> {
            final DanMarc2LineFormatReorderingDataPartitioner partitioner = DanMarc2LineFormatReorderingDataPartitioner
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

    @Test
    public void volumeIncludeParentsReordering() {
        final LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 4, 8, 1, 6, 9, 3, 5, 0, 7));

        final InputStream resourceAsStream = DanMarc2LineFormatReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.lin");
        final JobItemReorderer reorderer = new VolumeIncludeParents(42, entityManager);
        assertThat("add collection wrapper flag",
                reorderer.addCollectionWrapper(), is(true));

        final List<ResultSummary> expectedResults = new ArrayList<>(10);
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

        final List<ResultSummary> results = new ArrayList<>(10);
        persistenceContext.run(() -> {
            final DanMarc2LineFormatReorderingDataPartitioner partitioner = DanMarc2LineFormatReorderingDataPartitioner
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
