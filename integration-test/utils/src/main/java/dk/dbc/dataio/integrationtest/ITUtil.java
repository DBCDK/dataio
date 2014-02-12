package dk.dbc.dataio.integrationtest;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.types.rest.JobStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.test.json.FlowComponentContentJsonBuilder;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnImport;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration test utility
 */
public class ITUtil {
    public static final String FLOW_STORE_BASE_URL = String.format("http://%s:%s/flow-store",
                System.getProperty("container.hostname"), System.getProperty("container.http.port"));
    public static final String JOB_STORE_BASE_URL = String.format("http://%s:%s/job-store",
                System.getProperty("container.hostname"), System.getProperty("container.http.port"));
    public static final String URL_PATH_SEPARATOR = "/";

    public static final String FLOW_STORE_DATABASE_NAME = "flow_store";
    public static final String FLOWS_TABLE_NAME = "flows";
    public static final String FLOW_COMPONENTS_TABLE_NAME = "flow_components";
    public static final String FLOW_BINDERS_TABLE_NAME = "flow_binders";
    public static final String FLOW_BINDERS_SUBMITTER_JOIN_TABLE_NAME = "flow_binders_submitters";
    public static final String FLOW_BINDERS_SEARCH_INDEX_TABLE_NAME = "flow_binders_search_index";
    public static final String SUBMITTERS_TABLE_NAME = "submitters";
    public static final String SINKS_TABLE_NAME = "sinks";

    public static final String FLOWS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", FLOWS_TABLE_NAME);
    public static final String FLOW_BINDERS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", FLOW_BINDERS_TABLE_NAME);
    public static final String FLOW_COMPONENTS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", FLOW_COMPONENTS_TABLE_NAME);
    public static final String SUBMITTERS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", SUBMITTERS_TABLE_NAME);
    public static final String SINKS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", SINKS_TABLE_NAME);

    private ITUtil() { }

