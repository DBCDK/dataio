package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.types.RevisionInfo;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * JavaScriptProjectFetcherImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        JavascriptUtil.class,
        SvnConnector.class
})
public class JavaScriptSubversionProjectTest {
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static String originalTmpDir;

    private final String subversionScmEndpoint = "file:///test/repos";
    private final String projectName = "project";
    private final String javaScriptFileName = String.format("%s%s%sfile.js",
            JavaScriptSubversionProject.URL_DELIMITER,
            JavaScriptSubversionProject.TRUNK_PATH,
            JavaScriptSubversionProject.URL_DELIMITER);
    private final String javaScriptFunction = "func";
    private final long revision = 1;
    private final URISyntaxException uriSyntaxException = new URISyntaxException("input", "reason");
    private final SVNException svnException = new SVNException(SVNErrorMessage.create(SVNErrorCode.UNKNOWN));
    private final SVNRepository svnRepository = mock(SVNRepository.class);

    @BeforeClass
    public static void setUpClass() throws Exception {
        originalTmpDir = System.getProperty(JAVA_IO_TMPDIR);
        final Path testTmp = Files.createTempDirectory(JavaScriptSubversionProjectTest.class.getName());
        testTmp.toFile().deleteOnExit();
        System.setProperty(JAVA_IO_TMPDIR, testTmp.toString());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Restore original tmp directory
        System.setProperty(JAVA_IO_TMPDIR, originalTmpDir);
    }

