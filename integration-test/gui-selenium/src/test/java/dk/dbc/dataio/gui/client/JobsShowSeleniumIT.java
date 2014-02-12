package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

public class JobsShowSeleniumIT extends AbstractGuiSeleniumTest {
    private final static String DATAIO_JOB_STORE = "dataio-job-store";
    private final static String JOBINFO_FILE_NAME = "jobinfo.json";
    private TemporaryDataioJobstoreFolder jobstoreFolder;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void createTempFolders() throws IOException {
        Path dataioPath = Paths.get(tmpFolder.getRoot().toPath().getParent().toString(), DATAIO_JOB_STORE);
        jobstoreFolder = new TemporaryDataioJobstoreFolder(dataioPath);
    }

    @After
    public void deleteTempFolders() throws IOException {
        if (jobstoreFolder != null) {
            jobstoreFolder.delete();
        }
    }

    @Test
    public void testJobsShowEmptyList_NoContentIsShown() throws TimeoutException, Exception {
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertNoRows();
    }

    @Test
    public void testJobsInsertTwoRows_TwoElementsShown() throws IOException {
        final String JOB_ID_1 = "11234";
        final String FILE_NAME_1 = "File_name_one";
        final String SUBMITTER_NUMBER_1 = "111";
        final String JOB_ID_2 = "12234";
        final String FILE_NAME_2 = "File_name_two";
        final String SUBMITTER_NUMBER_2 = "222";
        jobstoreFolder.createTestJob(JOB_ID_1, FILE_NAME_1, SUBMITTER_NUMBER_1);
        jobstoreFolder.createTestJob(JOB_ID_2, FILE_NAME_2, SUBMITTER_NUMBER_2);
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();
        assertThat(rowData.get(0).get(0), is(JOB_ID_1));
        assertThat(rowData.get(0).get(1), is(FILE_NAME_1));
        assertThat(rowData.get(0).get(2), is(SUBMITTER_NUMBER_1));
        assertThat(rowData.get(1).get(0), is(JOB_ID_2));
        assertThat(rowData.get(1).get(1), is(FILE_NAME_2));
        assertThat(rowData.get(1).get(2), is(SUBMITTER_NUMBER_2));
    }

    private static void navigateToJobsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_JOBS_SHOW);
    }




    /**
     * Class for maintaining temporary job storage in file system
     */
    private class TemporaryDataioJobstoreFolder {
        Path jobFolderRoot = null;

        public TemporaryDataioJobstoreFolder(Path rootPath) throws IOException {
            jobFolderRoot = rootPath;
            if (!Files.exists(rootPath)) {
                jobFolderRoot = Files.createDirectory(jobFolderRoot);
            }
        }

        private void delete() throws IOException {
            Files.walkFileTree(jobFolderRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    if (e == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        throw e;
                    }
                }
            });
            jobFolderRoot = null;
        }

        private void createFile(String folderName, String fileName, String fileContent) throws IOException {
            Path fileFolder = Paths.get(jobFolderRoot.toString(), folderName);
            Path filePath = Paths.get(fileFolder.toString(), fileName);
            if (Files.notExists(fileFolder)) {
                Files.createDirectory(fileFolder);
            }
            try (PrintWriter printWriter = new PrintWriter(filePath.toFile())) {
                printWriter.print(fileContent);
            }
        }

        private void createTestJob(String jobId, String fileName, String submitterNumber) throws IOException {
            // Build JobSpecification JSON
            JobSpecificationJsonBuilder jobSpecificationBuider = new JobSpecificationJsonBuilder();
            jobSpecificationBuider.setDataFile(fileName);
            jobSpecificationBuider.setSubmitterId(Long.parseLong(submitterNumber));
            // Build  JobInfo JSON
            JobInfoJsonBuilder jobInfoBuilder = new JobInfoJsonBuilder();
            jobInfoBuilder.setJobId(Long.parseLong(jobId));
            jobInfoBuilder.setJobSpecification(jobSpecificationBuider.build());
            // Store JobInfo in file system based job-store
            createFile(jobId, JOBINFO_FILE_NAME, jobInfoBuilder.build());
        }

    }
}
