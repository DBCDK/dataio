package dk.dbc.dataio.commons.svn;

import dk.dbc.dataio.commons.types.RevisionInfo;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * SvnConnector unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SvnConnectorTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final long revision = 1;
    private final String illegalUrl = "|";
    private final String repositoryName = "repos";
    private final String projectName = "test-project";
    private final String helloFile = "hello.txt";
    private final String worldFile = "sub/world.txt";
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
        final SVNURL reposUrl = createTemporaryTestRepository();
        final SVNURL projectUrl = reposUrl.appendPath(projectName, false);

        List<RevisionInfo> revisions = SvnConnector.listAvailableRevisions(projectUrl.toDecodedString());
        Collections.reverse(revisions);

        assertThat(revisions.size(), greaterThanOrEqualTo(3));
        // Latest revision first
        assertThat(revisions.get(2).getRevision(), is(4L));
        assertThat(revisions.get(1).getRevision(), is(2L));
        assertThat(revisions.get(0).getRevision(), is(1L));
        // RevisionInfo content
        assertThat(revisions.get(1).getMessage(), is("commiting changes"));
        assertThat(revisions.get(1).getChangedItems().size(), is(1));
        assertThat(revisions.get(1).getChangedItems().get(0).getPath().endsWith(helloFile), is(true));
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
        final List<String> expectedPaths = new ArrayList<>(Arrays.asList("sub", worldFile, helloFile));
        final SVNURL reposUrl = createTemporaryTestRepository();
        final SVNURL projectUrl = reposUrl.appendPath(projectName, false);
        final List<String> paths = SvnConnector.listAvailablePaths(projectUrl.toDecodedString(), revision);
        assertThat(paths, is(expectedPaths));
    }

    @Test(expected = NullPointerException.class)
    public void export_projectUrlArgIsNull_throws() throws Exception {
        SvnConnector.export(null, revision, tempFolder.newFolder().toPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void export_projectUrlArgIsEmpty_throws() throws Exception {
        SvnConnector.export("", revision, tempFolder.newFolder().toPath());
    }

    @Test(expected = NullPointerException.class)
    public void export_exportToArgIsNull_throws() throws Exception {
        SvnConnector.export(projectName, revision, null);
    }

    @Test(expected = URISyntaxException.class)
    public void export_projectUrlIsIllegal_throws() throws Exception {
        SvnConnector.export(illegalUrl, revision, tempFolder.newFolder().toPath());
    }

    @Test(expected = SVNException.class)
    public void export_projectUrlRepositoryIsNonExisting_throws() throws Exception {
        SvnConnector.export(nonExistingRepositoryUrl, revision, tempFolder.newFolder().toPath());
    }

    @Test(expected = SVNException.class)
    public void export_projectUrlIsNonExisting_throws() throws Exception {
        final SVNURL reposUrl = createNewRepository();
        final SVNURL nonExistingProjectUrl = reposUrl.appendPath("no_such_project", false);
        SvnConnector.export(nonExistingProjectUrl.toDecodedString(), revision, tempFolder.newFolder().toPath());
    }

    @Test
    public void export_projectUrlPointsToEntireProject_exportsProject() throws Exception {
        tempFolder.newFolder("workspace");
        final File exportFolder = tempFolder.newFolder("export");

        final SVNURL reposUrl = createTemporaryTestRepository();
        final SVNURL projectUrl = reposUrl.appendPath(projectName, false);


        // export revision 4 which contains an external to
        SvnConnector.export(projectUrl.toDecodedString(), 4, exportFolder.toPath());

        // Get original file content from resources folder
        final List<String> expectedHelloFileContent = Arrays.asList("hello", "fisk");
        final List<String> expectedWorldFileContent = Collections.singletonList("world");


        // Get exported file content
        final List<String> exportedHelloFileContent = Files.readAllLines(
                FileSystems.getDefault().getPath(exportFolder.getPath(), helloFile), StandardCharsets.UTF_8);
        final List<String> exportedWorldFileContent = Files.readAllLines(
                FileSystems.getDefault().getPath(exportFolder.getPath(), worldFile), StandardCharsets.UTF_8);

        final List<String> files = fileList(exportFolder.toPath());

        final String[] expectedFiles = {"hello.txt", "sub"};

        assertThat(files, Matchers.containsInAnyOrder(expectedFiles));

        // Compare original file content with exported file content
        assertThat(exportedHelloFileContent.size(), is(expectedHelloFileContent.size()));
        assertThat(exportedHelloFileContent.get(0), is(expectedHelloFileContent.get(0)));
        assertThat(exportedWorldFileContent.size(), is(expectedWorldFileContent.size()));
        assertThat(exportedWorldFileContent.get(0), is(expectedWorldFileContent.get(0)));
    }

    @Test
    public void getRepository() throws Exception {
        final SVNURL repositoryUrl = createNewRepository();
        final SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString() + "/" + projectName);
        assertThat(repository, is(notNullValue()));
        repository.testConnection();
    }

    @Test
    public void dirExists_directoryDoesNotExistInRepository_returnsFalse() throws Exception {
        final SVNURL repositoryUrl = createTemporaryTestRepository();
        final SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
        assertThat(SvnConnector.dirExists(repository, "non-existing-dir"), is(false));
    }

    @Test
    public void dirExists_pathExistsInRepositoryButIsNotDirectory_returnsFalse() throws Exception {
        final SVNURL repositoryUrl = createTemporaryTestRepository();
        final SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
        assertThat(SvnConnector.dirExists(repository, projectName + "/" + helloFile), is(false));
    }

    @Test
    public void dirExists_repositoryArgIsNull_returnsFalse() throws Exception {
        assertThat(SvnConnector.dirExists(null, projectName), is(false));
    }

    @Test
    public void dirExists_dirArgIsNull_returnsFalse() throws Exception {
        final SVNURL repositoryUrl = createTemporaryTestRepository();
        final SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
        assertThat(SvnConnector.dirExists(repository, null), is(false));
    }

    @Test
    public void dirExists_directoryExistsInRepository_returnsTrue() throws Exception {
        final SVNURL repositoryUrl = createTemporaryTestRepository();
        final SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
        assertThat(SvnConnector.dirExists(repository, projectName), is(true));
    }

    private static List<String> fileList(Path directory) {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path path : directoryStream) {
                fileNames.add(path.getFileName().toString());
            }
        } catch (IOException ex) {
        }
        return fileNames;
    }

    @Test
    public void export_projectUrlPointsToSingleFile_exportsFile() throws Exception {
        final File exportFolder = tempFolder.newFolder("export");
        final SVNURL reposUrl = createTemporaryTestRepository();
        final SVNURL projectUrl = reposUrl.appendPath(projectName, false);
        final SVNURL fileUrl = projectUrl.appendPath(helloFile, false);

        // export first revision of file
        SvnConnector.export(fileUrl.toDecodedString(), revision, exportFolder.toPath());

        // Get original file content from resources folder
        final List<String> expectedHelloFileContent = Collections.singletonList("hello");

        // Get exported file content
        final List<String> exportedHelloFileContent = Files.readAllLines(
                FileSystems.getDefault().getPath(exportFolder.getPath(), helloFile), StandardCharsets.UTF_8);

        // Compare original file content with exported file content
        assertThat(exportedHelloFileContent.size(), is(expectedHelloFileContent.size()));
        assertThat(exportedHelloFileContent.get(0), is(expectedHelloFileContent.get(0)));
        assertThat(Files.notExists(FileSystems.getDefault().getPath(exportFolder.getPath(), worldFile)), is(true));
    }

    private SVNURL createTemporaryTestRepository() throws Exception {
        final File repoFileName = tempFolder.newFolder(repositoryName);
        final SVNAdminClient svnAdminClient = SVNClientManager.newInstance().getAdminClient();
        final SVNURL SVNRepo = svnAdminClient.doCreateRepository(repoFileName, null, true, false, false, false);

        final InputStream project = this.getClass().getResourceAsStream(String.format("/%s", "test-repository.dump"));
        svnAdminClient.doLoad(repoFileName, project);

        return SVNRepo;
    }

    private SVNURL createNewRepository() throws Exception {
        final File newFolder = tempFolder.newFolder(repositoryName);
        return SVNRepositoryFactory.createLocalRepository(newFolder, true, true);
    }
}
