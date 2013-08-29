package dk.dbc.dataio.commons.svn;

import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.integrationtest.ITUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * SvnConnector unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class SvnConnectorTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final long revision = 1;
    private final String illegalUrl = "|";
    private final String repositoryName = "repos";
    private final String projectName = "test-project";
    private final String projectPath1 = String.format("%s/sub", projectName);
    private final String projectPath2 = String.format("%s/sub/world.txt", projectName);
    private final String projectPath3 = String.format("%s/hello.txt", projectName);
    private final String nonExistingRepositoryUrl = "file://no_such_repository/no_such_project";

    @Test(expected = NullPointerException.class)
    public void listAvailableRevisions_projectUrlArgIsNull_throws() throws Exception {
         SvnConnector.listAvailableRevisions(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void listAvailableRevisions_projectUrlArgIsEmpty_throws() throws Exception {
        SvnConnector.listAvailableRevisions("");
    }

    @Test(expected = URISyntaxException.class)
    public void listAvailableRevisions_projectUrlIsIllegal_throws() throws Exception {
        SvnConnector.listAvailableRevisions(illegalUrl);
    }

    @Test(expected = SVNException.class)
    public void listAvailableRevisions_projectUrlRepositoryIsNonExisting_throws() throws Exception {
        SvnConnector.listAvailableRevisions(nonExistingRepositoryUrl);
    }

    @Test(expected = SVNException.class)
    public void listAvailableRevisions_projectUrlIsNonExisting_throws() throws Exception {
        final SVNURL reposUrl = createNewRepository();
        final SVNURL nonExistingProjectUrl = reposUrl.appendPath("no_such_project", false);
        SvnConnector.listAvailableRevisions(nonExistingProjectUrl.toDecodedString());
    }

    @Test
    public void listAvailableRevisions_projectUrlExists_returnsListOfRevisions() throws Exception {
        final String filename = "hello.txt";
        final String logMessage = "commiting changes";
        final File checkoutFolder = tempFolder.newFolder("workspace");
        final SVNURL reposUrl = createNewRepository();
        final SVNURL projectUrl = importProject(reposUrl);

        // Modify file and commit to force second revision
        ITUtil.doSvnCheckout(projectUrl, checkoutFolder.toPath());
        appendToFile(FileSystems.getDefault().getPath(checkoutFolder.getPath(), filename), "some data");
        ITUtil.doSvnCommit(checkoutFolder.toPath(), logMessage);

        final List<RevisionInfo> revisions = SvnConnector.listAvailableRevisions(projectUrl.toDecodedString());
        assertThat(revisions.size(), is(2));
        // Latest revision first
        assertThat(revisions.get(0).getRevision(), is(2L));
        assertThat(revisions.get(1).getRevision(), is(1L));
        // RevisionInfo content
        assertThat(revisions.get(0).getMessage(), is(logMessage));
        assertThat(revisions.get(0).getChangedItems().size(), is(1));
        assertThat(revisions.get(0).getChangedItems().get(0).getPath().endsWith(filename), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void listAvailablePaths_projectUrlArgIsNull_throws() throws Exception {
         SvnConnector.listAvailablePaths(null, revision);
    }

    @Test(expected = IllegalArgumentException.class)
    public void listAvailablePaths_projectUrlArgIsEmpty_throws() throws Exception {
        SvnConnector.listAvailablePaths("", revision);
    }

    @Test(expected = URISyntaxException.class)
    public void listAvailablePaths_projectUrlIsIllegal_throws() throws Exception {
        SvnConnector.listAvailablePaths(illegalUrl, revision);
    }

    @Test(expected = SVNException.class)
    public void listAvailablePaths_projectUrlRepositoryIsNonExisting_throws() throws Exception {
        SvnConnector.listAvailablePaths(nonExistingRepositoryUrl, revision);
    }

    @Test(expected = SVNException.class)
    public void listAvailablePaths_projectUrlIsNonExisting_throws() throws Exception {
        final SVNURL reposUrl = createNewRepository();
        final SVNURL nonExistingProjectUrl = reposUrl.appendPath("no_such_project", false);
        SvnConnector.listAvailablePaths(nonExistingProjectUrl.toDecodedString(), revision);
    }

    @Test
    public void listAvailablePaths_projectUrlExists_returnsListOfProjectPaths() throws Exception {
        final List<String> expectedPaths = new ArrayList<>(Arrays.asList(projectPath1, projectPath2, projectPath3));
        final SVNURL reposUrl = createNewRepository();
        final SVNURL projectUrl = importProject(reposUrl);
        final List<String> paths = SvnConnector.listAvailablePaths(projectUrl.toDecodedString(), revision);
        assertThat(paths, is(expectedPaths));
    }

    private SVNURL createNewRepository() throws Exception {
        final File repos = tempFolder.newFolder(repositoryName);
        return ITUtil.doSvnCreateFsRepository(repos.toPath());
    }

    private SVNURL importProject(final SVNURL reposUrl) throws Exception {
        final SVNURL projectUrl = reposUrl.appendPath(projectName, false);
        final URL project = this.getClass().getResource(String.format("/%s", projectName));
        ITUtil.doSvnImport(projectUrl, Paths.get(project.toURI()), "initial import");
        return projectUrl;
    }

    private void appendToFile(final Path filename, final String data) throws Exception {
        try (PrintWriter output = new PrintWriter(new FileWriter(filename.toString(), true))) {
            output.print(data);
        }
    }
}
