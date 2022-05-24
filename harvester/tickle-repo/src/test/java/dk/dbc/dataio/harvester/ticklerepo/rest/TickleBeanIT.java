/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.ticklerepo.rest;


import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.harvester.ticklerepo.IntegrationTest;
import dk.dbc.ticklerepo.TickleRepo;
import dk.dbc.ticklerepo.dto.Batch;
import dk.dbc.ticklerepo.dto.DataSet;
import dk.dbc.ticklerepo.dto.Record;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TickleBeanIT extends IntegrationTest {
    private TickleBean tickleBean;

    @Before
    public void createTickleBean() {
        tickleBean = new TickleBean();
        tickleBean.tickleRepo = new TickleRepo(
                environment.get("ticklerepo").getEntityManager());
    }

    @Test
    public void deleteOutdatedRecords() {
        final Set<Integer> expectedRecords = new HashSet<>();

        final JpaTestEnvironment env = environment.get("ticklerepo");
        final TickleRepo tickleRepo = tickleBean.tickleRepo;

        final DataSet dataset = tickleRepo.lookupDataSet(
                new DataSet().withName("dataset"))
                .orElse(null);

        /* Force two records from the dataset to have a
           timeOfLastModification value in the past and
           remember their IDs. */
        env.getPersistenceContext().run(() -> {
            try (TickleRepo.ResultSet<Record> rs = tickleRepo.getRecordsInDataSet(dataset)) {
                final Query updateTimeOfLastModification = env.getEntityManager()
                        .createNativeQuery("UPDATE record SET timeOfLastModification = ?1 WHERE id = ?2")
                        .setParameter(1, Timestamp.from(
                                Instant.now().minus(2, ChronoUnit.DAYS)));
                for (Record record : rs) {
                    if (expectedRecords.size() == 2) {
                        break;
                    }
                    updateTimeOfLastModification.setParameter(2, record.getId());
                    updateTimeOfLastModification.executeUpdate();
                    expectedRecords.add(record.getId());
                }
            }
        });

        /* Call deleteOutdatedRecords while ensuring that
           the two records modified above are included. */
        env.getPersistenceContext().run(() -> tickleBean.deleteOutdatedRecords(
                dataset.getId(),
                Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()));

        final Batch batch = tickleRepo.getNextBatch(new Batch()
                .withDataset(dataset.getId())
                .withId(3))
                .orElse(null);
        assertThat("batch timeOfCompletion",
                batch.getTimeOfCompletion(), is(notNullValue()));

        /* Verify the expected records. */
        int numberOfRecordsInBatch = 0;
        for (Record record : env.getPersistenceContext().run(
                        () -> tickleRepo.getRecordsInBatch(batch))) {
            assertThat("expected set of records contains ID " + record.getId(),
                    expectedRecords.contains(record.getId()), is(true));
            numberOfRecordsInBatch++;
        }
        assertThat("number of outdated records",
                numberOfRecordsInBatch, is(expectedRecords.size()));
    }
}
