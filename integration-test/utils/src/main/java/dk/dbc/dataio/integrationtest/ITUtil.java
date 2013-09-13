package dk.dbc.dataio.integrationtest;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
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
    public static final String FLOWS_TABLE_NAME = "flows";
    public static final String FLOW_COMPONENTS_TABLE_NAME = "flow_components";
    public static final String SUBMITTERS_TABLE_NAME = "submitters";

    public static final String FLOWS_URL_PATH = "flows";
    public static final String FLOW_COMPONENTS_URL_PATH = "components";
    public static final String SUBMITTERS_URL_PATH = "submitters";

    public static final String FLOWS_TABLE_INSERT_STMT = String.format(
            "INSERT INTO %s (id, version, content, name_idx) VALUES (?,?,?,?)", FLOWS_TABLE_NAME);
    public static final String FLOWS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=? AND version=?", FLOWS_TABLE_NAME);
    public static final String FLOW_COMPONENTS_TABLE_INSERT_STMT = String.format(
            "INSERT INTO %s (id, version, content, name_idx) VALUES (?,?,?,?)", FLOW_COMPONENTS_TABLE_NAME);
    public static final String FLOW_COMPONENTS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=? AND version=?", FLOW_COMPONENTS_TABLE_NAME);
    public static final String SUBMITTERS_TABLE_INSERT_STMT = String.format(
            "INSERT INTO %s (id, version, content, name_idx, number_idx) VALUES (?,?,?,?,?)", SUBMITTERS_TABLE_NAME);
    public static final String SUBMITTERS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", SUBMITTERS_TABLE_NAME);

    private ITUtil() { }

    /**
     * @return new connection to underlying h2 database
     */
    public static Connection newDbConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(
                String.format("jdbc:h2:tcp://localhost:%s/mem:flow_store", System.getProperty("h2.port")),
                "root", "root");
        conn.setAutoCommit(true);
        return conn;
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
     * Inserts new submitter row in flow-store database
     *
     * @param conn open database connection
     * @param id submitter id
     * @param version submitter version
     * @param content submitter content as JSON string
     * @param nameIdx submitter name index value
     * @param numberIdx submitter number index value
     *
     * @throws SQLException
     */
    public static void insertSubmitter(Connection conn, long id, long version, String content, String nameIdx, long numberIdx)
            throws SQLException {
        JDBCUtil.update(conn, SUBMITTERS_TABLE_INSERT_STMT, id, version, content, nameIdx, numberIdx);
    }

    /**
     * POSTs given data entity to endpoint constructed using given baseurl and path elements
     *
     * @param restClient RESTful web service client
     * @param data data entity
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPost(Client restClient, Entity data, String baseUrl, String... pathElements) {
        WebTarget target = restClient.target(baseUrl);
        for (String pathElement : pathElements) {
            target = target.path(pathElement);
        }
        return target.request().post(data);
    }

    /**
     * POSTs given data as application/json to endpoint constructed using given baseurl and path elements
     *
     * @param restClient RESTful web service client
     * @param data JSON data
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPostWithJson(Client restClient, String data, String baseUrl, String... pathElements) {
        return doPost(restClient, Entity.entity(data, MediaType.APPLICATION_JSON), baseUrl, pathElements);
    }

    /**
     * POSTs given data as application/x-www-form-urlencoded to endpoint constructed using given baseurl and path elements
     *
     * @param restClient RESTful web service client
     * @param formData form data
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doPostWithFormData(Client restClient, MultivaluedMap<String, String> formData, String baseUrl, String... pathElements) {
        return doPost(restClient, Entity.form(formData), baseUrl, pathElements);
    }

    /**
     * Issues GET request to endpoint constructed using given baseurl and path elements
     *
     * @param restClient RESTful web service client
     * @param queryParameters query parameters to be added to request
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doGet(Client restClient, Map<String, Object> queryParameters, String baseUrl, String... pathElements)  {
        WebTarget target = restClient.target(baseUrl);
        for (String pathElement : pathElements) {
            target = target.path(pathElement);
        }
        for (Map.Entry<String, Object> queryParameter : queryParameters.entrySet()) {
            target = target.queryParam(queryParameter.getKey(), queryParameter.getValue());
        }
        return target.request().get();
    }

     /**
     * Issues GET request to endpoint constructed using given baseurl and path elements
     *
     * @param restClient RESTful web service client
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doGet(Client restClient, String baseUrl, String... pathElements)  {
        return doGet(restClient, new HashMap<String, Object>(), baseUrl, pathElements);
    }

    /**
     * Issues GET request to endpoint constructed using given baseurl and path elements
     *
     * @param restClient RESTful web service client
     * @param queryParameters query parameters to be added to request
     * @param baseUrl base URL on the form http(s)://host:port/path
     * @param pathElements additional path elements to be added to base URL
     *
     * @return server response
     */
    public static Response doGetWithQueryParameters(Client restClient, Map<String, Object> queryParameters, String baseUrl, String... pathElements)  {
        return doGet(restClient, queryParameters, baseUrl, pathElements);
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
     * value from which a ResourceIdentifier object can be derived
     */
    public static ResourceIdentifier getResourceIdentifierFromLocationHeaderAndAssertHasValue(Response response) {
        final List<Object> locationHeader = getHeaderAndAssertNotNull(response, "Location");
        Assert.assertThat(locationHeader.size(), CoreMatchers.is(1));

        final String[] locationHeaderValueParts = ((String) locationHeader.get(0)).split("/");
        Assert.assertThat(locationHeaderValueParts.length > 2, CoreMatchers.is(true));

        final String id = locationHeaderValueParts[locationHeaderValueParts.length - 2];
        final String version = locationHeaderValueParts[locationHeaderValueParts.length - 1];

        return new ResourceIdentifier(Long.valueOf(id), Long.valueOf(version));
    }

    public static long getResourceIdFromLocationHeaderAndAssertHasValue(Response response) {
        final List<Object> locationHeader = getHeaderAndAssertNotNull(response, "Location");
        Assert.assertThat(locationHeader.size(), CoreMatchers.is(1));

        final String[] locationHeaderValueParts = ((String) locationHeader.get(0)).split("/");
        Assert.assertThat(locationHeaderValueParts.length > 2, CoreMatchers.is(true));

        final String id = locationHeaderValueParts[locationHeaderValueParts.length - 1];

        return Long.valueOf(id);
    }

    /**
     * Provides access to a tree based view of the given JSON document similar
     * to DOM nodes in XML DOM trees
     *
     * ToDo: this method should cease to exist when we get a general JSON utility class
     */
    public static JsonNode getJsonRoot(String json) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(json);
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

    /**
     * Simple resource identifier representation with id and version
     */
    public static class ResourceIdentifier {
        private Long id;
        private Long version;

        public ResourceIdentifier(Long id, Long version) {
            this.id = id;
            this.version = version;
        }

        public Long getId() {
            return id;
        }

        public Long getVersion() {
            return version;
        }
    }
}