    @Before
    public void setup() throws Exception {
        mockStatic(JavascriptUtil.class);
        mockStatic(SvnConnector.class);
        when(SvnConnector.dirExists(svnRepository, JavaScriptSubversionProject.TRUNK_PATH))
                .thenReturn(true);
        when(SvnConnector.getRepository(anyString()))
                .thenReturn(svnRepository);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_subversionScmEndpointArgIsNull_throws() throws Exception {
        new JavaScriptSubversionProject(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_subversionScmEndpointArgIsEmpty_throws() throws Exception {
        new JavaScriptSubversionProject("");
    }

    @Test
    public void constructor_subversionScmEndpointArgIsValid_returnsNewInstance() {
        final JavaScriptSubversionProject instance = newInstance();
        assertThat(instance, is(notNullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void fetchRevisions_projectNameArgIsNull_throws() throws Exception {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRevisions(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRevisions_projectNameArgIsEmpty_throws() throws Exception {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRevisions("");
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchRevisions_svnConnectorThrowsUriSyntaxException_throws() throws Exception {
        when(SvnConnector.listAvailableRevisions(anyString())).thenThrow(uriSyntaxException);

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchRevisions(projectName);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
            throw e;
        }
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchRevisions_svnConnectorThrowsSvnException_throws() throws Exception {
        when(SvnConnector.listAvailableRevisions(anyString())).thenThrow(svnException);

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchRevisions(projectName);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
            throw e;
        }
    }

    @Test
    public void fetchRevisions_svnConnectorReturnsRevisions_returnsRevisions() throws Exception {
        final Date now = new Date();
        final RevisionInfo.ChangedItem item1 = new RevisionInfo.ChangedItem("/path/file1", "D");
        final RevisionInfo.ChangedItem item2 = new RevisionInfo.ChangedItem("/path/file2", "A");
        final RevisionInfo.ChangedItem item3 = new RevisionInfo.ChangedItem("/path/file1", "A");
        final RevisionInfo rev42 = new RevisionInfo(42L, "bob", now, "message for revision 42", new ArrayList<>(Arrays.asList(item1, item2)));
        final RevisionInfo rev10 = new RevisionInfo(10L, "joe", new Date(now.getTime() - 1000000), "message for revision 10", new ArrayList<>(Collections.singletonList(item3)));
        final List<RevisionInfo> expectedRevisions = new ArrayList<>(Arrays.asList(rev42, rev10));
        final String projectUrl = subversionScmEndpoint + JavaScriptSubversionProject.URL_DELIMITER
                + projectName + JavaScriptSubversionProject.URL_DELIMITER + JavaScriptSubversionProject.TRUNK_PATH;

        when(SvnConnector.listAvailableRevisions(projectUrl)).thenReturn(expectedRevisions);

        final JavaScriptSubversionProject instance = newInstance();
        final List<RevisionInfo> revisions = instance.fetchRevisions(projectName);
        assertThat(revisions, is(expectedRevisions));
    }

    @Test(expected = NullPointerException.class)
    public void fetchJavaScriptFileNames_projectNameArgIsNull_throws() throws Exception {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchJavaScriptFileNames(null, revision);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchJavaScriptFileNames_projectNameArgIsEmpty_throws() throws Exception {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchJavaScriptFileNames("", revision);
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchJavaScriptFileNames_svnConnectorThrowsUriSyntaxException_throws() throws Exception {
        when(SvnConnector.listAvailablePaths(anyString(), anyLong())).thenThrow(uriSyntaxException);

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptFileNames(projectName, revision);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
            throw e;
        }
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchJavaScriptFileNames_svnConnectorThrowsSvnException_throws() throws Exception {
        when(SvnConnector.listAvailablePaths(anyString(), anyLong())).thenThrow(svnException);

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptFileNames(projectName, revision);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
            throw e;
        }
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
        final String projectUrl = subversionScmEndpoint + JavaScriptSubversionProject.URL_DELIMITER
                + projectName + JavaScriptSubversionProject.URL_DELIMITER + JavaScriptSubversionProject.TRUNK_PATH;

        when(SvnConnector.listAvailablePaths(eq(projectUrl), eq(revision))).thenReturn(paths);

        final JavaScriptSubversionProject instance = newInstance();
        final List<String> fileNames = instance.fetchJavaScriptFileNames(projectName, revision);
        assertThat(fileNames.size(), is(2));
        assertThat(fileNames.get(0), is(paths.get(0)));
        assertThat(fileNames.get(1), is(paths.get(3)));
    }

    @Test(expected = NullPointerException.class)
    public void fetchJavaScriptInvocationMethods_projectNameArgIsNull_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchJavaScriptInvocationMethods(null, revision, javaScriptFileName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchJavaScriptInvocationMethods_projectNameArgIsEmpty_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchJavaScriptInvocationMethods("", revision, javaScriptFileName);
    }

    @Test(expected = NullPointerException.class)
    public void fetchJavaScriptInvocationMethods_javaScriptFileNameArgIsNull_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchJavaScriptInvocationMethods(projectName, revision, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchJavaScriptInvocationMethods_javaScriptFileNameArgIsEmpty_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchJavaScriptInvocationMethods(projectName, revision, "");
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchJavaScriptInvocationMethods_svnConnectorThrowsUriSyntaxException_throws() throws Throwable {
        doThrow(uriSyntaxException).when(SvnConnector.class, "export", anyString(), anyLong(), any(Path.class));

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
            throw e;
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchJavaScriptInvocationMethods_svnConnectorThrowsSvnException_throws() throws Throwable {
        doThrow(svnException).when(SvnConnector.class, "export", anyString(), anyLong(), any(Path.class));

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
            throw e;
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test(expected = NullPointerException.class)
    public void fetchRequiredJavaScript_projectNameArgIsNull_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRequiredJavaScript(null, revision, javaScriptFileName, javaScriptFunction);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRequiredJavaScript_projectNameArgIsEmpty_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRequiredJavaScript("", revision, javaScriptFileName, javaScriptFunction);
    }

    @Test(expected = NullPointerException.class)
    public void fetchRequiredJavaScript_javaScriptFileNameArgIsNull_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, null, javaScriptFunction);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRequiredJavaScript_javaScriptFileNameArgIsEmpty_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, "", javaScriptFunction);
    }

    @Test(expected = NullPointerException.class)
    public void fetchRequiredJavaScript_javaScriptFunctionArgIsNull_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fetchRequiredJavaScript_javaScriptFunctionArgIsEmpty_throws() throws Throwable {
        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, "");
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchRequiredJavaScript_svnConnectorThrowsUriSyntaxException_throws() throws Throwable {
        doThrow(uriSyntaxException).when(SvnConnector.class, "export", anyString(), anyLong(), any(Path.class));

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
            throw e;
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test(expected = JavaScriptProjectException.class)
    public void fetchRequiredJavaScript_svnConnectorThrowsSvnException_throws() throws Throwable {
        doThrow(svnException).when(SvnConnector.class, "export", anyString(), anyLong(), any(Path.class));

        final JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
            throw e;
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Ignore
    @Test
    public void fetchRequiredJavaScript__returnsJavaScripts() throws Throwable {
        final String projectUrl = subversionScmEndpoint + JavaScriptSubversionProject.URL_DELIMITER
                + projectName + JavaScriptSubversionProject.URL_DELIMITER + JavaScriptSubversionProject.TRUNK_PATH;
        final String dependencyUrl = subversionScmEndpoint + JavaScriptSubversionProject.URL_DELIMITER
                + "jscommon" + JavaScriptSubversionProject.URL_DELIMITER + JavaScriptSubversionProject.TRUNK_PATH;

        doNothing().when(SvnConnector.class, "export", eq(projectUrl), eq(revision), any(Path.class));
        doNothing().when(SvnConnector.class, "export", eq(dependencyUrl), eq(revision), any(Path.class));

        final JavaScriptSubversionProject instance = newInstance();
        instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
        assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
    }

    private JavaScriptSubversionProject newInstance() {
        return new JavaScriptSubversionProject(subversionScmEndpoint);
    }
}
