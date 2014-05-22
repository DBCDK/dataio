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
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static final long ONE_MINUTE_IN_MILLIS = 60000; //one minute in milliseconds.

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
    public void testJobsShowEmptyList_NoContentIsShown() {
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
        final long JOB_CREATION_TIME_OLDEST = new Date().getTime();
        final long JOB_CREATION_TIME_NEWEST = new Date(JOB_CREATION_TIME_OLDEST + ONE_MINUTE_IN_MILLIS).getTime();

        jobstoreFolder.createTestJob(JOB_CREATION_TIME_OLDEST, JOB_ID_1, FILE_NAME_1, SUBMITTER_NUMBER_1);
        jobstoreFolder.createTestJob(JOB_CREATION_TIME_NEWEST, JOB_ID_2, FILE_NAME_2, SUBMITTER_NUMBER_2);
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(2);
        List<List<String>> rowData = table.get();

        assertTrue(rowData.get(0).get(0).equals(simpleDateFormat.format(new Date(JOB_CREATION_TIME_NEWEST))));
        assertThat(rowData.get(0).get(1), is(JOB_ID_2));   // Default sorting: Youngest first
        assertThat(rowData.get(0).get(2), is(FILE_NAME_2));
        assertThat(rowData.get(0).get(3), is(SUBMITTER_NUMBER_2));
        assertTrue(rowData.get(1).get(0).equals(simpleDateFormat.format(new Date(JOB_CREATION_TIME_OLDEST))));
        assertThat(rowData.get(1).get(1), is(JOB_ID_1));
        assertThat(rowData.get(1).get(2), is(FILE_NAME_1));
        assertThat(rowData.get(1).get(3), is(SUBMITTER_NUMBER_1));
    }

    @Test
    public void testJobs_MoreButtonVisible() throws IOException {
        navigateToJobsShowWidget(webDriver);
        assertTrue(findMoreButton(webDriver).isDisplayed());
    }

    @Test
    public void testJobsInsertTwoPageData_OnePageElementsShown() throws IOException {
        // Insert 21 rows of job data
        final long JOB_CREATION_TIME1 = new Date().getTime(); //Current time
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "101", "Fil01", "219");
        final long JOB_CREATION_TIME3 = new Date(JOB_CREATION_TIME2 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "102", "Fil02", "218");
        final long JOB_CREATION_TIME4 = new Date(JOB_CREATION_TIME3 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "103", "Fil03", "217");
        final long JOB_CREATION_TIME5 = new Date(JOB_CREATION_TIME4 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME5, "104", "Fil04", "216");
        final long JOB_CREATION_TIME6 = new Date(JOB_CREATION_TIME5 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME6, "105", "Fil05", "215");
        final long JOB_CREATION_TIME7 = new Date(JOB_CREATION_TIME6 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME7, "106", "Fil06", "214");
        final long JOB_CREATION_TIME8 = new Date(JOB_CREATION_TIME7 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME8, "107", "Fil07", "213");
        final long JOB_CREATION_TIME9 = new Date(JOB_CREATION_TIME8 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME9, "108", "Fil08", "212");
        final long JOB_CREATION_TIME10 = new Date(JOB_CREATION_TIME9 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME10, "109", "Fil09", "211");
        final long JOB_CREATION_TIME11 = new Date(JOB_CREATION_TIME10 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME11, "110", "Fil10", "210");
        final long JOB_CREATION_TIME12 = new Date(JOB_CREATION_TIME11 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME12, "111", "Fil11", "209");
        final long JOB_CREATION_TIME13 = new Date(JOB_CREATION_TIME12 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME13, "112", "Fil12", "208");
        final long JOB_CREATION_TIME14 = new Date(JOB_CREATION_TIME13 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME14, "113", "Fil13", "207");
        final long JOB_CREATION_TIME15 = new Date(JOB_CREATION_TIME14 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME15, "114", "Fil14", "206");
        final long JOB_CREATION_TIME16 = new Date(JOB_CREATION_TIME15 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME16, "115", "Fil15", "205");
        final long JOB_CREATION_TIME17 = new Date(JOB_CREATION_TIME16 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME17, "116", "Fil16", "204");
        final long JOB_CREATION_TIME18 = new Date(JOB_CREATION_TIME17 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME18, "117", "Fil17", "203");
        final long JOB_CREATION_TIME19 = new Date(JOB_CREATION_TIME18 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME19, "118", "Fil18", "202");
        final long JOB_CREATION_TIME20 = new Date(JOB_CREATION_TIME19 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME20, "119", "Fil19", "201");
        final long JOB_CREATION_TIME21 = new Date(JOB_CREATION_TIME20 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME21, "120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> rowData = table.get();

        // Assert 20 rows
        int i = 0;
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME21)),"120", "Fil20", "200");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)),"119", "Fil19", "201");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)),"118", "Fil18", "202");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)),"117", "Fil17", "203");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)),"116", "Fil16", "204");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)),"115", "Fil15", "205");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)),"114", "Fil14", "206");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)),"113", "Fil13", "207");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)),"112", "Fil12", "208");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)),"111", "Fil11", "209");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)),"110", "Fil10", "210");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)),"109", "Fil09", "211");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)),"108", "Fil08", "212");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)),"107", "Fil07", "213");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)),"106", "Fil06", "214");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)),"105", "Fil05", "215");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)),"104", "Fil04", "216");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)),"103", "Fil03", "217");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)),"102", "Fil02", "218");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)),"101", "Fil01", "219");
    }

    @Test
    public void testJobsInsert2PagesDataAndClickMore_CorrectNumberOfElementsShown() throws IOException {
        final long JOB_CREATION_TIME1 = new Date().getTime(); //Current time
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "101", "Fil01", "219");
        final long JOB_CREATION_TIME3 = new Date(JOB_CREATION_TIME2 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "102", "Fil02", "218");
        final long JOB_CREATION_TIME4 = new Date(JOB_CREATION_TIME3 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "103", "Fil03", "217");
        final long JOB_CREATION_TIME5 = new Date(JOB_CREATION_TIME4 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME5, "104", "Fil04", "216");
        final long JOB_CREATION_TIME6 = new Date(JOB_CREATION_TIME5 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME6, "105", "Fil05", "215");
        final long JOB_CREATION_TIME7 = new Date(JOB_CREATION_TIME6 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME7, "106", "Fil06", "214");
        final long JOB_CREATION_TIME8 = new Date(JOB_CREATION_TIME7 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME8, "107", "Fil07", "213");
        final long JOB_CREATION_TIME9 = new Date(JOB_CREATION_TIME8 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME9, "108", "Fil08", "212");
        final long JOB_CREATION_TIME10 = new Date(JOB_CREATION_TIME9 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME10, "109", "Fil09", "211");
        final long JOB_CREATION_TIME11 = new Date(JOB_CREATION_TIME10 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME11, "110", "Fil10", "210");
        final long JOB_CREATION_TIME12 = new Date(JOB_CREATION_TIME11 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME12, "111", "Fil11", "209");
        final long JOB_CREATION_TIME13 = new Date(JOB_CREATION_TIME12 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME13, "112", "Fil12", "208");
        final long JOB_CREATION_TIME14 = new Date(JOB_CREATION_TIME13 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME14, "113", "Fil13", "207");
        final long JOB_CREATION_TIME15 = new Date(JOB_CREATION_TIME14 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME15, "114", "Fil14", "206");
        final long JOB_CREATION_TIME16 = new Date(JOB_CREATION_TIME15 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME16, "115", "Fil15", "205");
        final long JOB_CREATION_TIME17 = new Date(JOB_CREATION_TIME16 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME17, "116", "Fil16", "204");
        final long JOB_CREATION_TIME18 = new Date(JOB_CREATION_TIME17 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME18, "117", "Fil17", "203");
        final long JOB_CREATION_TIME19 = new Date(JOB_CREATION_TIME18 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME19, "118", "Fil18", "202");
        final long JOB_CREATION_TIME20 = new Date(JOB_CREATION_TIME19 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME20, "119", "Fil19", "201");
        final long JOB_CREATION_TIME21 = new Date(JOB_CREATION_TIME20 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME21, "120", "Fil20", "200");

        // Navigate to Jobs Show List and click More
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        findMoreButton(webDriver).click();
        table.waitAssertRows(1);
        List<List<String>> rowData = table.get();

        // Assert 21 rows
        int i = 0;
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME21)), "120", "Fil20", "200");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)),  "118", "Fil18", "202");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");
        assertTableRow(rowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "100", "Fil00", "220");
    }

    @Test
    public void testColumnHeaderNames() throws IOException {
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        assertThat(table.findColumnHeader(0).getText(), is(texts.translate("columnHeader_JobCreationTime")));
        assertThat(table.findColumnHeader(1).getText(), is(texts.translate("columnHeader_JobId")));
        assertThat(table.findColumnHeader(2).getText(), is(texts.translate("columnHeader_FileName")));
        assertThat(table.findColumnHeader(3).getText(), is(texts.translate("columnHeader_SubmitterNumber")));
    }


    @Test
    public void testClickJobCreationTimeColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        final long JOB_CREATION_TIME1 = new Date().getTime(); //Current time
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "101", "Fil01", "219");
        final long JOB_CREATION_TIME3 = new Date(JOB_CREATION_TIME2 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "102", "Fil02", "218");
        final long JOB_CREATION_TIME4 = new Date(JOB_CREATION_TIME3 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "103", "Fil03", "217");
        final long JOB_CREATION_TIME5 = new Date(JOB_CREATION_TIME4 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME5, "104", "Fil04", "216");
        final long JOB_CREATION_TIME6 = new Date(JOB_CREATION_TIME5 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME6, "105", "Fil05", "215");
        final long JOB_CREATION_TIME7 = new Date(JOB_CREATION_TIME6 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME7, "106", "Fil06", "214");
        final long JOB_CREATION_TIME8 = new Date(JOB_CREATION_TIME7 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME8, "107", "Fil07", "213");
        final long JOB_CREATION_TIME9 = new Date(JOB_CREATION_TIME8 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME9, "108", "Fil08", "212");
        final long JOB_CREATION_TIME10 = new Date(JOB_CREATION_TIME9 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME10, "109", "Fil09", "211");
        final long JOB_CREATION_TIME11 = new Date(JOB_CREATION_TIME10 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME11, "110", "Fil10", "210");
        final long JOB_CREATION_TIME12 = new Date(JOB_CREATION_TIME11 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME12, "111", "Fil11", "209");
        final long JOB_CREATION_TIME13 = new Date(JOB_CREATION_TIME12 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME13, "112", "Fil12", "208");
        final long JOB_CREATION_TIME14 = new Date(JOB_CREATION_TIME13 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME14, "113", "Fil13", "207");
        final long JOB_CREATION_TIME15 = new Date(JOB_CREATION_TIME14 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME15, "114", "Fil14", "206");
        final long JOB_CREATION_TIME16 = new Date(JOB_CREATION_TIME15 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME16, "115", "Fil15", "205");
        final long JOB_CREATION_TIME17 = new Date(JOB_CREATION_TIME16 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME17, "116", "Fil16", "204");
        final long JOB_CREATION_TIME18 = new Date(JOB_CREATION_TIME17 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME18, "117", "Fil17", "203");
        final long JOB_CREATION_TIME19 = new Date(JOB_CREATION_TIME18 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME19, "118", "Fil18", "202");
        final long JOB_CREATION_TIME20 = new Date(JOB_CREATION_TIME19 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME20, "119", "Fil19", "201");
        final long JOB_CREATION_TIME21 = new Date(JOB_CREATION_TIME20 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME21, "120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // No need to click on Job Creation Time Header Column - it is selected by default, and column is descending
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();

        int i = 0;
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME21)), "120", "Fil20", "200");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");

        // Second click on Job Creation Time Column makes column ascending
        table.findColumnHeader(0).click();  // First column is JobCreationTime column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();

        i = 0;
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "100", "Fil00", "220");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");
    }

    @Test
    public void testClickJobIdColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        final long JOB_CREATION_TIME1 = new Date().getTime(); //Current time
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "101", "Fil01", "219");
        final long JOB_CREATION_TIME3 = new Date(JOB_CREATION_TIME2 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "102", "Fil02", "218");
        final long JOB_CREATION_TIME4 = new Date(JOB_CREATION_TIME3 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "103", "Fil03", "217");
        final long JOB_CREATION_TIME5 = new Date(JOB_CREATION_TIME4 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME5, "104", "Fil04", "216");
        final long JOB_CREATION_TIME6 = new Date(JOB_CREATION_TIME5 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME6, "105", "Fil05", "215");
        final long JOB_CREATION_TIME7 = new Date(JOB_CREATION_TIME6 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME7, "106", "Fil06", "214");
        final long JOB_CREATION_TIME8 = new Date(JOB_CREATION_TIME7 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME8, "107", "Fil07", "213");
        final long JOB_CREATION_TIME9 = new Date(JOB_CREATION_TIME8 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME9, "108", "Fil08", "212");
        final long JOB_CREATION_TIME10 = new Date(JOB_CREATION_TIME9 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME10, "109", "Fil09", "211");
        final long JOB_CREATION_TIME11 = new Date(JOB_CREATION_TIME10 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME11, "110", "Fil10", "210");
        final long JOB_CREATION_TIME12 = new Date(JOB_CREATION_TIME11 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME12, "111", "Fil11", "209");
        final long JOB_CREATION_TIME13 = new Date(JOB_CREATION_TIME12 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME13, "112", "Fil12", "208");
        final long JOB_CREATION_TIME14 = new Date(JOB_CREATION_TIME13 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME14, "113", "Fil13", "207");
        final long JOB_CREATION_TIME15 = new Date(JOB_CREATION_TIME14 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME15, "114", "Fil14", "206");
        final long JOB_CREATION_TIME16 = new Date(JOB_CREATION_TIME15 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME16, "115", "Fil15", "205");
        final long JOB_CREATION_TIME17 = new Date(JOB_CREATION_TIME16 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME17, "116", "Fil16", "204");
        final long JOB_CREATION_TIME18 = new Date(JOB_CREATION_TIME17 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME18, "117", "Fil17", "203");
        final long JOB_CREATION_TIME19 = new Date(JOB_CREATION_TIME18 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME19, "118", "Fil18", "202");
        final long JOB_CREATION_TIME20 = new Date(JOB_CREATION_TIME19 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME20, "119", "Fil19", "201");
        final long JOB_CREATION_TIME21 = new Date(JOB_CREATION_TIME20 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME21, "120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        table.findColumnHeader(1).click();  // Second column is Filename column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending job id's
        int i = 0;
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "100", "Fil00", "220");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");

        // Second click on Job Id Header Column makes makes column descending
        table.findColumnHeader(0).click();  // First column is Job Id column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();

        i = 0;
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME21)), "120", "Fil20", "200");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");
    }

    @Test
    public void testClickFileNameColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        final long JOB_CREATION_TIME1 = new Date().getTime(); //Current time
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "101", "Fil01", "219");
        final long JOB_CREATION_TIME3 = new Date(JOB_CREATION_TIME2 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "102", "Fil02", "218");
        final long JOB_CREATION_TIME4 = new Date(JOB_CREATION_TIME3 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "103", "Fil03", "217");
        final long JOB_CREATION_TIME5 = new Date(JOB_CREATION_TIME4 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME5, "104", "Fil04", "216");
        final long JOB_CREATION_TIME6 = new Date(JOB_CREATION_TIME5 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME6, "105", "Fil05", "215");
        final long JOB_CREATION_TIME7 = new Date(JOB_CREATION_TIME6 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME7, "106", "Fil06", "214");
        final long JOB_CREATION_TIME8 = new Date(JOB_CREATION_TIME7 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME8, "107", "Fil07", "213");
        final long JOB_CREATION_TIME9 = new Date(JOB_CREATION_TIME8 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME9, "108", "Fil08", "212");
        final long JOB_CREATION_TIME10 = new Date(JOB_CREATION_TIME9 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME10, "109", "Fil09", "211");
        final long JOB_CREATION_TIME11 = new Date(JOB_CREATION_TIME10 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME11, "110", "Fil10", "210");
        final long JOB_CREATION_TIME12 = new Date(JOB_CREATION_TIME11 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME12, "111", "Fil11", "209");
        final long JOB_CREATION_TIME13 = new Date(JOB_CREATION_TIME12 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME13, "112", "Fil12", "208");
        final long JOB_CREATION_TIME14 = new Date(JOB_CREATION_TIME13 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME14, "113", "Fil13", "207");
        final long JOB_CREATION_TIME15 = new Date(JOB_CREATION_TIME14 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME15, "114", "Fil14", "206");
        final long JOB_CREATION_TIME16 = new Date(JOB_CREATION_TIME15 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME16, "115", "Fil15", "205");
        final long JOB_CREATION_TIME17 = new Date(JOB_CREATION_TIME16 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME17, "116", "Fil16", "204");
        final long JOB_CREATION_TIME18 = new Date(JOB_CREATION_TIME17 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME18, "117", "Fil17", "203");
        final long JOB_CREATION_TIME19 = new Date(JOB_CREATION_TIME18 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME19, "118", "Fil18", "202");
        final long JOB_CREATION_TIME20 = new Date(JOB_CREATION_TIME19 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME20, "119", "Fil19", "201");
        final long JOB_CREATION_TIME21 = new Date(JOB_CREATION_TIME20 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME21, "120", "Fil20", "200");


        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Filename Header Column makes Filename column ascending ( => Job Id column is also ascending)
        table.findColumnHeader(2).click();  // third column is Filename column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending file names
        int i = 0;
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "100", "Fil00", "220");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");

        // Second click on Filename Header Column makes Filename column descending ( => Job Id column is also descending)
        table.findColumnHeader(2).click();  // Third column is Filename column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();

        // Assert correct order of 20 rows - descending job Id's
        i = 0;
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME21)), "120", "Fil20", "200");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");
    }

    @Test
    public void testClickSubmitterNumberColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        final long JOB_CREATION_TIME1 = new Date().getTime(); //Current time
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "101", "Fil01", "219");
        final long JOB_CREATION_TIME3 = new Date(JOB_CREATION_TIME2 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "102", "Fil02", "218");
        final long JOB_CREATION_TIME4 = new Date(JOB_CREATION_TIME3 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "103", "Fil03", "217");
        final long JOB_CREATION_TIME5 = new Date(JOB_CREATION_TIME4 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME5, "104", "Fil04", "216");
        final long JOB_CREATION_TIME6 = new Date(JOB_CREATION_TIME5 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME6, "105", "Fil05", "215");
        final long JOB_CREATION_TIME7 = new Date(JOB_CREATION_TIME6 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME7, "106", "Fil06", "214");
        final long JOB_CREATION_TIME8 = new Date(JOB_CREATION_TIME7 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME8, "107", "Fil07", "213");
        final long JOB_CREATION_TIME9 = new Date(JOB_CREATION_TIME8 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME9, "108", "Fil08", "212");
        final long JOB_CREATION_TIME10 = new Date(JOB_CREATION_TIME9 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME10, "109", "Fil09", "211");
        final long JOB_CREATION_TIME11 = new Date(JOB_CREATION_TIME10 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME11, "110", "Fil10", "210");
        final long JOB_CREATION_TIME12 = new Date(JOB_CREATION_TIME11 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME12, "111", "Fil11", "209");
        final long JOB_CREATION_TIME13 = new Date(JOB_CREATION_TIME12 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME13, "112", "Fil12", "208");
        final long JOB_CREATION_TIME14 = new Date(JOB_CREATION_TIME13 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME14, "113", "Fil13", "207");
        final long JOB_CREATION_TIME15 = new Date(JOB_CREATION_TIME14 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME15, "114", "Fil14", "206");
        final long JOB_CREATION_TIME16 = new Date(JOB_CREATION_TIME15 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME16, "115", "Fil15", "205");
        final long JOB_CREATION_TIME17 = new Date(JOB_CREATION_TIME16 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME17, "116", "Fil16", "204");
        final long JOB_CREATION_TIME18 = new Date(JOB_CREATION_TIME17 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME18, "117", "Fil17", "203");
        final long JOB_CREATION_TIME19 = new Date(JOB_CREATION_TIME18 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME19, "118", "Fil18", "202");
        final long JOB_CREATION_TIME20 = new Date(JOB_CREATION_TIME19 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME20, "119", "Fil19", "201");
        final long JOB_CREATION_TIME21 = new Date(JOB_CREATION_TIME20 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME21, "120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Submitternumber Header Column makes Submitternumber column ascending
        table.findColumnHeader(3).click();  // fourth column is Submitternumber column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending submitter numbers
        int i = 0;
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME21)), "120", "Fil20", "200");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");

        // Second click on Submitternumber Header Column makes Submitternumber column descending
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<String>> secondRowData = secondSortTable.get();

        // Assert correct order of 20 rows - descending submitter numbers
        i = 0;
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "100", "Fil00", "220");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "101", "Fil01", "219");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME5)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME6)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME7)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME8)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME9)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME10)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME11)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME12)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME13)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME14)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME15)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME16)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME17)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME18)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME19)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME20)), "119", "Fil19", "201");
    }

    @Test
    public void testEqualFilenames_ClickOnceOnFileNameColumn_SortAccordingly() throws IOException {
        final long JOB_CREATION_TIME1 = new Date().getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "1", "b", "2");
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "2", "a", "1");
        final long JOB_CREATION_TIME3 = JOB_CREATION_TIME1;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "3", "a", "2");
        final long JOB_CREATION_TIME4 = JOB_CREATION_TIME2;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(2).click();  // Third column is Filename column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "3", "a", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "2", "a", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "4", "b", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "1", "b", "2");
    }

    @Test
    public void testEqualFilenames_ClickTwiceOnFileNameColumn_SortAccordingly() throws IOException {
        final long JOB_CREATION_TIME1 = new Date().getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "1", "b", "2");
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "2", "a", "1");
        final long JOB_CREATION_TIME3 = JOB_CREATION_TIME1;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "3", "a", "2");
        final long JOB_CREATION_TIME4 = JOB_CREATION_TIME2;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(2).click();  // Third column is Filename column => Ascending
        table.findColumnHeader(2).click();  // Third column is Filename column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "4", "b", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "1", "b", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "3", "a", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "2", "a", "1");
    }


    @Test
    public void testEqualJobCreationTime_ClickOnceOnJobCreationTimeColumn_SortAccordingly() throws IOException {
        final long JOB_CREATION_TIME1 = new Date().getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "1", "b", "2");
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "2", "a", "1");
        final long JOB_CREATION_TIME3 = JOB_CREATION_TIME1;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "3", "a", "2");
        final long JOB_CREATION_TIME4 = JOB_CREATION_TIME2;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "4", "b", "1");
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the JobCreationTime Column
        table.findColumnHeader(0).click();  // First column is JobCreationTime column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "3", "a", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "1", "b", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "4", "b", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "2", "a", "1");
    }

    @Test
    public void testEqualJobCreationTime_ClickTwiceOnJobCreationTimeColumn_SortAccordingly() throws IOException {
        final long JOB_CREATION_TIME1 = new Date().getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "1", "b", "2");
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "2", "a", "1");
        final long JOB_CREATION_TIME3 = JOB_CREATION_TIME1;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "3", "a", "2");
        final long JOB_CREATION_TIME4 = JOB_CREATION_TIME2;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "4", "b", "1");
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the JobCreationTime Column
        table.findColumnHeader(0).click();  // first column is JobCreationTime column => Descending
        table.findColumnHeader(0).click();  // first column is JobCreationTime column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "4", "b", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "2", "a", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "3", "a", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "1", "b", "2");
    }

    @Test
    public void testEqualSubmitterNumbers_ClickOnceOnSubmitterNumberColumn_SortAccordingly() throws IOException {
        final long JOB_CREATION_TIME1 = new Date().getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "1", "b", "2");
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "2", "a", "1");
        final long JOB_CREATION_TIME3 = JOB_CREATION_TIME1;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "3", "a", "2");
        final long JOB_CREATION_TIME4 = JOB_CREATION_TIME2;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "4", "b", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "2", "a", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "3", "a", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "1", "b", "2");
    }

    @Test
    public void testEqualSubmitterNumbers_ClickTwiceOnSubmitterNumberColumn_SortAccordingly() throws IOException {
        final long JOB_CREATION_TIME1 = new Date().getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME1, "1", "b", "2");
        final long JOB_CREATION_TIME2 = new Date(JOB_CREATION_TIME1 + ONE_MINUTE_IN_MILLIS).getTime();
        jobstoreFolder.createTestJob(JOB_CREATION_TIME2, "2", "a", "1");
        final long JOB_CREATION_TIME3 = JOB_CREATION_TIME1;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME3, "3", "a", "2");
        final long JOB_CREATION_TIME4 = JOB_CREATION_TIME2;
        jobstoreFolder.createTestJob(JOB_CREATION_TIME4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column => Ascending
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<String>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME3)), "3", "a", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME1)), "1", "b", "2");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME4)), "4", "b", "1");
        assertTableRow(tableData.get(i++), simpleDateFormat.format(new Date(JOB_CREATION_TIME2)), "2", "a", "1");
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

    private void assertTableRow(List<String> row, String jobCreationTime, String jobId, String fileName, String submitterId) {
        assertTrue(row.get(0).equals(jobCreationTime));
        assertThat(row.get(1), is(jobId));
        assertThat(row.get(2), is(fileName));
        assertThat(row.get(3), is(submitterId));
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

        private void createTestJob(long jobCreationTime, String jobId, final String fileName, String submitterNumber) throws IOException {
            // Build JobSpecification JSON
            JobSpecificationJsonBuilder jobSpecificationBuilder = new JobSpecificationJsonBuilder();
            jobSpecificationBuilder.setDataFile(fileName);
            jobSpecificationBuilder.setSubmitterId(Long.parseLong(submitterNumber));
            // Build  JobInfo JSON
            JobInfoJsonBuilder jobInfoBuilder = new JobInfoJsonBuilder();
            jobInfoBuilder.setJobId(Long.parseLong(jobId));
            jobInfoBuilder.setJobSpecification(jobSpecificationBuilder.build());
            jobInfoBuilder.setJobCreationTime(jobCreationTime);
            // Store JobInfo in file system based job-store
            String res = jobInfoBuilder.build();
            createFile(jobId, JOBINFO_FILE_NAME, res);
        }
    }
}
