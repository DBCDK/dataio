package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.javascript.JavascriptUtil;
import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import mockit.Expectations;
import mockit.integration.junit4.JMockit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;

import java.io.File;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * JavaScriptProjectFetcherImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(JMockit.class)
public class JavaScriptProjectFetcherImplTest {
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static String originalTmpDir;

    private final String subversionScmEndpoint = "file:///test/repos";
    private final String illegalProjectName = "name/with/path/elements";
    private final String projectName = "project";
    private final String javaScriptFileName = String.format("%s%s%sfile.js",
            JavaScriptProjectFetcherImpl.URL_DELIMITER,
            JavaScriptProjectFetcherImpl.TRUNK_PATH,
            JavaScriptProjectFetcherImpl.URL_DELIMITER);
    private final String javaScriptFunction = "func";
    private final long revision = 1;

    @BeforeClass
    public static void setUpClass() throws Exception {
        originalTmpDir = System.getProperty(JAVA_IO_TMPDIR);
        final Path testTmp = Files.createTempDirectory(JavaScriptProjectFetcherImplTest.class.getName());
        testTmp.toFile().deleteOnExit();
        System.setProperty(JAVA_IO_TMPDIR, testTmp.toString());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Restore original tmp directory
        System.setProperty(JAVA_IO_TMPDIR, originalTmpDir);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_subversionScmEndpointArgIsNull_throws() throws Exception {
         new JavaScriptProjectFetcherImpl(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_subversionScmEndpointArgIsEmpty_throws() throws Exception {
        new JavaScriptProjectFetcherImpl("");
    }

    @Test
    public void constructor_subversionScmEndpointArgIsValid_returnsNewInstance() {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        assertThat(instance, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void fetchRevisions_projectNameArgIsNull_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRevisions(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRevisions_projectNameArgIsEmpty_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRevisions("");
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchRevisions_projectNameArgIsIllegal_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRevisions(illegalProjectName);
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchRevisions_svnConnectorThrowsUriSyntaxException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        new Expectations(SvnConnector.class) { {
                SvnConnector.listAvailableRevisions(anyString); result = new URISyntaxException("input", "reason");
        } };
        instance.fetchRevisions(projectName);
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchRevisions_svnConnectorThrowsSvnException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        new Expectations(SvnConnector.class) { {
                SvnConnector.listAvailableRevisions(anyString); result = new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN));
        } };
        instance.fetchRevisions(projectName);
    }

    @Test
    public void fetchRevisions_svnConnectorReturnsRevisions_returnsRevisions() throws Exception {
        final Date now = new Date();
        final RevisionInfo.ChangedItem item1 = new RevisionInfo.ChangedItem("/path/file1", "D");
        final RevisionInfo.ChangedItem item2 = new RevisionInfo.ChangedItem("/path/file2", "A");
        final RevisionInfo.ChangedItem item3 = new RevisionInfo.ChangedItem("/path/file1", "A");
        final RevisionInfo rev42 = new RevisionInfo(42L, "bob", now, "message for revision 42", new ArrayList<RevisionInfo.ChangedItem>(Arrays.asList(item1, item2)));
        final RevisionInfo rev10 = new RevisionInfo(10L, "joe", new Date(now.getTime() - 1000000), "message for revision 10", new ArrayList<RevisionInfo.ChangedItem>(Arrays.asList(item3)));
        final List<RevisionInfo> expectedRevisions = new ArrayList<RevisionInfo>(Arrays.asList(rev42, rev10));
        final String projectUrl = subversionScmEndpoint + JavaScriptProjectFetcherImpl.URL_DELIMITER
                + projectName + JavaScriptProjectFetcherImpl.URL_DELIMITER + JavaScriptProjectFetcherImpl.TRUNK_PATH;

        final JavaScriptProjectFetcherImpl instance = newInstance();
        new Expectations(SvnConnector.class) { {
                SvnConnector.listAvailableRevisions(projectUrl); result = expectedRevisions;
        } };
        final List<RevisionInfo> revisions = instance.fetchRevisions(projectName);
        assertThat(revisions, is(expectedRevisions));
    }

    @Test(expected = NullPointerException.class)
    public void fetchJavaScriptFileNames_projectNameArgIsNull_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptFileNames(null, revision);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchJavaScriptFileNames_projectNameArgIsEmpty_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptFileNames("", revision);
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchJavaScriptFileNames_projectNameArgIsIllegal_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptFileNames(illegalProjectName, revision);
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchJavaScriptFileNames_svnConnectorThrowsUriSyntaxException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
         new Expectations(SvnConnector.class) { {
                SvnConnector.listAvailablePaths(anyString, revision); result = new URISyntaxException("input", "reason");
        } };
        instance.fetchJavaScriptFileNames(projectName, revision);
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchJavaScriptFileNames_svnConnectorThrowsSvnException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
         new Expectations(SvnConnector.class) { {
                SvnConnector.listAvailablePaths(anyString, revision); result = new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN));
        } };
        instance.fetchJavaScriptFileNames(projectName, revision);
    }

