package dk.dbc.dataio.harvester.phholdingsitems.ejb;

import dk.dbc.dataio.commons.utils.test.jms.MockedJmsObjectMessage;
import dk.dbc.dataio.openagency.OpenAgencyConnector;
import dk.dbc.dataio.openagency.OpenAgencyConnectorException;
import dk.dbc.dataio.openagency.ejb.OpenAgencyConnectorBean;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.QueueJob;
import dk.dbc.phlog.PhLog;
import dk.dbc.phlog.PhLogDatabaseMigrator;
import org.mockito.Mockito;
import org.postgresql.ds.PGSimpleDataSource;

import javax.ejb.EJB;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.mockito.Mockito.mock;

public class PhLogTest {

    @EJB
    static HoldingsItemsMessageConsumerBean holdingsItemsMessageConsumerBean;

    private final static DataSource dataSource = mock(DataSource.class);
    private final static HoldingsItemsDAO holdingsItemsDao = mock(
            HoldingsItemsDAO.class);

    private static Map<String, String> entityManagerProperties = new HashMap<>();
    private static EntityManagerFactory entityManagerFactory;

    private static final PGSimpleDataSource datasource;
    static {
        datasource = new PGSimpleDataSource();
        datasource.setDatabaseName("phlog");
        datasource.setServerName("localhost");
        datasource.setPortNumber(5432);
        datasource.setUser("jsj");
        datasource.setPassword("jsj");
    }

    public static void createEntityManagerFactory() {
        entityManagerProperties.put(JDBC_USER, datasource.getUser());
        entityManagerProperties.put(JDBC_PASSWORD, datasource.getPassword());
        entityManagerProperties.put(JDBC_URL, datasource.getUrl());
        entityManagerProperties.put(JDBC_DRIVER, "org.postgresql.Driver");
        entityManagerProperties.put("eclipselink.logging.level", "FINE");
        entityManagerFactory = Persistence.createEntityManagerFactory("phLogIT", entityManagerProperties);
    }

    public static void main(String[] args) {
        try {
            Map<String, Integer> statusMap = new HashMap<>();
            statusMap.put("Decommissioned", 2);
            statusMap.put("OnShelf", 3);
            HoldingsItemsMessageConsumerBean holdingsItemsMessageConsumerBean = Mockito.spy(
                    new HoldingsItemsMessageConsumerBean());

            PhLogDatabaseMigrator migrator = new PhLogDatabaseMigrator(datasource);
            migrator.migrate();
            createEntityManagerFactory();
            EntityManager entityManager = entityManagerFactory.createEntityManager(entityManagerProperties);
            EntityTransaction transaction = entityManager.getTransaction();
            transaction.begin();
            PhLogHandler phLogHandler = new PhLogHandler();
            phLogHandler.phLog = new PhLog(entityManager);
            holdingsItemsMessageConsumerBean.phLogHandler = phLogHandler;
            Mockito.when(holdingsItemsDao.getStatusFor(Mockito.anyString(),
                    Mockito.anyInt())).thenReturn(statusMap);
            Mockito.when(holdingsItemsMessageConsumerBean.getHoldingsItemsDao(Mockito.any(
                    Connection.class))).thenReturn(holdingsItemsDao);
            OpenAgencyConnectorBean openAgencyConnectorBean = mock(OpenAgencyConnectorBean.class);
            OpenAgencyConnector openAgencyConnector = mock(OpenAgencyConnector.class);
            Set<Integer> set = new HashSet<>();
            set.add(830190);
            Mockito.when(openAgencyConnector.getPHLibraries()).thenReturn(set);
            Mockito.when(openAgencyConnectorBean.getConnector()).thenReturn(openAgencyConnector);

            holdingsItemsMessageConsumerBean.openAgencyConnectorBean = openAgencyConnectorBean;

            QueueJob qjob = new QueueJob("52568765", 830190, "17", "");
            ObjectMessage message = new MockedJmsObjectMessage();
            message.setObject(qjob);
            holdingsItemsMessageConsumerBean.dataSource = datasource;
            holdingsItemsMessageConsumerBean.onMessage(message);
            transaction.commit();

            Query query = entityManager.createNativeQuery("select count(*) from entry");
            List cursor = query.getResultList();
            System.out.println("c: " + cursor);
            //Iterator iter = cursor.iterator();
            //while(iter.hasNext())
                //for(Object o : (Object[])iter.next())
                    //System.out.println("c: " + o.getClass().getName());
        } catch(JMSException | HoldingsItemsException | OpenAgencyConnectorException e) {
            System.err.println("error: " + e.toString());
        }
    }
}
