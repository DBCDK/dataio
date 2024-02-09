package dk.dbc.dataio.commons.javascript;

import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.types.RevisionInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * JavaScriptProjectFetcherImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
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
    private final SVNRepository svnRepository = Mockito.mock(SVNRepository.class);
    private MockedStatic<SvnConnector> svnMock;

    @BeforeAll
    public static void setUpClass() throws Exception {
        originalTmpDir = System.getProperty(JAVA_IO_TMPDIR);
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
        // Restore original tmp directory
        System.setProperty(JAVA_IO_TMPDIR, originalTmpDir);
    }

    @BeforeEach
    public void setup() throws Exception {
        Path testTmp = Files.createTempDirectory(JavaScriptSubversionProjectTest.class.getName());
        testTmp.toFile().deleteOnExit();
        System.setProperty(JAVA_IO_TMPDIR, testTmp.toString());
        svnMock = mockStatic(SvnConnector.class);
        svnMock.when(() -> SvnConnector.dirExists(svnRepository, JavaScriptSubversionProject.TRUNK_PATH)).thenReturn(true);
        svnMock.when(() -> SvnConnector.getRepository(anyString())).thenReturn(svnRepository);
    }

    @AfterEach
    public void tearDown() {
        svnMock.close();
    }

    @Test
    public void constructor_subversionScmEndpointArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new JavaScriptSubversionProject(null));
    }

    @Test
    public void constructor_subversionScmEndpointArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new JavaScriptSubversionProject(""));
    }

    @Test
    public void constructor_subversionScmEndpointArgIsValid_returnsNewInstance() {
        JavaScriptSubversionProject instance = newInstance();
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void fetchRevisions_projectNameArgIsNull_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(NullPointerException.class, () -> instance.fetchRevisions(null));
    }

    @Test
    public void fetchRevisions_projectNameArgIsEmpty_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(IllegalArgumentException.class, () -> instance.fetchRevisions(""));
    }

    @Test
    public void fetchRevisions_svnConnectorThrowsUriSyntaxException_throws() {
        try {
            svnMock.when(() -> SvnConnector.listAvailableRevisions(anyString())).thenThrow(uriSyntaxException);
            JavaScriptSubversionProject instance = newInstance();
            instance.fetchRevisions(projectName);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
        }
    }

    @Test
    public void fetchRevisions_svnConnectorThrowsSvnException_throws() {
        try {
            svnMock.when(() -> SvnConnector.listAvailableRevisions(anyString())).thenThrow(svnException);
            JavaScriptSubversionProject instance = newInstance();
            instance.fetchRevisions(projectName);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
        }
    }

    @Test
    public void fetchRevisions_svnConnectorReturnsRevisions_returnsRevisions() throws Exception {
        Date now = new Date();
        RevisionInfo.ChangedItem item1 = new RevisionInfo.ChangedItem("/path/file1", "D");
        RevisionInfo.ChangedItem item2 = new RevisionInfo.ChangedItem("/path/file2", "A");
        RevisionInfo.ChangedItem item3 = new RevisionInfo.ChangedItem("/path/file1", "A");
        RevisionInfo rev42 = new RevisionInfo(42L, "bob", now, "message for revision 42", List.of(item1, item2));
        RevisionInfo rev10 = new RevisionInfo(10L, "joe", new Date(now.getTime() - 1000000), "message for revision 10", List.of(item3));
        List<RevisionInfo> expectedRevisions = List.of(rev42, rev10);
        final String projectUrl = subversionScmEndpoint + JavaScriptSubversionProject.URL_DELIMITER
                + projectName + JavaScriptSubversionProject.URL_DELIMITER + JavaScriptSubversionProject.TRUNK_PATH;

        svnMock.when(() -> SvnConnector.listAvailableRevisions(eq(projectUrl))).thenReturn(expectedRevisions);
        JavaScriptSubversionProject instance = newInstance();
        List<RevisionInfo> revisions = instance.fetchRevisions(projectName);
        assertThat(revisions, is(expectedRevisions));
    }

    @Test
    public void fetchJavaScriptFileNames_projectNameArgIsNull_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(NullPointerException.class, () -> instance.fetchJavaScriptFileNames(null, revision));
    }

    @Test
    public void fetchJavaScriptFileNames_projectNameArgIsEmpty_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(IllegalArgumentException.class, () -> instance.fetchJavaScriptFileNames("", revision));
    }

    @Test
    public void fetchJavaScriptFileNames_svnConnectorThrowsUriSyntaxException_throws() {
        svnMock.when(() -> SvnConnector.listAvailablePaths(anyString(), anyLong())).thenThrow(uriSyntaxException);
        JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptFileNames(projectName, revision);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
        }
    }

    @Test
    public void fetchJavaScriptFileNames_svnConnectorThrowsSvnException_throws() {
        svnMock.when(() -> SvnConnector.listAvailablePaths(anyString(), anyLong())).thenThrow(svnException);

        JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptFileNames(projectName, revision);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
        }
    }

    @Test
    public void fetchJavaScriptFileNames_svnConnectorReturnsPaths_returnsNamesOfFilesWithJavaScriptExtension() throws Exception {
        final List<String> paths = List.of(
                "main.js",          // expected in returned result
                "main.js.bak",
                "js.README",
                "sub/aux.js",       // expected in returned result
                "sub/mod.use.js"
        );
        final String projectUrl = subversionScmEndpoint + JavaScriptSubversionProject.URL_DELIMITER
                + projectName + JavaScriptSubversionProject.URL_DELIMITER + JavaScriptSubversionProject.TRUNK_PATH;

        svnMock.when(() -> SvnConnector.listAvailablePaths(eq(projectUrl), eq(revision))).thenReturn(paths);

        JavaScriptSubversionProject instance = newInstance();
        List<String> fileNames = instance.fetchJavaScriptFileNames(projectName, revision);
        assertThat(fileNames.size(), is(2));
        assertThat(fileNames.get(0), is(paths.get(0)));
        assertThat(fileNames.get(1), is(paths.get(3)));
    }

    @Test
    public void fetchJavaScriptInvocationMethods_projectNameArgIsNull_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(NullPointerException.class, () -> instance.fetchJavaScriptInvocationMethods(null, revision, javaScriptFileName));
    }

    @Test
    public void fetchJavaScriptInvocationMethods_projectNameArgIsEmpty_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(IllegalArgumentException.class, () -> instance.fetchJavaScriptInvocationMethods("", revision, javaScriptFileName));
    }

    @Test
    public void fetchJavaScriptInvocationMethods_javaScriptFileNameArgIsNull_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(NullPointerException.class, () -> instance.fetchJavaScriptInvocationMethods(projectName, revision, null));
    }

    @Test
    public void fetchJavaScriptInvocationMethods_javaScriptFileNameArgIsEmpty_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(IllegalArgumentException.class, () -> instance.fetchJavaScriptInvocationMethods(projectName, revision, ""));
    }

    @Test
    public void fetchJavaScriptInvocationMethods_svnConnectorThrowsUriSyntaxException_throws() {
        svnMock.when(() -> SvnConnector.export(anyString(), anyLong(), any(Path.class))).thenThrow(uriSyntaxException);
        JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test
    public void fetchJavaScriptInvocationMethods_svnConnectorThrowsSvnException_throws() {
        svnMock.when(() -> SvnConnector.export(anyString(), anyLong(), any(Path.class))).thenThrow(svnException);

        JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test
    public void fetchRequiredJavaScript_projectNameArgIsNull_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(NullPointerException.class,
                () -> instance.fetchRequiredJavaScript(null, revision, javaScriptFileName, javaScriptFunction));
    }

    @Test
    public void fetchRequiredJavaScript_projectNameArgIsEmpty_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(IllegalArgumentException.class,
                () -> instance.fetchRequiredJavaScript("", revision, javaScriptFileName, javaScriptFunction));
    }

    @Test
    public void fetchRequiredJavaScript_javaScriptFileNameArgIsNull_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(NullPointerException.class,
                () -> instance.fetchRequiredJavaScript(projectName, revision, null, javaScriptFunction));
    }

    @Test
    public void fetchRequiredJavaScript_javaScriptFileNameArgIsEmpty_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(IllegalArgumentException.class,
                () -> instance.fetchRequiredJavaScript(projectName, revision, "", javaScriptFunction));
    }

    @Test
    public void fetchRequiredJavaScript_javaScriptFunctionArgIsNull_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(NullPointerException.class,
                () -> instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, null));
    }

    @Test
    public void fetchRequiredJavaScript_javaScriptFunctionArgIsEmpty_throws() {
        JavaScriptSubversionProject instance = newInstance();
        assertThrows(IllegalArgumentException.class,
                () -> instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, ""));
    }

    @Test
    public void fetchRequiredJavaScript_svnConnectorThrowsUriSyntaxException_throws() throws Throwable {
        svnMock.when(() -> SvnConnector.export(anyString(), anyLong(), any(Path.class))).thenThrow(uriSyntaxException);
        JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_INVALID_URL));
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    @Test
    public void fetchRequiredJavaScript_svnConnectorThrowsSvnException_throws() throws Throwable {
        svnMock.when(() -> SvnConnector.export(anyString(), anyLong(), any(Path.class))).thenThrow(svnException);
        JavaScriptSubversionProject instance = newInstance();
        try {
            instance.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
            fail("Must throw JavaScriptProjectException");
        } catch (JavaScriptProjectException e) {
            assertThat(e.getErrorCode(), is(JavaScriptProjectError.SCM_SERVER_ERROR));
        } finally {
            assertThat(new File(System.getProperty(JAVA_IO_TMPDIR)).list().length, is(0));
        }
    }

    private JavaScriptSubversionProject newInstance() {
        return new JavaScriptSubversionProject(subversionScmEndpoint);
    }
}
