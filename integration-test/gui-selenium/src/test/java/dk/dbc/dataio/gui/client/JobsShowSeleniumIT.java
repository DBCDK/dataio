package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
    public void testJobsInsertTwoPageData_OnePageElementsShown() throws IOException {
        // Insert 21 rows of job data
        jobstoreFolder.createTestJob("100", "Fil00", "220");
        jobstoreFolder.createTestJob("101", "Fil01", "219");
        jobstoreFolder.createTestJob("102", "Fil02", "218");
        jobstoreFolder.createTestJob("103", "Fil03", "217");
        jobstoreFolder.createTestJob("104", "Fil04", "216");
        jobstoreFolder.createTestJob("105", "Fil05", "215");
        jobstoreFolder.createTestJob("106", "Fil06", "214");
        jobstoreFolder.createTestJob("107", "Fil07", "213");
        jobstoreFolder.createTestJob("108", "Fil08", "212");
        jobstoreFolder.createTestJob("109", "Fil09", "211");
        jobstoreFolder.createTestJob("110", "Fil10", "210");
        jobstoreFolder.createTestJob("111", "Fil11", "209");
        jobstoreFolder.createTestJob("112", "Fil12", "208");
        jobstoreFolder.createTestJob("113", "Fil13", "207");
        jobstoreFolder.createTestJob("114", "Fil14", "206");
        jobstoreFolder.createTestJob("115", "Fil15", "205");
        jobstoreFolder.createTestJob("116", "Fil16", "204");
        jobstoreFolder.createTestJob("117", "Fil17", "203");
        jobstoreFolder.createTestJob("118", "Fil18", "202");
        jobstoreFolder.createTestJob("119", "Fil19", "201");
        jobstoreFolder.createTestJob("120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> rowData = table.get();

        // Assert 20 rows
        int i = 0;
        assertTableRow(rowData.get(i++), "120", "Fil20", "200");
        assertTableRow(rowData.get(i++), "119", "Fil19", "201");
        assertTableRow(rowData.get(i++), "118", "Fil18", "202");
        assertTableRow(rowData.get(i++), "117", "Fil17", "203");
        assertTableRow(rowData.get(i++), "116", "Fil16", "204");
        assertTableRow(rowData.get(i++), "115", "Fil15", "205");
        assertTableRow(rowData.get(i++), "114", "Fil14", "206");
        assertTableRow(rowData.get(i++), "113", "Fil13", "207");
        assertTableRow(rowData.get(i++), "112", "Fil12", "208");
        assertTableRow(rowData.get(i++), "111", "Fil11", "209");
        assertTableRow(rowData.get(i++), "110", "Fil10", "210");
        assertTableRow(rowData.get(i++), "109", "Fil09", "211");
        assertTableRow(rowData.get(i++), "108", "Fil08", "212");
        assertTableRow(rowData.get(i++), "107", "Fil07", "213");
        assertTableRow(rowData.get(i++), "106", "Fil06", "214");
        assertTableRow(rowData.get(i++), "105", "Fil05", "215");
        assertTableRow(rowData.get(i++), "104", "Fil04", "216");
        assertTableRow(rowData.get(i++), "103", "Fil03", "217");
        assertTableRow(rowData.get(i++), "102", "Fil02", "218");
        assertTableRow(rowData.get(i++), "101", "Fil01", "219");
    }

    @Test
    public void testJobsInsert2PagesDataAndClickMore_CorrectNumberOfElementsShown() throws IOException {
        // Insert 21 rows of job data
        jobstoreFolder.createTestJob("100", "Fil00", "220");
        jobstoreFolder.createTestJob("101", "Fil01", "219");
        jobstoreFolder.createTestJob("102", "Fil02", "218");
        jobstoreFolder.createTestJob("103", "Fil03", "217");
        jobstoreFolder.createTestJob("104", "Fil04", "216");
        jobstoreFolder.createTestJob("105", "Fil05", "215");
        jobstoreFolder.createTestJob("106", "Fil06", "214");
        jobstoreFolder.createTestJob("107", "Fil07", "213");
        jobstoreFolder.createTestJob("108", "Fil08", "212");
        jobstoreFolder.createTestJob("109", "Fil09", "211");
        jobstoreFolder.createTestJob("110", "Fil10", "210");
        jobstoreFolder.createTestJob("111", "Fil11", "209");
        jobstoreFolder.createTestJob("112", "Fil12", "208");
        jobstoreFolder.createTestJob("113", "Fil13", "207");
        jobstoreFolder.createTestJob("114", "Fil14", "206");
        jobstoreFolder.createTestJob("115", "Fil15", "205");
        jobstoreFolder.createTestJob("116", "Fil16", "204");
        jobstoreFolder.createTestJob("117", "Fil17", "203");
        jobstoreFolder.createTestJob("118", "Fil18", "202");
        jobstoreFolder.createTestJob("119", "Fil19", "201");
        jobstoreFolder.createTestJob("120", "Fil20", "200");

        // Navigate to Jobs Show List and click More
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        findMoreButton(webDriver).click();
        table.waitAssertRows(1);
        List<List<String>> rowData = table.get();

        // Assert 21 rows
        int i = 0;
        assertTableRow(rowData.get(i++), "120", "Fil20", "200");
        assertTableRow(rowData.get(i++), "119", "Fil19", "201");
        assertTableRow(rowData.get(i++), "118", "Fil18", "202");
        assertTableRow(rowData.get(i++), "117", "Fil17", "203");
        assertTableRow(rowData.get(i++), "116", "Fil16", "204");
        assertTableRow(rowData.get(i++), "115", "Fil15", "205");
        assertTableRow(rowData.get(i++), "114", "Fil14", "206");
        assertTableRow(rowData.get(i++), "113", "Fil13", "207");
        assertTableRow(rowData.get(i++), "112", "Fil12", "208");
        assertTableRow(rowData.get(i++), "111", "Fil11", "209");
        assertTableRow(rowData.get(i++), "110", "Fil10", "210");
        assertTableRow(rowData.get(i++), "109", "Fil09", "211");
        assertTableRow(rowData.get(i++), "108", "Fil08", "212");
        assertTableRow(rowData.get(i++), "107", "Fil07", "213");
        assertTableRow(rowData.get(i++), "106", "Fil06", "214");
        assertTableRow(rowData.get(i++), "105", "Fil05", "215");
        assertTableRow(rowData.get(i++), "104", "Fil04", "216");
        assertTableRow(rowData.get(i++), "103", "Fil03", "217");
        assertTableRow(rowData.get(i++), "102", "Fil02", "218");
        assertTableRow(rowData.get(i++), "101", "Fil01", "219");
        assertTableRow(rowData.get(i++), "100", "Fil00", "220");
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
        // Insert 21 rows of job data
        jobstoreFolder.createTestJob("100", "Fil00", "220");
        jobstoreFolder.createTestJob("101", "Fil01", "219");
        jobstoreFolder.createTestJob("102", "Fil02", "218");
        jobstoreFolder.createTestJob("103", "Fil03", "217");
        jobstoreFolder.createTestJob("104", "Fil04", "216");
        jobstoreFolder.createTestJob("105", "Fil05", "215");
        jobstoreFolder.createTestJob("106", "Fil06", "214");
        jobstoreFolder.createTestJob("107", "Fil07", "213");
        jobstoreFolder.createTestJob("108", "Fil08", "212");
        jobstoreFolder.createTestJob("109", "Fil09", "211");
        jobstoreFolder.createTestJob("110", "Fil10", "210");
        jobstoreFolder.createTestJob("111", "Fil11", "209");
        jobstoreFolder.createTestJob("112", "Fil12", "208");
        jobstoreFolder.createTestJob("113", "Fil13", "207");
        jobstoreFolder.createTestJob("114", "Fil14", "206");
        jobstoreFolder.createTestJob("115", "Fil15", "205");
        jobstoreFolder.createTestJob("116", "Fil16", "204");
        jobstoreFolder.createTestJob("117", "Fil17", "203");
        jobstoreFolder.createTestJob("118", "Fil18", "202");
        jobstoreFolder.createTestJob("119", "Fil19", "201");
        jobstoreFolder.createTestJob("120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // No need to click on Job Id Header Column - it is selectd by default, and column is descending
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - descending job id's
        int i = 0;
        assertTableRow(firstRowData.get(i++), "120", "Fil20", "200");
        assertTableRow(firstRowData.get(i++), "119", "Fil19", "201");
        assertTableRow(firstRowData.get(i++), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), "101", "Fil01", "219");

        // Second click on Job Id Header Column makes column ascending
        table.findColumnHeader(0).click();  // First column is JobId column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();

        // Assert correct order of 20 rows - ascending job id's
        i = 0;
        assertTableRow(secondRowData.get(i++), "100", "Fil00", "220");
        assertTableRow(secondRowData.get(i++), "101", "Fil01", "219");
        assertTableRow(secondRowData.get(i++), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), "119", "Fil19", "201");
    }

    @Test
    public void testClickFileNameColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        jobstoreFolder.createTestJob("100", "Fil00", "220");
        jobstoreFolder.createTestJob("101", "Fil01", "219");
        jobstoreFolder.createTestJob("102", "Fil02", "218");
        jobstoreFolder.createTestJob("103", "Fil03", "217");
        jobstoreFolder.createTestJob("104", "Fil04", "216");
        jobstoreFolder.createTestJob("105", "Fil05", "215");
        jobstoreFolder.createTestJob("106", "Fil06", "214");
        jobstoreFolder.createTestJob("107", "Fil07", "213");
        jobstoreFolder.createTestJob("108", "Fil08", "212");
        jobstoreFolder.createTestJob("109", "Fil09", "211");
        jobstoreFolder.createTestJob("110", "Fil10", "210");
        jobstoreFolder.createTestJob("111", "Fil11", "209");
        jobstoreFolder.createTestJob("112", "Fil12", "208");
        jobstoreFolder.createTestJob("113", "Fil13", "207");
        jobstoreFolder.createTestJob("114", "Fil14", "206");
        jobstoreFolder.createTestJob("115", "Fil15", "205");
        jobstoreFolder.createTestJob("116", "Fil16", "204");
        jobstoreFolder.createTestJob("117", "Fil17", "203");
        jobstoreFolder.createTestJob("118", "Fil18", "202");
        jobstoreFolder.createTestJob("119", "Fil19", "201");
        jobstoreFolder.createTestJob("120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Filename Header Column makes Filename column ascending ( => Job Id column is also ascending)
        table.findColumnHeader(1).click();  // Second column is Filename column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending file names
        int i = 0;
        assertTableRow(firstRowData.get(i++), "100", "Fil00", "220");
        assertTableRow(firstRowData.get(i++), "101", "Fil01", "219");
        assertTableRow(firstRowData.get(i++), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), "119", "Fil19", "201");

        // Second click on Filename Header Column makes Filename column descending ( => Job Id column is also descending)
        table.findColumnHeader(1).click();  // Second column is Filename column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();

        // Assert correct order of 20 rows - descending file names
        i = 0;
        assertTableRow(secondRowData.get(i++), "120", "Fil20", "200");
        assertTableRow(secondRowData.get(i++), "119", "Fil19", "201");
        assertTableRow(secondRowData.get(i++), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), "101", "Fil01", "219");
    }

    @Test
    public void testClickSubmitterNumberColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        jobstoreFolder.createTestJob("100", "Fil00", "220");
        jobstoreFolder.createTestJob("101", "Fil01", "219");
        jobstoreFolder.createTestJob("102", "Fil02", "218");
        jobstoreFolder.createTestJob("103", "Fil03", "217");
        jobstoreFolder.createTestJob("104", "Fil04", "216");
        jobstoreFolder.createTestJob("105", "Fil05", "215");
        jobstoreFolder.createTestJob("106", "Fil06", "214");
        jobstoreFolder.createTestJob("107", "Fil07", "213");
        jobstoreFolder.createTestJob("108", "Fil08", "212");
        jobstoreFolder.createTestJob("109", "Fil09", "211");
        jobstoreFolder.createTestJob("110", "Fil10", "210");
        jobstoreFolder.createTestJob("111", "Fil11", "209");
        jobstoreFolder.createTestJob("112", "Fil12", "208");
        jobstoreFolder.createTestJob("113", "Fil13", "207");
        jobstoreFolder.createTestJob("114", "Fil14", "206");
        jobstoreFolder.createTestJob("115", "Fil15", "205");
        jobstoreFolder.createTestJob("116", "Fil16", "204");
        jobstoreFolder.createTestJob("117", "Fil17", "203");
        jobstoreFolder.createTestJob("118", "Fil18", "202");
        jobstoreFolder.createTestJob("119", "Fil19", "201");
        jobstoreFolder.createTestJob("120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Submitternumber Header Column makes Submitternumber column ascending
        table.findColumnHeader(2).click();  // Third column is Submitternumber column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending submitter numbers
        int i = 0;
        assertTableRow(firstRowData.get(i++), "120", "Fil20", "200");
        assertTableRow(firstRowData.get(i++), "119", "Fil19", "201");
        assertTableRow(firstRowData.get(i++), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), "101", "Fil01", "219");

        // Second click on Submitternumber Header Column makes Submitternumber column descending
        table.findColumnHeader(2).click();  // Third column is Submitternumber column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();

        // Assert correct order of 20 rows - descending submitter numbers
        i = 0;
        assertTableRow(secondRowData.get(i++), "100", "Fil00", "220");
        assertTableRow(secondRowData.get(i++), "101", "Fil01", "219");
        assertTableRow(secondRowData.get(i++), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), "119", "Fil19", "201");
    }

    @Test
    public void testEqualFilenames_ClickOnceOnFileNameColumn_SortAccordingly() throws IOException {
        jobstoreFolder.createTestJob("1", "b", "2");
        jobstoreFolder.createTestJob("2", "a", "1");
        jobstoreFolder.createTestJob("3", "a", "2");
        jobstoreFolder.createTestJob("4", "b", "1");
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(1).click();  // Second column is Filename column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), "3", "a", "2");
        assertTableRow(tableData.get(i++), "2", "a", "1");
        assertTableRow(tableData.get(i++), "4", "b", "1");
        assertTableRow(tableData.get(i++), "1", "b", "2");

    }

    @Test
    public void testEqualFilenames_ClickTwiceOnFileNameColumn_SortAccordingly() throws IOException {
        jobstoreFolder.createTestJob("1", "b", "2");
        jobstoreFolder.createTestJob("2", "a", "1");
        jobstoreFolder.createTestJob("3", "a", "2");
        jobstoreFolder.createTestJob("4", "b", "1");
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(1).click();  // Second column is Filename column => Ascending
        table.findColumnHeader(1).click();  // Second column is Filename column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), "4", "b", "1");
        assertTableRow(tableData.get(i++), "1", "b", "2");
        assertTableRow(tableData.get(i++), "3", "a", "2");
        assertTableRow(tableData.get(i++), "2", "a", "1");

    }

    @Test
    public void testEqualSubmitterNumbers_ClickOnceOnSubmitterNumberColumn_SortAccordingly() throws IOException {
        jobstoreFolder.createTestJob("1", "b", "2");
        jobstoreFolder.createTestJob("2", "a", "1");
        jobstoreFolder.createTestJob("3", "a", "2");
        jobstoreFolder.createTestJob("4", "b", "1");
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(2).click();  // Third column is Submitternumber column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), "4", "b", "1");
        assertTableRow(tableData.get(i++), "2", "a", "1");
        assertTableRow(tableData.get(i++), "3", "a", "2");
        assertTableRow(tableData.get(i++), "1", "b", "2");

    }

    @Test
    public void testEqualSubmitterNumbers_ClickTwiceOnSubmitterNumberColumn_SortAccordingly() throws IOException {
        jobstoreFolder.createTestJob("1", "b", "2");
        jobstoreFolder.createTestJob("2", "a", "1");
        jobstoreFolder.createTestJob("3", "a", "2");
        jobstoreFolder.createTestJob("4", "b", "1");
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(2).click();  // Third column is Submitternumber column => Ascending
        table.findColumnHeader(2).click();  // Third column is Submitternumber column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), "3", "a", "2");
        assertTableRow(tableData.get(i++), "1", "b", "2");
        assertTableRow(tableData.get(i++), "4", "b", "1");
        assertTableRow(tableData.get(i++), "2", "a", "1");

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

    private void assertTableRow(List<String> row, String jobId, String fileName, String submitterId) {
        assertThat(row.get(0), is(jobId));
        assertThat(row.get(1), is(fileName));
        assertThat(row.get(2), is(submitterId));
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

        private void createTestJob(String jobId, final String fileName, String submitterNumber) throws IOException {
            // Build JobSpecification JSON
            JobSpecificationJsonBuilder jobSpecificationBuider = new JobSpecificationJsonBuilder();
            jobSpecificationBuider.setDataFile(fileName);
            jobSpecificationBuider.setSubmitterId(Long.parseLong(submitterNumber));
            // Build  JobInfo JSON
            JobInfoJsonBuilder jobInfoBuilder = new JobInfoJsonBuilder();
            jobInfoBuilder.setJobId(Long.parseLong(jobId));
            jobInfoBuilder.setJobSpecification(jobSpecificationBuider.build());
            // Store JobInfo in file system based job-store
            String res = jobInfoBuilder.build();
            createFile(jobId, JOBINFO_FILE_NAME, res);
        }

    }
}
