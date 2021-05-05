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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.MockedJobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.jpa.JPATestUtils;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.sink.es.entity.inflight.EsInFlight;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public abstract class SinkIT {
    private static final long SINK_ID = 1;
    protected static final DataSource ES_INFLIGHT_DATASOURCE;
    protected static final DataSource ES_DATASOURCE;
    protected static final String ES_INFLIGHT_DATABASE_NAME = "testdb";
    protected static final String ES_RESOURCE_NAME = "test/resource";
    protected static final String ES_DATABASE_NAME = "dbname";

    protected EntityManager esInFlightEntityManager;
    protected JMSContext jmsContext = mock(JMSContext.class);
    protected JSONBContext jsonbContext = new JSONBContext();
    protected JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    protected MockedJobStoreServiceConnector jobStoreServiceConnector;
    protected FlowStoreServiceConnectorBean flowStoreServiceConnectorBean = mock(FlowStoreServiceConnectorBean.class);
    protected FlowStoreServiceConnector flowStoreServiceConnector;

    static {
        try {
            ES_INFLIGHT_DATASOURCE = JPATestUtils.getIntegrationTestDataSource(ES_INFLIGHT_DATABASE_NAME);
            ES_DATASOURCE = JPATestUtils.getIntegrationTestDataSource(ES_INFLIGHT_DATABASE_NAME);
        } catch( SQLException e) {
            throw new RuntimeException("Test Setup error Unable to connect to test database");
        }
    }

    @BeforeClass
    public static void setInitialContextFactory() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @BeforeClass
    public static void createEsInFlightDb() {
        final StartupDBMigrator dbMigrator = new StartupDBMigrator();
        dbMigrator.dataSource = ES_INFLIGHT_DATASOURCE;
        dbMigrator.onStartup();
    }

    @BeforeClass
    public static void addSinkIdToEnvironmentVariables() {
        try {
            for (Class declaredClass : Collections.class.getDeclaredClasses()) {
                if ("java.util.Collections$UnmodifiableMap".equals(declaredClass.getName())) {
                    Field field = declaredClass.getDeclaredField("m");
                    field.setAccessible(true);
                    Object currentEnvironment = field.get(System.getenv());
                    Map<String, String> newEnvironment = (Map<String, String>) currentEnvironment;
                    newEnvironment.put(Constants.SINK_ID_ENV_VARIABLE, String.valueOf(SINK_ID));
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Environment not in legal state");
        }
    }

    @Before
    public void setInitialContext() {
        InMemoryInitialContextFactory.bind(ES_RESOURCE_NAME, ES_DATASOURCE);
    }

    @Before
    public void mockJmsContext() {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Before
    public void mockJobStoreServiceConnector() {
        jobStoreServiceConnector = new MockedJobStoreServiceConnector();
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Before
    public void mockFlowStoreServiceConnector() {
        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        when(flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
    }

    @Before
    public void initialiseEsInFlightDbEntityManager() {
        esInFlightEntityManager = JPATestUtils.getIntegrationTestEntityManager("esInFlightIT");
        esInFlightEntityManager.getTransaction().begin();
        esInFlightEntityManager.createNativeQuery("delete from taskpackage").executeUpdate();
        esInFlightEntityManager.createNativeQuery("delete from esinflight").executeUpdate();
        esInFlightEntityManager.getTransaction().commit();

    }

    @After
    public void clearEntityManagerCache() {
        esInFlightEntityManager.clear();
        esInFlightEntityManager.getEntityManagerFactory().getCache().evictAll();
    }

    @After
    public void clearInitialContext() {
        InMemoryInitialContextFactory.clear();
    }

    protected EsMessageProcessorBean getEsMessageProcessorBean() {
        final TestableEsMessageProcessorBean esMessageProcessorBean = new TestableEsMessageProcessorBean();
        esMessageProcessorBean.esConnector = getEsConnectorBean();
        esMessageProcessorBean.esInFlightAdmin = getEsInFlightBean();
        esMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        esMessageProcessorBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        esMessageProcessorBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        esMessageProcessorBean.setup();
        return esMessageProcessorBean;
    }

    protected EsCleanupBean getEsCleanupBean() {
        final EsCleanupBean esCleanupBean = new EsCleanupBean();
        esCleanupBean.esConnector = getEsConnectorBean();
        esCleanupBean.esInFlightAdmin = getEsInFlightBean();
        esCleanupBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        esCleanupBean.flowStoreServiceConnectorBean = flowStoreServiceConnectorBean;
        return esCleanupBean;
    }


    protected EsConnectorBean getEsConnectorBean() {
        final EsConnectorBean esConnectorBean = new EsConnectorBean();
        esConnectorBean.entityManager = JPATestUtils.getIntegrationTestEntityManager("esIT");
        return esConnectorBean;
    }

    protected EsInFlightBean getEsInFlightBean() {
        final EsInFlightBean esInFlightBean = new EsInFlightBean();
        esInFlightBean.entityManager = esInFlightEntityManager;
        return esInFlightBean;
    }

    protected MockedJmsTextMessage getSinkMessage(Chunk chunk, Sink sink) throws JSONBException, JMSException {
        final TextMessage basicMessage = jmsContext.createTextMessage(jsonbContext.marshall(chunk));
        basicMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        basicMessage.setLongProperty(JmsConstants.SINK_ID_PROPERTY_NAME, sink.getId());
        basicMessage.setLongProperty(JmsConstants.SINK_VERSION_PROPERTY_NAME, sink.getVersion());
        final MockedJmsTextMessage message = (MockedJmsTextMessage) basicMessage;

        message.setText(jsonbContext.marshall(chunk));
        return message;
    }

    protected MockedJmsTextMessage getSinkMessage(Chunk chunk) throws JMSException, JSONBException {
        final Sink sink = new SinkBuilder().setId(1).build();
        return getSinkMessage(chunk, sink);
    }

    protected List<EsInFlight> listEsInFlight() {
        final TypedQuery<EsInFlight> query = esInFlightEntityManager.createQuery(
                "SELECT esInFlight FROM EsInFlight esInFlight", EsInFlight.class);
        return query.getResultList();
    }

    protected List<Integer> findTaskPackages() {
        try (final Connection connection = ES_DATASOURCE.getConnection()) {
            return ESTaskPackageIntegrationTestUtil.findTaskpackagesForDBName(connection, ES_DATABASE_NAME);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    protected byte[] getValidAddi() {
        return ("131\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "</es:referencedata>\n1\nb\n").getBytes(StandardCharsets.UTF_8);
    }

    protected byte[] getValidAddiWithMultipleRecords(int numberOfRecords) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (numberOfRecords-- > 0) {
            try {
                outputStream.write(getValidAddi());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return outputStream.toByteArray();
    }

    protected byte[] getValidAddiWithProcessingTrue() {
        return ("235\n" +
                "<es:referencedata xmlns:es=\"http://oss.dbc.dk/ns/es\">" +
                "<es:info format=\"basis\" language=\"dan\" submitter=\"870970\"/>" +
                "<dataio:sink-processing xmlns:dataio=\"dk.dbc.dataio.processing\" encodeAs2709=\"true\" charset=\"danmarc2\"/>" +
                "</es:referencedata>" +
                "\n506\n" +
                "<marcx:record xmlns:marcx='info:lc/xmlns/marcxchange-v1'>" +
                "<marcx:leader>00000n    2200000   4500</marcx:leader>" +
                "<marcx:datafield tag='100' ind1='0' ind2='0'>" +
                "<marcx:subfield code='a'>field1</marcx:subfield>" +
                "<marcx:subfield code='b'/>" +
                "<marcx:subfield code='d'>Field2</marcx:subfield>" +
                "</marcx:datafield><marcx:datafield tag='101' ind1='1' ind2='2'>" +
                "<marcx:subfield code='h'>est</marcx:subfield>" +
                "<marcx:subfield code='k'>o</marcx:subfield>" +
                "<marcx:subfield code='G'>ris</marcx:subfield>" +
                "</marcx:datafield></marcx:record>\n").getBytes(StandardCharsets.UTF_8);
    }

    protected byte[] getValidAddiWithMultipleRecordsWithProcessingTrue(int numberOfRecords) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        while (numberOfRecords-- > 0) {
            try {
                outputStream.write(getValidAddiWithProcessingTrue());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return outputStream.toByteArray();
    }

    protected static class TestableEsMessageProcessorBean extends EsMessageProcessorBean {
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
        public MessageDrivenContext getMessageDrivenContext() {
            return this.messageDrivenContext;
        }
    }
}
