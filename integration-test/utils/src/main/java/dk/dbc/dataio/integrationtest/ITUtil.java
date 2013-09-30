package dk.dbc.dataio.integrationtest;

import dk.dbc.commons.jdbc.util.JDBCUtil;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
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
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Integration test utility
 */
public class ITUtil {
    public static final String FLOWS_TABLE_NAME = "flows";
    public static final String FLOW_COMPONENTS_TABLE_NAME = "flow_components";
    public static final String FLOW_BINDERS_TABLE_NAME = "flow_binders";
    public static final String FLOW_BINDERS_SUBMITTER_JOIN_TABLE_NAME = "flow_binders_submitters";
    public static final String FLOW_BINDERS_SEARCH_INDEX_TABLE_NAME = "flow_binders_search_index";
    public static final String SUBMITTERS_TABLE_NAME = "submitters";

    public static final String FLOWS_URL_PATH = "flows";
    public static final String FLOW_COMPONENTS_URL_PATH = "components";
    public static final String FLOW_BINDERS_URL_PATH = "binders";
    public static final String SUBMITTERS_URL_PATH = "submitters";

    public static final String FLOWS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", FLOWS_TABLE_NAME);
    public static final String FLOW_BINDERS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", FLOW_BINDERS_TABLE_NAME);
    public static final String FLOW_COMPONENTS_TABLE_SELECT_CONTENT_STMT = String.format(
            "SELECT content FROM %s WHERE id=?", FLOW_COMPONENTS_TABLE_NAME);
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
                SUBMITTERS_TABLE_NAME);
    }

    public static long createFlowComponentWithName(String name) {
        final String baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        final Client restClient = ClientBuilder.newClient();
        final String flowComponentContent = new FlowComponentContentJsonBuilder()
                .setName(name)
                .build();
        return createFlowComponent(restClient, baseUrl, flowComponentContent);
    }

    public static long createFlowComponent(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, ITUtil.FLOW_COMPONENTS_URL_PATH));
    }

    public static long createFlow(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, ITUtil.FLOWS_URL_PATH));
    }

    public static long createSubmitter(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, ITUtil.SUBMITTERS_URL_PATH));
    }

    public static long createFlowBinder(Client restClient, String baseUrl, String content) {
        return getResourceIdFromLocationHeaderAndAssertHasValue(
                HttpClient.doPostWithJson(restClient, content, baseUrl, ITUtil.FLOW_BINDERS_URL_PATH));
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

        return Long.valueOf(id);
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
     * Abstract base class for JSON content builders
     */
    public abstract static class JsonBuilder {
        protected static final String MEMBER_DELIMITER = ", ";
        protected static final String NULL_VALUE = "null";
        protected static final String START_ARRAY = "[";
        protected static final String END_ARRAY = "]";
        protected static final String START_OBJECT = "{";
        protected static final String END_OBJECT = "}";

        protected String asTextMember(String memberName, String memberValue) {
            if (memberValue == null) {
                return String.format("\"%s\": null", memberName);
            }
            return String.format("\"%s\": \"%s\"", memberName, memberValue);
        }

        protected String asLongMember(String memberName, Long memberValue) {
            final String memberValueAsString = (memberValue == null) ? NULL_VALUE
                : Long.toString(memberValue);
            return String.format("\"%s\": %s", memberName, memberValueAsString);
        }

        protected String asObjectMember(String memberName, String memberValue) {
            final String memberValueAsString = (memberValue == null) ? NULL_VALUE
                    : memberValue;
            return String.format("\"%s\": %s", memberName, memberValueAsString);
        }

        protected String asObjectArray(String memberName, List<String> memberValues) {
            final String memberValuesAsString = (memberValues == null) ? NULL_VALUE
                    : String.format("%s%s%s", START_ARRAY, joinNonTextValues(",", memberValues), END_ARRAY);
            return String.format("\"%s\": %s", memberName, memberValuesAsString);
        }

        protected String asLongArray(String memberName, List<Long> memberValues) {
            final String memberValuesAsString = (memberValues == null) ? NULL_VALUE
                : String.format("%s%s%s", START_ARRAY, joinLongs(",", memberValues), END_ARRAY);
            return String.format("\"%s\": %s", memberName, memberValuesAsString);
        }

        protected String joinNonTextValues(String delimiter, List<String> memberValues) {
            final StringBuilder stringbuilder = new StringBuilder();
            for (String memberValue : memberValues) {
                final String value = (memberValue != null) ? memberValue : NULL_VALUE;
                stringbuilder.append(value).append(delimiter);
            }
            return stringbuilder.toString().replaceFirst(String.format("%s$", delimiter), "");
        }

        protected String joinLongs(String delimiter, List<Long> ids) {
            final StringBuilder stringbuilder = new StringBuilder();
            for (Long id : ids) {
                final String idAsString = (id != null) ? Long.toString(id) : NULL_VALUE;
                stringbuilder.append(idAsString).append(delimiter);
            }
            return stringbuilder.toString().replaceFirst(String.format("%s$", delimiter), "");
        }
    }

    public static class SubmitterContentJsonBuilder extends JsonBuilder {
        private String name = "name";
        private String description = "description";
        private Long number = 42L;

        public SubmitterContentJsonBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public SubmitterContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public SubmitterContentJsonBuilder setNumber(Long number) {
            this.number = number;
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongMember("number", number));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

    public static class JavaScriptJsonBuilder extends JsonBuilder {
        private String javascript = "javascript";
        private String moduleName = "moduleName";

        public JavaScriptJsonBuilder setJavascript(String javascript) {
            this.javascript = javascript;
            return this;
        }

        public JavaScriptJsonBuilder setModuleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("javascript", javascript)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("moduleName", moduleName));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

    public static class FlowComponentContentJsonBuilder extends JsonBuilder {
        private String name = "name";
        private String invocationMethod = "invocationMethod";
        private List<String> javascripts = new ArrayList<>(Arrays.asList(
                new JavaScriptJsonBuilder().build()));

        public FlowComponentContentJsonBuilder setInvocationMethod(String invocationMethod) {
            this.invocationMethod = invocationMethod;
            return this;
        }

        public FlowComponentContentJsonBuilder setJavascripts(List<String> javascripts) {
            this.javascripts = new ArrayList<>(javascripts);
            return this;
        }

        public FlowComponentContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

       public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
           stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("invocationMethod", invocationMethod)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asObjectArray("javascripts", javascripts));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

    public static class FlowContentJsonBuilder extends JsonBuilder {
        private String name = "name";
        private String description = "description";
        private List<String> components = new ArrayList<>(Arrays.asList(
                new FlowComponentJsonBuilder().build()));

        public FlowContentJsonBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public FlowContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public FlowContentJsonBuilder setComponents(List<String> components) {
            this.components = new ArrayList<>(components);
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asObjectArray("components", components));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

    public static class FlowBinderContentJsonBuilder extends JsonBuilder {
        private String name = "name";
        private String packaging = "packaging";
        private String format = "format";
        private String destination = "destination";
        private String charset = "charset";
        private String description = "description";
        private String recordSplitter = "recordSplitter";
        private Long flowId = 42L;
        private List<Long> submitterIds = new ArrayList<>(Arrays.asList(43L));

        public FlowBinderContentJsonBuilder setCharset(String charset) {
            this.charset = charset;
            return this;
        }

        public FlowBinderContentJsonBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public FlowBinderContentJsonBuilder setDestination(String destination) {
            this.destination = destination;
            return this;
        }

        public FlowBinderContentJsonBuilder setFlowId(Long flowId) {
            this.flowId = flowId;
            return this;
        }

        public FlowBinderContentJsonBuilder setFormat(String format) {
            this.format = format;
            return this;
        }

        public FlowBinderContentJsonBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public FlowBinderContentJsonBuilder setPackaging(String packaging) {
            this.packaging = packaging;
            return this;
        }

        public FlowBinderContentJsonBuilder setRecordSplitter(String recordSplitter) {
            this.recordSplitter = recordSplitter;
            return this;
        }

        public FlowBinderContentJsonBuilder setSubmitterIds(List<Long> submitterIds) {
            this.submitterIds = new ArrayList<>(submitterIds);
            return this;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asTextMember("name", name)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("description", description)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("packaging", packaging)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("format", format)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("charset", charset)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("destination", destination)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asTextMember("recordSplitter", recordSplitter)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongMember("flowId", flowId)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongArray("submitterIds", submitterIds));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }

    public static class FlowComponentJsonBuilder extends JsonBuilder {
        private Long id = 42L;
        private Long version = 1L;
        private String content = new FlowComponentContentJsonBuilder().build();

        public FlowComponentJsonBuilder setId(Long id) {
            this.id = id;
            return this;
        }

        public FlowComponentJsonBuilder setVersion(Long version) {
            this.version = version;
            return this;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String build() {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(START_OBJECT);
            stringBuilder.append(asLongMember("id", id)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asLongMember("version", version)); stringBuilder.append(MEMBER_DELIMITER);
            stringBuilder.append(asObjectMember("content", content));
            stringBuilder.append(END_OBJECT);
            return stringBuilder.toString();
        }
    }
}