    @Test
    public void fetchJavaScriptFileNames_svnConnectorReturnsPaths_returnsNamesOfFilesWithJavaScriptExtension() throws Exception {
        final List<String> paths = new ArrayList<String>(Arrays.asList(
            "main.js",          // expected in returned result
            "main.js.bak",
            "js.README",
            "sub/aux.js",       // expected in returned result
            "sub/mod.use.js"
        ));
        final String projectUrl = subversionScmEndpoint + JavaScriptProjectFetcherImpl.URL_DELIMITER
                + projectName + JavaScriptProjectFetcherImpl.URL_DELIMITER + JavaScriptProjectFetcherImpl.TRUNK_PATH;

        final JavaScriptProjectFetcherImpl instance = newInstance();
        new Expectations(SvnConnector.class) { {
                SvnConnector.listAvailablePaths(projectUrl, revision); result = paths;
        } };
        final List<String> fileNames = instance.fetchJavaScriptFileNames(projectName, revision);
        assertThat(fileNames.size(), is(2));
        assertThat(fileNames.get(0), is(paths.get(0)));
        assertThat(fileNames.get(1), is(paths.get(3)));
    }

    @Test(expected = NullPointerException.class)
    public void fetchJavaScriptInvocationMethods_projectNameArgIsNull_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptInvocationMethods(null, revision, javaScriptFileName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchJavaScriptInvocationMethods_projectNameArgIsEmpty_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptInvocationMethods("", revision, javaScriptFileName);
    }

