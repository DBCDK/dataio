/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import static org.junit.Assert.assertThat;

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
    public void testReordering() {
        final LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 4, 8, 7, 5, 1, 9, 6, 3, 0));

        final InputStream resourceAsStream = DanMarc2LineFormatReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.lin");
        final JobItemReorderer jobItemReorderer = new JobItemReorderer(42, entityManager);

        persistenceContext.run(() -> {
            final DanMarc2LineFormatReorderingDataPartitioner partitioner = DanMarc2LineFormatReorderingDataPartitioner
                    .newInstance(resourceAsStream, "latin1", jobItemReorderer);
            int itemNo = 0;
            for (DataPartitionerResult result : partitioner) {
                assertThat("result " + (itemNo++) + " position in datafile",
                        result.getPositionInDatafile(), is(expectedPositions.remove()));
            }});
    }

    @Test
    public void testReorderingIncludingParents() {
        final LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 4, 8, 1, 6, 9, 3, 5, 0, 7));

        final InputStream resourceAsStream = DanMarc2LineFormatReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.lin");
        final JobItemReorderer reorderer = new ParentsIncludingReorderer(42, entityManager);

        final List<DataPartitionerResultTransformer.ResultSummary> expectedResults =
                new ArrayList<>(10);
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standalone")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.FAILURE)
                .withIds(Collections.emptyList()));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("standaloneWithout004")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Arrays.asList("volume", "section", "head")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Arrays.asList("volumeDeleted", "headDeleted")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("volumeParentNotFound")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("sectionDeleted")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("section")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("headDeleted")));
        expectedResults.add(new DataPartitionerResultTransformer.ResultSummary()
                .withStatus(ChunkItem.Status.SUCCESS)
                .withIds(Collections.singletonList("head")));

        final List<DataPartitionerResultTransformer.ResultSummary> results =
                new ArrayList<>(10);
        persistenceContext.run(() -> {
            final DanMarc2LineFormatReorderingDataPartitioner partitioner = DanMarc2LineFormatReorderingDataPartitioner
                    .newInstance(resourceAsStream, "latin1", reorderer);
            int itemNo = 0;
            for (DataPartitionerResult result : partitioner) {
                assertThat("result " + (itemNo++) + " position in datafile",
                        result.getPositionInDatafile(), is(expectedPositions.remove()));
                DataPartitionerResultTransformer.toSummarizedResult(result)
                        .ifPresent(results::add);
            }});
        assertThat("results", results, is(expectedResults));
    }
}
