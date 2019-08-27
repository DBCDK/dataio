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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test load af ItemEntity
 */
public class ItemEntityIT {

    private EntityManager em;
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEntityIT.class);

    @Before
    public void setUp() throws Exception {
        em = JPATestUtils.getIntegrationTestEntityManager("jobstoreIT");
        JPATestUtils.clearDatabase(em);
    }

    @Test
    public void loadItemEntity() throws Exception {
        JPATestUtils.runSqlFromResource(em, this, "create_jobstore_v14.sql");
        JPATestUtils.runSqlFromResource(em, this, "itemEntityIT_upgrade.sql");

        // Execute flyway upgrade
        final Flyway flyway = Flyway.configure()
                .table("schema_version")
                .baselineOnMigrate(true)
                .dataSource(JPATestUtils.getIntegrationTestDataSource("testdb"))
                .load();
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.debug("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();

        int jobId = 39098;
        for( short id = 0; id <= 9; id++) {
            ItemEntity item = em.find(ItemEntity.class, new ItemEntity.Key(jobId, 0, (short) 1));
            assertThat(item.getDeliveringOutcome(), notNullValue());
        }

        jobId = 39044;
        ItemEntity item = em.find(ItemEntity.class, new ItemEntity.Key(jobId, 0, (short) 0));
        assertThat(item.getPartitioningOutcome(), nullValue());
        assertThat(item.getDeliveringOutcome(), nullValue());
        assertThat(item.getProcessingOutcome(), nullValue());
    }
}