    @Test(expected = NullPointerException.class)
    public void fetchJavaScriptInvocationMethods_javaScriptFileNameArgIsNull_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptInvocationMethods(projectName, revision, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchJavaScriptInvocationMethods_javaScriptFileNameArgIsEmpty_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptInvocationMethods(projectName, revision, "");
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchJavaScriptInvocationMethods_projectNameArgIsIllegal_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchJavaScriptInvocationMethods(illegalProjectName, revision, javaScriptFileName);
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchJavaScriptInvocationMethods_svnConnectorThrowsUriSyntaxException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
         new Expectations(SvnConnector.class) { {
                SvnConnector.export(anyString, revision, (Path) any); result = new URISyntaxException("input", "reason");
        } };
        try {
            instance.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchJavaScriptInvocationMethods_svnConnectorThrowsSvnException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
         new Expectations(SvnConnector.class) { {
                SvnConnector.export(anyString, revision, (Path) any); result = new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN));
        } };
        try {
            instance.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test
    public void fetchJavaScriptInvocationMethods__returnsMethodNames() throws Exception {
        final String projectUrl = subversionScmEndpoint + JavaScriptProjectFetcherImpl.URL_DELIMITER
                + projectName + javaScriptFileName;
        final String filename = new File(javaScriptFileName).getName();
        final List<String> expectedFunctionNames = new ArrayList<String>(Arrays.asList("funC", "funA", "funB"));

        final JavaScriptProjectFetcherImpl instance = newInstance();
        new Expectations(JavaScriptProjectFetcherImpl.class, SvnConnector.class, JavascriptUtil.class) { {
                SvnConnector.export(projectUrl, revision, (Path) any);
                invoke(JavaScriptProjectFetcherImpl.class, "getReaderForFile", withAny(Path.class));
                JavascriptUtil.getAllToplevelFunctionsInJavascriptWithFakeUseFunction((Reader) any, filename); result = expectedFunctionNames;
        } };
        final List<String> functionNames = instance.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
        assertThat(functionNames.size(), is(expectedFunctionNames.size()));
        // assert that returned list is sorted
        assertThat(functionNames.get(0), is(expectedFunctionNames.get(1)));
        assertThat(functionNames.get(1), is(expectedFunctionNames.get(2)));
        assertThat(functionNames.get(2), is(expectedFunctionNames.get(0)));
        assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
    }

    @Test(expected = NullPointerException.class)
    public void fetchRequiredJavaScript_projectNameArgIsNull_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRequiredJavaScript(null, revision, javaScriptFileName, javaScriptFunction);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRequiredJavaScript_projectNameArgIsEmpty_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRequiredJavaScript("", revision, javaScriptFileName, javaScriptFunction);
    }

    @Test(expected = NullPointerException.class)
    public void fetchRequiredJavaScript_javaScriptFileNameArgIsNull_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, null, javaScriptFunction);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRequiredJavaScript_javaScriptFileNameArgIsEmpty_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, "", javaScriptFunction);
    }

    @Test(expected = NullPointerException.class)
    public void fetchRequiredJavaScript_javaScriptFunctionArgIsNull_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRequiredJavaScript_javaScriptFunctionArgIsEmpty_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, "");
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchRequiredJavaScript_projectNameArgIsIllegal_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
        instance.fetchRequiredJavaScript(illegalProjectName, revision, javaScriptFileName, javaScriptFunction);
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchRequiredJavaScript_svnConnectorThrowsUriSyntaxException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
         new Expectations(SvnConnector.class) { {
                SvnConnector.export(anyString, revision, (Path) any); result = new URISyntaxException("input", "reason");
        } };
        try {
            instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test(expected = JavaScriptProjectFetcherException.class)
    public void fetchRequiredJavaScript_svnConnectorThrowsSvnException_throws() throws Exception {
        final JavaScriptProjectFetcherImpl instance = newInstance();
         new Expectations(SvnConnector.class) { {
                SvnConnector.export(anyString, revision, (Path) any); result = new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN));
        } };
        try {
            instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Ignore
    @Test
    public void fetchRequiredJavaScript__returnsJavaScripts() throws Exception {
        final String projectUrl = subversionScmEndpoint + JavaScriptProjectFetcherImpl.URL_DELIMITER
                + projectName + JavaScriptProjectFetcherImpl.URL_DELIMITER + JavaScriptProjectFetcherImpl.TRUNK_PATH;
        final String dependencyUrl = subversionScmEndpoint + JavaScriptProjectFetcherImpl.URL_DELIMITER
                + "jscommon" + JavaScriptProjectFetcherImpl.URL_DELIMITER + JavaScriptProjectFetcherImpl.TRUNK_PATH;

        final JavaScriptProjectFetcherImpl instance = newInstance();
        new Expectations(SvnConnector.class) { {
                SvnConnector.export(projectUrl, revision, (Path) any);
                SvnConnector.export(dependencyUrl, revision, (Path) any);
        } };
        instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
        assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
    }

    private JavaScriptProjectFetcherImpl newInstance() {
        return new JavaScriptProjectFetcherImpl(subversionScmEndpoint);
    }
}
