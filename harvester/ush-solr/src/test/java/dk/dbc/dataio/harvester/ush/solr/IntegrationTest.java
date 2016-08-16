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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.jpa.TransactionScopedPersistenceContext;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

public abstract class IntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTest.class);

    protected EntityManager entityManager;
    protected TransactionScopedPersistenceContext persistenceContext;

    @Before
    public void initializeDatabaseAndCreateEntityManager() throws Exception {
        final Flyway flyway = new Flyway();
        flyway.setTable("schema_version");
        flyway.setBaselineOnMigrate(true);
        flyway.setDataSource(JPATestUtils.getTestDataSource("testdb"));
        for (MigrationInfo i : flyway.info().all()) {
            LOGGER.debug("db task {} : {} from file '{}'", i.getVersion(), i.getDescription(), i.getScript());
        }
        flyway.migrate();

        entityManager = JPATestUtils.createEntityManagerForIntegrationTest("harvesterUshSolr_IT_PU");
        persistenceContext = new TransactionScopedPersistenceContext(entityManager);
        clearTables();
    }

    @After
    public void clearTables() {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
        entityManager.getTransaction().begin();
        entityManager.createNativeQuery("DELETE FROM progressWal").executeUpdate();
        entityManager.getTransaction().commit();
    }

    protected void persist(Object entity) {
        persistenceContext.run(() ->
            entityManager.persist(entity));
    }
}
