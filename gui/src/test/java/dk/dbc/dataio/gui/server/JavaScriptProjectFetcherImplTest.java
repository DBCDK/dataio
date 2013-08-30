package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.svn.SvnConnector;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import mockit.Expectations;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;

import java.net.URISyntaxException;
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
    private final String subversionScmEndpoint = "file:///test/repos";
    private final String illegalProjectName = "name/with/path/elements";
    private final String projectName = "project";
    private final long revision = 1;

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
            "sub/mod.use.js"    // expected in returned result
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

    private JavaScriptProjectFetcherImpl newInstance() {
        return new JavaScriptProjectFetcherImpl(subversionScmEndpoint);
    }
}
