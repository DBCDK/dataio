/*
 * DataIO - Data IO
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.phholdingsitems.ejb;

import dk.dbc.phlog.PhLogDatabaseMigrator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.postgresql.ds.PGSimpleDataSource;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;

public class PhHarvesterIntegrationTest {
    protected static int PHAGENCYID = 830190;
    private static final PGSimpleDataSource phLogDataSource;
    static {
        phLogDataSource = new PGSimpleDataSource();
        phLogDataSource.setDatabaseName("phlog");
        phLogDataSource.setServerName("localhost");
        phLogDataSource.setPortNumber(Integer.parseInt(System.getProperty(
                "postgresql.port", "5432")));
        phLogDataSource.setUser(System.getProperty("user.name"));
        phLogDataSource.setPassword(System.getProperty("user.name"));
    }
    protected static EntityManager phLogEntityManager;

    @BeforeClass
    public static void setUpPhLogDb() {
        PhLogDatabaseMigrator migrator = new PhLogDatabaseMigrator(phLogDataSource);
        migrator.migrate();
        phLogEntityManager = createEntityManager(phLogDataSource,
                "phLogIT");
    }

    @Before
    public void clearPhLog() {
        phLogEntityManager.getTransaction().begin();
        Query query = phLogEntityManager.createNativeQuery("delete from entry");
        query.executeUpdate();
        phLogEntityManager.getTransaction().commit();
    }

    protected Long getCount(EntityManager entityManager) {
        return (Long) runSqlCmdSingleResult(entityManager,
                "select count(*) from entry");
    }

    protected Object runSqlCmdSingleResult(EntityManager entityManager, String sqlCmd) {
        Query query = entityManager.createNativeQuery(sqlCmd);
        return query.getSingleResult();
    }

    private static EntityManager createEntityManager(
            PGSimpleDataSource dataSource, String persistenceUnitName) {
        Map<String, String> entityManagerProperties = new HashMap<>();
        entityManagerProperties.put(JDBC_USER, dataSource.getUser());
        entityManagerProperties.put(JDBC_PASSWORD, dataSource.getPassword());
        entityManagerProperties.put(JDBC_URL, dataSource.getUrl());
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put("eclipselink.logging.level", "FINE");
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName,
                entityManagerProperties);
        return factory.createEntityManager(entityManagerProperties);
    }
}
