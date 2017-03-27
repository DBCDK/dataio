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

import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.jpa.TransactionScopedPersistenceContext;
import dk.dbc.dataio.jobstore.service.ejb.DatabaseMigrator;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class DanMarc2LineFormatReorderingDataPartitionerIT {
    private EntityManager entityManager;
    private TransactionScopedPersistenceContext persistenceContext;

    @Before
    public void setupDatabase() throws SQLException {
        DatabaseMigrator databaseMigrator =new DatabaseMigrator().withDataSource( JPATestUtils.getTestDataSource("testdb") );
        databaseMigrator.onStartup();
    }

    @Before
    public void setupEntityManager() throws Exception {
        entityManager = JPATestUtils.createEntityManagerForIntegrationTest("jobstoreIT");
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        JPATestUtils.clearDatabase(entityManager);
    }

    @Test
    public void testReordering() {
        final Supplier<LinkedList<String>> supplier = LinkedList::new;
        final LinkedList<String> expected = Stream.of(
                ">standalone<",
                "notLineFormat",
                ">standaloneWithout004<",
                ">head<",
                ">section<",
                ">volume<",
                ">volumedelete<",
                ">sectiondelete<",
                ">headdelete<"
        ).collect(Collectors.toCollection(supplier));

        final InputStream resourceAsStream = DanMarc2LineFormatReorderingDataPartitionerIT.class
                .getResourceAsStream("/test-records-reorder-danmarc2.lin");
        final String encoding = "latin1";
        final JobItemReorderer jobItemReorderer = new JobItemReorderer(42, entityManager);

        persistenceContext.run(() -> {
            final DanMarc2LineFormatReorderingDataPartitioner partitioner = DanMarc2LineFormatReorderingDataPartitioner
                    .newInstance(resourceAsStream, encoding, jobItemReorderer);
            int itemNo = 0;
            for (DataPartitionerResult dataPartitionerResult : partitioner) {
                assertThat("Item number " + itemNo++, StringUtil.asString(dataPartitionerResult.getChunkItem().getData()), containsString(expected.removeFirst()));
            }
        });
    }

}
