package dk.dbc.dataio.harvester.task.rest;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.persistence.JpaIntegrationTest;
import dk.dbc.commons.persistence.JpaTestEnvironment;
import dk.dbc.dataio.commons.testcontainers.PostgresContainerJPAUtils;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.task.TaskRepo;
import dk.dbc.dataio.harvester.task.TaskRepoDatabaseMigrator;
import dk.dbc.dataio.harvester.task.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvestRequest;
import dk.dbc.dataio.harvester.types.HarvestSelectorRequest;
import dk.dbc.dataio.harvester.types.HarvestTaskSelector;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Query;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestTasksBeanIT extends JpaIntegrationTest implements PostgresContainerJPAUtils {
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final UriBuilder uriBuilder = mock(UriBuilder.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private final long harvestId = 42;

    @Override
    public JpaTestEnvironment setup() {
        DataSource dataSource = getDataSource();
        migrateDatabase(dataSource);
        return new JpaTestEnvironment(dataSource, "taskrepoIT_PU",
                dbContainer.entityManagerProperties());
    }

    @Before
    public void clearDatabase() throws SQLException {
        try (Connection conn = env().getDatasource().getConnection();
             Statement statement = conn.createStatement()) {
            statement.executeUpdate("DELETE FROM task");
        }
    }

    @Before
    public void setupMockedUriInfo() throws URISyntaxException {
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("location"));
    }

    @Test
    public void requestIsIllegalJson() {
        HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        Response response = harvestTasksBean.createHarvestTask(uriInfo, harvestId, "not JSON");
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void requestIsInvalidJson() {
        HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        Response response = harvestTasksBean.createHarvestTask(uriInfo, harvestId, "{\"key\": \"value\"}");
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void requestIsUnknown() throws JSONBException {
        HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        Response response = harvestTasksBean.createHarvestTask(
                uriInfo, harvestId, jsonbContext.marshall(new UnknownRequest()));
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void recordsTaskIsCreated() {
        List<AddiMetaData> expectedRecords = new ArrayList<>();
        expectedRecords.add(new AddiMetaData()
                .withBibliographicRecordId("id1")
                .withSubmitterNumber(123456)
                .withLibraryRules(new AddiMetaData.LibraryRules()));
        expectedRecords.add(new AddiMetaData()
                .withBibliographicRecordId("id2")
                .withSubmitterNumber(654321)
                .withLibraryRules(new AddiMetaData.LibraryRules()));

        HarvestRecordsRequest request = new HarvestRecordsRequest(expectedRecords);

        HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        Response response = env().getPersistenceContext().run(() ->
                harvestTasksBean.createHarvestTask(uriInfo, harvestId, jsonbContext.marshall(request)));

        assertThat("Response status", response.getStatus(), is(Response.Status.CREATED.getStatusCode()));

        Query query = env().getEntityManager()
                .createQuery("SELECT task FROM HarvestTask task WHERE task.configId = :configId")
                .setParameter("configId", harvestId);

        List<HarvestTask> created = query.getResultList();
        assertThat("Number of tasks created", created.size(), is(1));

        HarvestTask task = created.get(0);
        assertThat("Task records", task.getRecords(), is(expectedRecords));
        assertThat("Task number of records", task.getNumberOfRecords(), is(expectedRecords.size()));
    }

    @Test
    public void selectorTaskIsCreated() {
        HarvestTaskSelector selector = new HarvestTaskSelector("dataset", "42");

        HarvestSelectorRequest request = new HarvestSelectorRequest(selector);

        HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        Response response = env().getPersistenceContext().run(() ->
                harvestTasksBean.createHarvestTask(uriInfo, harvestId, jsonbContext.marshall(request)));

        assertThat("Response status", response.getStatus(), is(Response.Status.CREATED.getStatusCode()));

        Query query = env().getEntityManager()
                .createQuery("SELECT task FROM HarvestTask task WHERE task.configId = :configId")
                .setParameter("configId", harvestId);

        List<HarvestTask> created = query.getResultList();
        assertThat("Number of tasks created", created.size(), is(1));

        HarvestTask task = created.get(0);
        assertThat("Task selector", task.getSelector(), is(selector));
    }

    private HarvestTasksBean createHarvestTasksBean() {
        HarvestTasksBean harvestTasksBean = new HarvestTasksBean();
        harvestTasksBean.taskRepo = new TaskRepo(env().getEntityManager());
        return harvestTasksBean;
    }

    private DataSource getDataSource() {
        return dbContainer.datasource();
    }

    private void migrateDatabase(DataSource datasource) {
        TaskRepoDatabaseMigrator dbMigrator = new TaskRepoDatabaseMigrator(datasource);
        dbMigrator.migrate();
    }

    private static class UnknownRequest extends HarvestRequest<UnknownRequest> {
        private static final long serialVersionUID = -3753043123849598257L;
    }
}
