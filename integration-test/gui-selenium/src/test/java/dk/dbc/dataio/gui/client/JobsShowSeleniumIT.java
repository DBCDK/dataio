package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class  JobsShowSeleniumIT extends AbstractGuiSeleniumTest {
    private static ConstantsProperties texts = new ConstantsProperties("pages/jobsshow/JobsShowConstants_dk.properties");

    private final static String DATAIO_JOB_STORE = "dataio-job-store";
    private final static String JOBINFO_FILE_NAME = "jobinfo.json";
    private TemporaryDataioJobstoreFolder jobstoreFolder;

    private final static Integer JOB_ID_OFFSET = 1000;
    private final static String FILE_NAME_PREFIX = "File_Name_";
    private final static Integer FILE_NAME_OFFSET = 3000;
    private final static Integer SUBMITTER_NUMBER_OFFSET = 2000;
    private final static Integer JOBS_SHOW_PAGE_SIZE = 20;

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
        assertThat(rowData.get(0).get(0), is(JOB_ID_2));   // Default sorting: Youngest first
        assertThat(rowData.get(0).get(1), is(FILE_NAME_2));
        assertThat(rowData.get(0).get(2), is(SUBMITTER_NUMBER_2));
        assertThat(rowData.get(1).get(0), is(JOB_ID_1));
        assertThat(rowData.get(1).get(1), is(FILE_NAME_1));
        assertThat(rowData.get(1).get(2), is(SUBMITTER_NUMBER_1));
    }

    @Test
    public void testJobs_MoreButtonVisible() throws IOException {
        navigateToJobsShowWidget(webDriver);
        assertTrue(findMoreButton(webDriver).isDisplayed());
    }

    @Test
    public void testJobsInsert21Rows_20ElementsShown() throws IOException {
        insertRows(webDriver, JOBS_SHOW_PAGE_SIZE + 1);
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> rowData = table.get();
        assertJobIdsDescending(rowData, JOBS_SHOW_PAGE_SIZE, JOBS_SHOW_PAGE_SIZE + 1);
    }

    @Test
    public void testJobsInsert21RowsAndClickMore_21ElementsShown() throws IOException {
        insertRows(webDriver, JOBS_SHOW_PAGE_SIZE + 1);
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        findMoreButton(webDriver).click();
        table.waitAssertRows(1);
        List<List<String>> rowData = table.get();
        assertJobIdsDescending(rowData, JOBS_SHOW_PAGE_SIZE + 1, JOBS_SHOW_PAGE_SIZE + 1);
    }

    @Test
    public void testColumnHeaderNames() throws IOException {
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        assertThat(table.findColumnHeader(0).getText(), is(texts.translate("columnHeader_JobId")));
        assertThat(table.findColumnHeader(1).getText(), is(texts.translate("columnHeader_FileName")));
        assertThat(table.findColumnHeader(2).getText(), is(texts.translate("columnHeader_SubmitterNumber")));
    }

    @Test
    public void testClickJobIdColumnHeader_OrderAccordingly() throws IOException {
        insertRows(webDriver, JOBS_SHOW_PAGE_SIZE + 1);
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Job Id Header Column makes column ascending
        table.findColumnHeader(0).click();  // First column is JobId column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();
        assertJobIdsAscending(firstRowData, JOBS_SHOW_PAGE_SIZE);

        // Second click on Job Id Header Column makes column descending
        table.findColumnHeader(0).click();  // First column is JobId column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();
        assertJobIdsDescending(secondRowData, JOBS_SHOW_PAGE_SIZE, JOBS_SHOW_PAGE_SIZE + 1);
    }

    @Test
    public void testClickFileNameColumnHeader_OrderAccordingly() throws IOException {
        insertRows(webDriver, JOBS_SHOW_PAGE_SIZE + 1);
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Filename Header Column makes Filename column ascending ( => Job Id column is also ascending)
        table.findColumnHeader(1).click();  // Second column is Filename column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();
        assertJobIdsAscending(firstRowData, JOBS_SHOW_PAGE_SIZE);

        // Second click on Filename Header Column makes Filename column descending ( => Job Id column is also descending)
        table.findColumnHeader(1).click();  // Second column is Filename column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();
        assertJobIdsDescending(secondRowData, JOBS_SHOW_PAGE_SIZE, JOBS_SHOW_PAGE_SIZE + 1);
    }

    @Test
    public void testClickSubmitterNumberColumnHeader_OrderAccordingly() throws IOException {
        insertRows(webDriver, JOBS_SHOW_PAGE_SIZE + 1);
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Submitternumber Header Column makes Submitternumber column ascending ( => Job Id column is descending)
        table.findColumnHeader(2).click();  // Third column is Submitternumber column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();
        assertJobIdsDescending(firstRowData, JOBS_SHOW_PAGE_SIZE, JOBS_SHOW_PAGE_SIZE + 1);

        // Second click on Submitternumber Header Column makes Submitternumber column descending ( => Job Id column is ascending)
        table.findColumnHeader(2).click();  // Third column is Submitternumber column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();
        assertJobIdsAscending(secondRowData, JOBS_SHOW_PAGE_SIZE);
    }

    @Test
    public void testEtEllerAndet() throws IOException {

        jobstoreFolder.createTestJob("1", "a", "1");
        jobstoreFolder.createTestJob("6", "a", "1");
        jobstoreFolder.createTestJob("4", "a", "2");
        jobstoreFolder.createTestJob("5", "a", "2");
        jobstoreFolder.createTestJob("3", "a", "3");
        jobstoreFolder.createTestJob("2", "a", "1");
        jobstoreFolder.createTestJob("7", "a", "1");

        navigateToJobsShowWidget(webDriver);

        try {Thread.sleep(120000);} catch (InterruptedException e) {}
    }

    /**
         * Private utility methods
         */
    private static void navigateToJobsShowWidget(WebDriver webDriver) {
        NavigationPanelSeleniumIT.navigateTo(webDriver, ClientFactoryImpl.GUIID_MENU_ITEM_JOBS_SHOW);
    }

    private static WebElement findMoreButton(WebDriver webDriver) {
        return SeleniumUtil.findElementInCurrentView(webDriver, JobsShowViewImpl.GUIID_JOBS_MORE_BUTTON);
    }

    private void insertRows(WebDriver webDriver, int count) throws IOException {
        for (int i=0; i<count; i++) {
            jobstoreFolder.createTestJob(Integer.toString(JOB_ID_OFFSET+i), FILE_NAME_PREFIX + Integer.toString(FILE_NAME_OFFSET+i), Integer.toString(SUBMITTER_NUMBER_OFFSET-i));
        }
    }

    private void assertJobIdsAscending(List<List<String>> tableData, int count) {
        assertThat(tableData.size(), is(count));
        for (int i=0; i<count; i++) {
            assertThat(tableData.get(i).get(0), is(Integer.toString(JOB_ID_OFFSET+i)));
            assertThat(tableData.get(i).get(1), is(FILE_NAME_PREFIX + Integer.toString(FILE_NAME_OFFSET+i)));
            assertThat(tableData.get(i).get(2), is(Integer.toString(SUBMITTER_NUMBER_OFFSET-i)));
        }
    }

    private void assertJobIdsDescending(List<List<String>> tableData, int count, int totalCount) {
        assertThat(tableData.size(), is(count));
        for (int i=0; i<count; i++) {
            int j = totalCount - 1 - i;
            assertThat(tableData.get(i).get(0), is(Integer.toString(JOB_ID_OFFSET+j)));
            assertThat(tableData.get(i).get(1), is(FILE_NAME_PREFIX + Integer.toString(FILE_NAME_OFFSET+j)));
            assertThat(tableData.get(i).get(2), is(Integer.toString(SUBMITTER_NUMBER_OFFSET-j)));
        }
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
