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

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.jpa.TransactionScopedPersistenceContext;
import dk.dbc.dataio.jobstore.service.ejb.DatabaseMigrator;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class Iso2709ReorderingDataPartitionerIT {
    private EntityManager entityManager;
    private TransactionScopedPersistenceContext persistenceContext;

    @Before
    public void setupDatabase() throws SQLException {
        final DatabaseMigrator migrator = new DatabaseMigrator()
                .withDataSource(JPATestUtils.getIntegrationTestDataSource());
        migrator.onStartup();
    }

    @Before
    public void setupEntityManager() throws Exception {
        entityManager = JPATestUtils.getIntegrationTestEntityManager();
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        JPATestUtils.clearDatabase(entityManager);
    }

    @Test
    public void testReordering() {
        final LinkedList<Integer> expectedPositions = new LinkedList<>(Arrays.asList(
                2, 7, 6, 4, 1, 5, 3, 0));

        final InputStream resourceAsStream = Iso2709ReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.iso");
        final JobItemReorderer jobItemReorderer = new JobItemReorderer(42, entityManager);

        persistenceContext.run(() -> {
            final Iso2709ReorderingDataPartitioner partitioner = Iso2709ReorderingDataPartitioner
                    .newInstance(resourceAsStream, "latin1", jobItemReorderer);
            int itemNo = 0;
            for (DataPartitionerResult result : partitioner) {
                assertThat("result " + (itemNo++) + " position in datafile",
                        result.getPositionInDatafile(), is(expectedPositions.remove()));
            }});
    }
}