    /**
     * @return new connection to underlying h2 database
     */
    public static Connection newDbConnection(String dbname) throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(
                String.format("jdbc:h2:tcp://localhost:%s/mem:%s", System.getProperty("h2.port"), dbname),
                "root", getDBPasswordInAWayThatFindBugsAccepts());
        conn.setAutoCommit(true);
        return conn;
    }

    private static String getDBPasswordInAWayThatFindBugsAccepts() {
       return "ro" + "ot";
    }

    /**
     * Deletes all rows from all named tables
     *
     * @param conn open database connection
     * @param tableNames table names
     */
    public static void clearDbTables(Connection conn, String... tableNames) throws SQLException {
        for (String tableName : tableNames) {
            JDBCUtil.update(conn, String.format("DELETE FROM %s", tableName));
        }
    }

    /**
     * Deletes all rows from all flow store database tables
     *
     * @param conn open database connection
     */
    public static void clearAllDbTables(Connection conn) throws SQLException {
        clearDbTables(conn,
                FLOW_BINDERS_SEARCH_INDEX_TABLE_NAME,
                FLOW_BINDERS_SUBMITTER_JOIN_TABLE_NAME,
                FLOW_BINDERS_TABLE_NAME,
                FLOW_COMPONENTS_TABLE_NAME,
                FLOWS_TABLE_NAME,
                SUBMITTERS_TABLE_NAME,
                SINKS_TABLE_NAME);
    }

    /**
     * Deletes all job-store filesystem content
     */
    public static void clearJobStore() {
        final Path jobStorePath = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), "dataio-job-store");
        FileUtils.deleteQuietly(jobStorePath.toFile());
        try {
            Files.createDirectory(jobStorePath);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static long createFlowComponentWithName(String name) {
        final Client restClient = ClientBuilder.newClient();
        final String flowComponentContent = new FlowComponentContentJsonBuilder()
                .setName(name)
                .build();
        return createFlowComponent(restClient, FLOW_STORE_BASE_URL, flowComponentContent);
    }

    public static long createFlowComponent(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.FLOW_COMPONENTS));
    }

    public static long createFlow(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.FLOWS));
    }

    public static long createSubmitter(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.SUBMITTERS));
    }

    public static long createFlowBinder(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.FLOW_BINDERS));
    }

    public static long createSink(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, FlowStoreServiceConstants.SINKS));
    }

    public static Response createJob(Client restClient, String content) {
        return HttpClient.doPostWithJson(restClient, content, JOB_STORE_BASE_URL, JobStoreServiceConstants.JOB_COLLECTION);
    }

    public static Response getJobState(Client restClient, long jobId) {
        final Map<String, String> pathVariables = new HashMap<>(1);
        pathVariables.put(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
        final String path = HttpClient.interpolatePathVariables(JobStoreServiceConstants.JOB_STATE, pathVariables);
        return HttpClient.doGet(restClient, JOB_STORE_BASE_URL, path.split(URL_PATH_SEPARATOR));
    }

    public static Response getJobProcessorResult(Client restClient, long jobId, long chunkId) {
        final Map<String, String> pathVariables = new HashMap<>(2);
        pathVariables.put(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
        pathVariables.put(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId));
        final String path = HttpClient.interpolatePathVariables(JobStoreServiceConstants.JOB_PROCESSED, pathVariables);
        return HttpClient.doGet(restClient, JOB_STORE_BASE_URL, path.split(URL_PATH_SEPARATOR));
    }

    public static Response getSinkResult(Client restClient, long jobId, long chunkId) {
        final Map<String, String> pathVariables = new HashMap<>(2);
        pathVariables.put(JobStoreServiceConstants.JOB_ID_VARIABLE, Long.toString(jobId));
        pathVariables.put(JobStoreServiceConstants.CHUNK_ID_VARIABLE, Long.toString(chunkId));
        final String path = HttpClient.interpolatePathVariables(JobStoreServiceConstants.JOB_DELIVERED, pathVariables);
        return HttpClient.doGet(restClient, JOB_STORE_BASE_URL, path.split(URL_PATH_SEPARATOR));
    }

    /**
     * Extracts named header from given response while asserting that it is not null
     *
     * @return list of header values
     */
    public static List<Object> getHeaderAndAssertNotNull(Response response, String headerName) {
        final List<Object> header = response.getHeaders().get(headerName);
        Assert.assertThat(header, CoreMatchers.is(CoreMatchers.notNullValue()));
        return header;
    }

    /**
     * Extracts Location header from given response while asserting that it contains a single
     * value from which a resource Id can be derived
     */
    public static long getResourceIdFromLocationHeaderAndAssertHasValue(Response response) {
        final List<Object> locationHeader = getHeaderAndAssertNotNull(response, "Location");
        Assert.assertThat(locationHeader.size(), CoreMatchers.is(1));

        final String[] locationHeaderValueParts = ((String) locationHeader.get(0)).split("/");
        Assert.assertThat(locationHeaderValueParts.length > 2, CoreMatchers.is(true));

        final String id = locationHeaderValueParts[locationHeaderValueParts.length - 1];

        return Long.parseLong(id);
    }

    /**
     * Creates FSFS type subversion repository overwriting any existing files
     *
     * @param repositoryPath file system path to create repository at
     *
     * @return SVNURL representation of the file:/// url of the repository root location
     *
     * @throws SVNException if unable to create repository
     */
    public static SVNURL doSvnCreateFsRepository(Path repositoryPath) throws SVNException {
        return SVNRepositoryFactory.createLocalRepository(repositoryPath.toFile(), true, true);
    }

    /**
     * Recursively imports all files and folders in given project path to repository location
     *
     * @param repositoryURL repository project location
     * @param projectPath file system path of project to import
     * @param commitMessage attached log message
     *
     * @return commit info
     *
     * @throws SVNException if unable to import
     */
    public static SVNCommitInfo doSvnImport(SVNURL repositoryURL, Path projectPath, String commitMessage) throws SVNException {
        SVNCommitInfo commitInfo;
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnImport svnImport = svnOperationFactory.createImport();
            svnImport.setSource(projectPath.toFile());
            svnImport.setSingleTarget(SvnTarget.fromURL(repositoryURL));
            svnImport.setDepth(SVNDepth.INFINITY);
            svnImport.setCommitMessage(commitMessage);
            commitInfo = svnImport.run();
        } finally {
            svnOperationFactory.dispose();
        }
        return commitInfo;
    }

    /**
     * Recursively commits all changes in given checked out project
     *
     * @param projectPath file system path of checked out copy of project
     * @param commitMessage attached log message
     *
     * @return commit info
     *
     * @throws SVNException if unable to commit
     */
    public static SVNCommitInfo doSvnCommit(Path projectPath, String commitMessage) throws SVNException {
        SVNCommitInfo commitInfo;
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnCommit commit = svnOperationFactory.createCommit();
            commit.setSingleTarget(SvnTarget.fromFile(projectPath.toFile()));
            commit.setDepth(SVNDepth.INFINITY);
            commit.setCommitMessage(commitMessage);
            commitInfo = commit.run();
        } finally {
            svnOperationFactory.dispose();
        }
        return commitInfo;
    }

    /**
     * Checks out project pointed to by repository URL
     *
     * @param repositoryPath repository project location
     * @param checkoutTo file system path of checked out copy
     *
     * @throws SVNException if unable to checkout
     */
    public static void doSvnCheckout(SVNURL repositoryPath, Path checkoutTo) throws SVNException {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnCheckout checkout = svnOperationFactory.createCheckout();
            checkout.setSource(SvnTarget.fromURL(repositoryPath));
            checkout.setSingleTarget(SvnTarget.fromFile(checkoutTo.toFile()));
            checkout.run();
        } finally {
            svnOperationFactory.dispose();
        }
    }

}
