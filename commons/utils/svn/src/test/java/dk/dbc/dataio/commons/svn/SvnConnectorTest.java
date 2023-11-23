package dk.dbc.dataio.commons.svn;

import dk.dbc.dataio.commons.types.RevisionInfo;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SvnConnector unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
public class SvnConnectorTest {
    private final long revision = 1;
    private final String illegalUrl = "|";
    private final String repositoryName = "repos";
    private final String projectName = "test-project";
    private final String helloFile = "hello.txt";
    private final String worldFile = "sub/world.txt";
    private final String nonExistingRepositoryUrl = "file://no_such_repository/no_such_project";

    @Test
    public void listAvailableRevisions_projectUrlArgIsNull_throws() {
        Assertions.assertThrows(NullPointerException.class, () -> SvnConnector.listAvailableRevisions(null));
    }

    @Test
    public void listAvailableRevisions_projectUrlArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> SvnConnector.listAvailableRevisions(""));
    }

    @Test
    public void listAvailableRevisions_projectUrlIsIllegal_throws() {
        assertThrows(URISyntaxException.class, () -> SvnConnector.listAvailableRevisions(illegalUrl));
    }

    @Test
    public void listAvailableRevisions_projectUrlRepositoryIsNonExisting_throws() {
        assertThrows(SVNException.class, () -> SvnConnector.listAvailableRevisions(nonExistingRepositoryUrl));
    }

    @Test
    public void listAvailableRevisions_projectUrlIsNonExisting_throws() throws Exception {
        SVNURL reposUrl = createNewRepository();
        SVNURL nonExistingProjectUrl = reposUrl.appendPath("no_such_project", false);
        assertThrows(SVNException.class, () -> SvnConnector.listAvailableRevisions(nonExistingProjectUrl.toDecodedString()));
    }

    @Test
    public void listAvailableRevisions_projectUrlExists_returnsListOfRevisions() throws Exception {
        SVNURL reposUrl = createTemporaryTestRepository();
        SVNURL projectUrl = reposUrl.appendPath(projectName, false);

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

    @Test
    public void listAvailablePaths_projectUrlArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> SvnConnector.listAvailablePaths(null, revision));
    }

    @Test
    public void listAvailablePaths_projectUrlArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> SvnConnector.listAvailablePaths("", revision));
    }

    @Test
    public void listAvailablePaths_projectUrlIsIllegal_throws() {
        assertThrows(URISyntaxException.class, () -> SvnConnector.listAvailablePaths(illegalUrl, revision));
    }

    @Test
    public void listAvailablePaths_projectUrlRepositoryIsNonExisting_throws() throws Exception {
        assertThrows(SVNException.class, () -> SvnConnector.listAvailablePaths(nonExistingRepositoryUrl, revision));
    }

    @Test
    public void listAvailablePaths_projectUrlIsNonExisting_throws() throws Exception {
        SVNURL reposUrl = createNewRepository();
        SVNURL nonExistingProjectUrl = reposUrl.appendPath("no_such_project", false);
        assertThrows(SVNException.class, () -> SvnConnector.listAvailablePaths(nonExistingProjectUrl.toDecodedString(), revision));
    }

    @Test
    public void listAvailablePaths_projectUrlExists_returnsListOfProjectPaths() throws Exception {
        List<String> expectedPaths = new ArrayList<>(Arrays.asList("sub", worldFile, helloFile));
        SVNURL reposUrl = createTemporaryTestRepository();
        SVNURL projectUrl = reposUrl.appendPath(projectName, false);
        List<String> paths = SvnConnector.listAvailablePaths(projectUrl.toDecodedString(), revision);
        assertThat(paths, is(expectedPaths));
    }

    @Test
    public void export_projectUrlArgIsNull_throws() throws Exception {
        assertThrows(NullPointerException.class, () -> SvnConnector.export(null, revision,createTempFolder()));
    }

    private Path createTempFolder() throws IOException {
        return Files.createTempDirectory("svn-test_");
    }

    @Test
    public void export_projectUrlArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> SvnConnector.export("", revision, createTempFolder()));
    }

    @Test
    public void export_exportToArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> SvnConnector.export(projectName, revision, null));
    }

    @Test
    public void export_projectUrlIsIllegal_throws() throws Exception {
        assertThrows(URISyntaxException.class, () -> SvnConnector.export(illegalUrl, revision, createTempFolder()));
    }

    @Test
    public void export_projectUrlRepositoryIsNonExisting_throws() {
        assertThrows(SVNException.class, () -> SvnConnector.export(nonExistingRepositoryUrl, revision, createTempFolder()));
    }

    @Test
    public void export_projectUrlIsNonExisting_throws() throws Exception {
        SVNURL reposUrl = createNewRepository();
        SVNURL nonExistingProjectUrl = reposUrl.appendPath("no_such_project", false);
        assertThrows(SVNException.class, () -> SvnConnector.export(nonExistingProjectUrl.toDecodedString(), revision, createTempFolder()));
    }

    @Test
    public void export_projectUrlPointsToEntireProject_exportsProject() throws Exception {
        Path tempFolder = createTempFolder();
        Files.createDirectory(tempFolder.resolve("workspace"));
        Path exportFolder = Files.createDirectory(tempFolder.resolve("export"));

        SVNURL reposUrl = createTemporaryTestRepository();
        SVNURL projectUrl = reposUrl.appendPath(projectName, false);


        // export revision 4 which contains an external to
        SvnConnector.export(projectUrl.toDecodedString(), 4, exportFolder);

        // Get original file content from resources folder
        List<String> expectedHelloFileContent = Arrays.asList("hello", "fisk");
        List<String> expectedWorldFileContent = Collections.singletonList("world");


        // Get exported file content
        List<String> exportedHelloFileContent = Files.readAllLines(exportFolder.resolve(helloFile), StandardCharsets.UTF_8);
        List<String> exportedWorldFileContent = Files.readAllLines(exportFolder.resolve(worldFile), StandardCharsets.UTF_8);

        List<String> files = fileList(exportFolder);

        String[] expectedFiles = {"hello.txt", "sub"};

        assertThat(files, Matchers.containsInAnyOrder(expectedFiles));

        // Compare original file content with exported file content
        assertThat(exportedHelloFileContent.size(), is(expectedHelloFileContent.size()));
        assertThat(exportedHelloFileContent.get(0), is(expectedHelloFileContent.get(0)));
        assertThat(exportedWorldFileContent.size(), is(expectedWorldFileContent.size()));
        assertThat(exportedWorldFileContent.get(0), is(expectedWorldFileContent.get(0)));
    }

    @Test
    public void getRepository() throws Exception {
        SVNURL repositoryUrl = createNewRepository();
        SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString() + "/" + projectName);
        assertThat(repository, is(notNullValue()));
        repository.testConnection();
    }

    @Test
    public void dirExists_directoryDoesNotExistInRepository_returnsFalse() throws Exception {
        SVNURL repositoryUrl = createTemporaryTestRepository();
        SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
        assertThat(SvnConnector.dirExists(repository, "non-existing-dir"), is(false));
    }

    @Test
    public void dirExists_pathExistsInRepositoryButIsNotDirectory_returnsFalse() throws Exception {
        SVNURL repositoryUrl = createTemporaryTestRepository();
        SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
        assertThat(SvnConnector.dirExists(repository, projectName + "/" + helloFile), is(false));
    }

    @Test
    public void dirExists_repositoryArgIsNull_returnsFalse() throws Exception {
        assertThat(SvnConnector.dirExists(null, projectName), is(false));
    }

    @Test
    public void dirExists_dirArgIsNull_returnsFalse() throws Exception {
        SVNURL repositoryUrl = createTemporaryTestRepository();
        SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
        assertThat(SvnConnector.dirExists(repository, null), is(false));
    }

    @Test
    public void dirExists_directoryExistsInRepository_returnsTrue() throws Exception {
        SVNURL repositoryUrl = createTemporaryTestRepository();
        SVNRepository repository = SvnConnector.getRepository(repositoryUrl.toString());
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
        Path exportFolder = createTempFolder();
        SVNURL reposUrl = createTemporaryTestRepository();
        SVNURL projectUrl = reposUrl.appendPath(projectName, false);
        SVNURL fileUrl = projectUrl.appendPath(helloFile, false);

        // export first revision of file
        SvnConnector.export(fileUrl.toDecodedString(), revision, exportFolder);

        // Get original file content from resources folder
        List<String> expectedHelloFileContent = Collections.singletonList("hello");

        // Get exported file content
        List<String> exportedHelloFileContent = Files.readAllLines(exportFolder.resolve(helloFile));

        // Compare original file content with exported file content
        assertThat(exportedHelloFileContent.size(), is(expectedHelloFileContent.size()));
        assertThat(exportedHelloFileContent.get(0), is(expectedHelloFileContent.get(0)));
        assertThat(Files.notExists(exportFolder.resolve(worldFile)), is(true));
    }

    private SVNURL createTemporaryTestRepository() throws Exception {
        File repoFileName = createTempFolder().toFile();
        SVNAdminClient svnAdminClient = SVNClientManager.newInstance().getAdminClient();
        SVNURL SVNRepo = svnAdminClient.doCreateRepository(repoFileName, null, true, false, false, false);

        InputStream project = getClass().getResourceAsStream(String.format("/%s", "test-repository.dump"));
        svnAdminClient.doLoad(repoFileName, project);

        return SVNRepo;
    }

    private SVNURL createNewRepository() throws Exception {
        File newFolder = createTempFolder().toFile();
        return SVNRepositoryFactory.createLocalRepository(newFolder, true, true);
    }
}
