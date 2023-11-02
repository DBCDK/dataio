package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.commons.types.WorldCatSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.testutil.ObjectFactory;
import dk.dbc.oclc.wciru.WciruServiceConnector;
import dk.dbc.ocnrepo.OcnRepo;
import dk.dbc.ocnrepo.dto.WorldCatEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WorldcatMessageConsumerIT extends IntegrationTest {
    private final WciruServiceBroker wciruServiceBroker = mock(WciruServiceBroker.class);
    private final WciruServiceConnector wciruServiceConnector = mock(WciruServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final WorldCatConfigBean worldCatConfigBean = mock(WorldCatConfigBean.class);
    private final WorldCatSinkConfig config = new WorldCatSinkConfig();

    @Override
    public JpaTestEnvironment setup() {
        final PGSimpleDataSource dataSource = (PGSimpleDataSource) dbContainer.datasource();
        migrateDatabase(dataSource);
        jpaTestEnvironment = new JpaTestEnvironment(dataSource, "ocnRepoIT",
                getEntityManagerFactoryProperties(dataSource));
        return jpaTestEnvironment;
    }

    @Before
    public void resetDatabase() throws SQLException {
        try (Connection conn = jpaTestEnvironment.getDatasource().getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM worldcat");
        }
    }

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(worldCatConfigBean.getConfig(any(ConsumedMessage.class))).thenReturn(config);

    }

    /**
     * When: a never before seen PID is handled
     * Then: a WCIRU push is executed
     * And: a new WorldCatEntity is created
     */
    @Test
    public void isCreatedForUnknownPid() {
        final String ocn = "42";
        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withOcn(ocn)
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.ADD_OR_UPDATE)));

        final Pid pid = Pid.of("778899-test:new");
        final ChunkItem chunkItem = newChunkItem("data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.emptyList()));

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        final ChunkItem result = bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        verifyPush();

        assertThat("result status", result.getStatus(), is(ChunkItem.Status.SUCCESS));

        final WorldCatEntity entity = jpaTestEnvironment.getEntityManager().find(WorldCatEntity.class, pid.toString());
        assertThat("WorldCat entity created", entity, is(notNullValue()));
        assertThat("WorldCat entity ocn updated", entity.getOcn(), is(ocn));
        assertThat("WorldCat entity checksum updated", entity.getChecksum(), is(notNullValue()));
        assertThat("WorldCat entity hasLhr flag", entity.hasLHR(), is(false));
    }

    /**
     * When: a known PID is handled
     * And: the checksum indicates no change
     * Then: no WCIRU push is executed
     * And: the result has status set to IGNORE
     */
    @Test
    public void isIgnoredWhenChecksumMatches() {
        executeScriptResource("/worldcat_existing_entries.sql");

        final Pid pid = Pid.of("123456-test:existing");
        final ChunkItem chunkItem = newChunkItem("data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.emptyList()));

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        final ChunkItem result =  bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        verifyNoPush();

        assertThat("result status", result.getStatus(), is(ChunkItem.Status.IGNORE));
    }

    /**
     * When: a known PID is handled
     * And: the checksum indicates change
     * Then: a WCIRU push is executed
     * And: the existing WorldCatEntity is updated
     */
    @Test
    public void isUpdatedForExistingPid() {
        executeScriptResource("/worldcat_existing_entries.sql");

        final String ocn = "newOCN";
        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withOcn(ocn)
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.ADD_OR_UPDATE)));

        final Pid pid = Pid.of("123456-test:existing");
        final ChunkItem chunkItem = newChunkItem("updated data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.emptyList())
                .withLhr(true));

        final String checksumBeforeUpdate = jpaTestEnvironment.getEntityManager()
                .find(WorldCatEntity.class, pid.toString()).getChecksum();

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        final ChunkItem result = bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        verifyPush();

        assertThat("result status", result.getStatus(), is(ChunkItem.Status.SUCCESS));

        final WorldCatEntity entity = jpaTestEnvironment.getEntityManager().find(WorldCatEntity.class, pid.toString());
        assertThat("WorldCat entity ocn updated", entity.getOcn(), is(ocn));
        assertThat("WorldCat entity checksum updated", entity.getChecksum(), is(not(checksumBeforeUpdate)));
        assertThat("WorldCat entity hasLhr flag", entity.hasLHR(), is(true));
    }

    /**
     * When: a known PID is handled
     * And: the checksum indicates change
     * And: the holdings indicates deletion
     * Then: a WCIRU push is executed
     * And: the existing WorldCatEntity is removed
     */
    @Test
    public void isDeletedForExistingPid() {
        executeScriptResource("/worldcat_existing_entries.sql");

        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.DELETE)));

        final Pid pid = Pid.of("123456-test:existing");
        final ChunkItem chunkItem = newChunkItem("updated data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.emptyList()));

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        final ChunkItem result =  bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        verifyPush();

        assertThat("result status", result.getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("worldcat entity deleted",
                jpaTestEnvironment.getEntityManager().find(WorldCatEntity.class, pid.toString()), is(nullValue()));
    }

    /**
     * When: a known PID is handled
     * And: the checksum indicates change
     * And: the holdings indicates deletion
     * Then: a WCIRU push is executed but fails
     * And: the existing WorldCatEntity is not removed
     */
    @Test
    public void isKeptWhenWciruFails() {
        executeScriptResource("/worldcat_existing_entries.sql");

        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withException(new IllegalStateException("fail"))
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.DELETE)));

        final Pid pid = Pid.of("123456-test:existing");
        final ChunkItem chunkItem = newChunkItem("updated data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.emptyList()));

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        final ChunkItem result = bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        verifyPush();

        assertThat("worldcat entity not deleted",
                jpaTestEnvironment.getEntityManager().find(WorldCatEntity.class, pid.toString()), is(notNullValue()));
    }

    /**
     * When: WCIRU request fails
     * Then: the result is failed
     */
    @Test
    public void isFailed() {
        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withOcn("42")
                .withException(new IllegalStateException("fail"))
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.ADD_OR_UPDATE)));

        final Pid pid = Pid.of("778899-test:new");
        final ChunkItem chunkItem = newChunkItem("data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.emptyList()));

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        final ChunkItem result = bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        verifyPush();

        assertThat("result status", result.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    /**
     * When: cached holding symbols differ from current
     * Then: the difference is added as holdings with action DELETE
     * When: the WCIRU requests succeeds
     * Then: the cached active holding symbols are updated
     */
    @Test
    public void maintainsActiveHoldingSymbols() {
        executeScriptResource("/worldcat_existing_entries.sql");

        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withOcn("ocnQWERTY")
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.ADD_OR_UPDATE)));

        final Pid pid = Pid.of("987654-test:existing");
        final ChunkItem chunkItem = newChunkItem("data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.singletonList(new Holding()
                        .withSymbol("DEF")
                        .withAction(Holding.Action.INSERT))));

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
         bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        final ArgumentCaptor<ChunkItemWithWorldCatAttributes> chunkItemArgumentCaptor =
                ArgumentCaptor.forClass(ChunkItemWithWorldCatAttributes.class);
        verify(wciruServiceBroker).push(chunkItemArgumentCaptor.capture(), any(WorldCatEntity.class));
        assertThat(chunkItemArgumentCaptor.getValue().getWorldCatAttributes().getHoldings(),
                is(Arrays.asList(
                        new Holding().withSymbol("DEF").withAction(Holding.Action.INSERT),
                        new Holding().withSymbol("ABC").withAction(Holding.Action.DELETE))));

        final WorldCatEntity entity = jpaTestEnvironment.getEntityManager().find(WorldCatEntity.class, pid.toString());
        assertThat(entity.getActiveHoldingSymbols(), is(Collections.singletonList("DEF")));
    }

    /**
     * When: cached holding symbols differ from current
     * Then: the difference is added as holdings with action DELETE
     * When: the WCIRU requests fails
     * Then: the cached active holding symbols are not updated
     */
    @Test
    public void activeHoldingSymbolsKeptWhenWciruFails() {
        executeScriptResource("/worldcat_existing_entries.sql");

        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withOcn("ocnQWERTY")
                .withException(new IllegalStateException("fail"))
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.ADD_OR_UPDATE)));

        final Pid pid = Pid.of("987654-test:existing");
        final ChunkItem chunkItem = newChunkItem("data", new WorldCatAttributes()
                .withPid(pid.toString())
                .withHoldings(Collections.singletonList(new Holding()
                        .withSymbol("DEF")
                        .withAction(Holding.Action.INSERT))));

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        bean.handleChunkItem(chunkItem, new OcnRepo(jpaTestEnvironment.getEntityManager()));

        verifyPush();

        final WorldCatEntity entity = jpaTestEnvironment.getEntityManager().find(WorldCatEntity.class, pid.toString());
        assertThat(entity.getActiveHoldingSymbols(), is(Collections.singletonList("ABC")));
    }

    /**
     * When: a chunk result becomes available
     * Then: it is uploaded to the job-store
     */
    @Test
    public void uploadsResult() throws JobStoreServiceConnectorException, InvalidMessageException {
        whenPush().thenReturn(new WciruServiceBroker(wciruServiceConnector).new Result()
                .withOcn("42")
                .withEvents(new WciruServiceBroker.Event()
                        .withAction(WciruServiceBroker.Event.Action.ADD_OR_UPDATE)));

        final ChunkItem chunkItem = newChunkItem("data", new WorldCatAttributes()
                .withPid(Pid.of("778899-test:new").toString())
                .withHoldings(Collections.emptyList()));

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).appendItem(chunkItem).build();
        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        bean.handleConsumedMessage(message);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(Chunk.class), anyInt(), anyLong());
    }

    /**
     * When: an input chunk item is already failed
     * Then: it is ignored
     */
    @Test
    public void failedByJobProcessor() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder().setStatus(ChunkItem.Status.FAILURE).build()))
                .build();

        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        jpaTestEnvironment.getPersistenceContext().run(() -> bean.handleConsumedMessage(message));

        final ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), anyInt(), anyLong());

        assertThat(chunkArgumentCaptor.getValue().getType(), is(Chunk.Type.DELIVERED));
        assertThat(chunkArgumentCaptor.getValue().getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
    }

    /**
     * When: an input chunk item is already ignored
     * Then: it is ignored
     */
    @Test
    public void ignoredByJobProcessor() throws JobStoreServiceConnectorException {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build()))
                .build();

        final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);

        final WorldcatMessageConsumer bean = newMessageConsumerBean();
        jpaTestEnvironment.getPersistenceContext().run(() -> bean.handleConsumedMessage(message));

        final ArgumentCaptor<Chunk> chunkArgumentCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(chunkArgumentCaptor.capture(), anyInt(), anyLong());

        assertThat(chunkArgumentCaptor.getValue().getType(), is(Chunk.Type.DELIVERED));
        assertThat(chunkArgumentCaptor.getValue().getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
    }


    private Map<String, String> getEntityManagerFactoryProperties(PGSimpleDataSource datasource) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(JDBC_USER, datasource.getUser());
        properties.put(JDBC_PASSWORD, datasource.getPassword());
        properties.put(JDBC_URL, datasource.getUrl());
        properties.put(JDBC_DRIVER, "org.postgresql.Driver");
        properties.put("eclipselink.logging.level", "FINE");
        return properties;
    }


    private WorldcatMessageConsumer newMessageConsumerBean() {
        final WorldcatMessageConsumer worldcatMessageConsumer = new WorldcatMessageConsumer(
                new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build(), jpaTestEnvironment.getEntityManagerFactory());
        worldcatMessageConsumer.wciruServiceBroker = wciruServiceBroker;
        worldcatMessageConsumer.worldCatConfigBean = worldCatConfigBean;
        worldcatMessageConsumer.config = config;
        return worldcatMessageConsumer;
    }

    private ChunkItem newChunkItem(String data, WorldCatAttributes attributes) {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            return new ChunkItemBuilder()
                    .setData(new AddiRecord(
                            StringUtil.asBytes(jsonbContext.marshall(attributes)),
                            StringUtil.asBytes(data)).getBytes())
                    .build();
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private OngoingStubbing<WciruServiceBroker.Result> whenPush() {
        return when(wciruServiceBroker.push(any(ChunkItemWithWorldCatAttributes.class), any(WorldCatEntity.class)));
    }

    private void verifyPush() {
        verify(wciruServiceBroker).push(any(ChunkItemWithWorldCatAttributes.class), any(WorldCatEntity.class));
    }

    private void verifyNoPush() {
        verify(wciruServiceBroker, times(0)).push(any(ChunkItemWithWorldCatAttributes.class), any(WorldCatEntity.class));
    }
}
