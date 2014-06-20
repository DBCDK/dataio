package dk.dbc.dataio.gui.client;

import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.utils.test.json.ChunkCounterJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.ItemResultCounterJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobInfoJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.JobSpecificationJsonBuilder;
import dk.dbc.dataio.gui.client.pages.jobsshow.JobsShowViewImpl;
import dk.dbc.dataio.gui.util.ClientFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    private final static String JOBINFO_FILE_NAME = "jobinfo.json";
    private TemporaryDataioJobstoreFolder jobstoreFolder;

    private final static Integer JOBS_SHOW_PAGE_SIZE = 20;


    @Before
    public void createTempFolders() throws IOException {
        final Path dataioPath = Paths.get(System.getProperty("job.store.basepath"));
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
        final String JOB_ID_3 = "11674";
        final String FILE_NAME_3 = "File_name_three";
        final String SUBMITTER_NUMBER_3 = "333";
        final String JOB_ID_4 = "11699";
        final String FILE_NAME_4 = "File_name_four";
        final String SUBMITTER_NUMBER_4 = "444";
        final long JOB_CREATION_TIME_ONE = new Date().getTime();
        final long JOB_CREATION_TIME_TWO = getModifiedDate(JOB_CREATION_TIME_ONE, 1);
        final long JOB_CREATION_TIME_THREE = getModifiedDate(JOB_CREATION_TIME_TWO, 1);
        final long JOB_CREATION_TIME_FOUR = getModifiedDate(JOB_CREATION_TIME_THREE, 1);

        jobstoreFolder.createTestJob(JOB_CREATION_TIME_ONE, JOB_ID_1, FILE_NAME_1, SUBMITTER_NUMBER_1, JobErrorCode.NO_ERROR, true, 0L, 0L, 0L);
        jobstoreFolder.createTestJob(JOB_CREATION_TIME_TWO, JOB_ID_2, FILE_NAME_2, SUBMITTER_NUMBER_2, JobErrorCode.NO_ERROR, true, 1L, 0L, 0L);
        jobstoreFolder.createTestJob(JOB_CREATION_TIME_THREE, JOB_ID_3, FILE_NAME_3, SUBMITTER_NUMBER_3, JobErrorCode.DATA_FILE_INVALID, true, 0L, 0L, 0L);
        jobstoreFolder.createTestJob(JOB_CREATION_TIME_FOUR, JOB_ID_4, FILE_NAME_4, SUBMITTER_NUMBER_4, JobErrorCode.NO_ERROR, false, 0L, 0L, 0L);
        navigateToJobsShowWidget(webDriver);

        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);
        List<List<SeleniumGWTTable.Cell>> rowData = table.get();

        assertTrue(rowData.get(0).get(0).getCellContent().equals(formatDate(JOB_CREATION_TIME_FOUR)));
        assertThat(rowData.get(0).get(1).getCellContent(), is(JOB_ID_4));   // Default sorting: Youngest first
        assertThat(rowData.get(0).get(2).getCellContent(), is(FILE_NAME_4));
        assertThat(rowData.get(0).get(3).getCellContent(), is(SUBMITTER_NUMBER_4));
        assertThat(rowData.get(0).get(4).hasClassName(JobsShowViewImpl.GUICLASS_GRAY), is(true));

        assertTrue(rowData.get(1).get(0).getCellContent().equals(formatDate(JOB_CREATION_TIME_THREE)));
        assertThat(rowData.get(1).get(1).getCellContent(), is(JOB_ID_3));
        assertThat(rowData.get(1).get(2).getCellContent(), is(FILE_NAME_3));
        assertThat(rowData.get(1).get(3).getCellContent(), is(SUBMITTER_NUMBER_3));
        assertThat(rowData.get(1).get(4).hasClassName(JobsShowViewImpl.GUICLASS_RED), is(true));

        assertTrue(rowData.get(2).get(0).getCellContent().equals(formatDate(JOB_CREATION_TIME_TWO)));
        assertThat(rowData.get(2).get(1).getCellContent(), is(JOB_ID_2));
        assertThat(rowData.get(2).get(2).getCellContent(), is(FILE_NAME_2));
        assertThat(rowData.get(2).get(3).getCellContent(), is(SUBMITTER_NUMBER_2));
        assertThat(rowData.get(2).get(4).hasClassName(JobsShowViewImpl.GUICLASS_RED), is(true));

        assertTrue(rowData.get(3).get(0).getCellContent().equals(formatDate(JOB_CREATION_TIME_ONE)));
        assertThat(rowData.get(3).get(1).getCellContent(), is(JOB_ID_1));
        assertThat(rowData.get(3).get(2).getCellContent(), is(FILE_NAME_1));
        assertThat(rowData.get(3).get(3).getCellContent(), is(SUBMITTER_NUMBER_1));
        assertThat(rowData.get(3).get(4).hasClassName(JobsShowViewImpl.GUICLASS_GREEN), is(true));
    }

    @Test
    public void testJobs_MoreButtonVisible() throws IOException {
        navigateToJobsShowWidget(webDriver);
        assertTrue(findMoreButton(webDriver).isDisplayed());
    }

    @Test
    public void testJobsInsertTwoPageData_OnePageElementsShown() throws IOException {
        // Insert 21 rows of job data
        final long jobCreationTime = new Date().getTime(); // Current time
        jobstoreFolder.createTestJob(jobCreationTime, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 1), "101", "Fil01", "219");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 2), "102", "Fil02", "218");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 3), "103", "Fil03", "217");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 4), "104", "Fil04", "216");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 5), "105", "Fil05", "215");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 6), "106", "Fil06", "214");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 7), "107", "Fil07", "213");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 8), "108", "Fil08", "212");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 9), "109", "Fil09", "211");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 10), "110", "Fil10", "210");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 11), "111", "Fil11", "209");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 12), "112", "Fil12", "208");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 13), "113", "Fil13", "207");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 14), "114", "Fil14", "206");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 15), "115", "Fil15", "205");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 16), "116", "Fil16", "204");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 17), "117", "Fil17", "203");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 18), "118", "Fil18", "202");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 19), "119", "Fil19", "201");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 20), "120", "Fil20", "200");


        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> rowData = table.get();

        // Assert 20 rows
        int i = 0;
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 20)), "120", "Fil20", "200");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
    }

    @Test
    public void testJobsInsert2PagesDataAndClickMore_CorrectNumberOfElementsShown() throws IOException {
        // Insert 21 rows of job data
        final long jobCreationTime = new Date().getTime(); // Current time
        jobstoreFolder.createTestJob(jobCreationTime, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 1), "101", "Fil01", "219");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 2), "102", "Fil02", "218");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 3), "103", "Fil03", "217");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 4), "104", "Fil04", "216");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 5), "105", "Fil05", "215");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 6), "106", "Fil06", "214");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 7), "107", "Fil07", "213");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 8), "108", "Fil08", "212");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 9), "109", "Fil09", "211");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 10), "110", "Fil10", "210");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 11), "111", "Fil11", "209");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 12), "112", "Fil12", "208");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 13), "113", "Fil13", "207");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 14), "114", "Fil14", "206");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 15), "115", "Fil15", "205");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 16), "116", "Fil16", "204");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 17), "117", "Fil17", "203");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 18), "118", "Fil18", "202");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 19), "119", "Fil19", "201");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 20), "120", "Fil20", "200");

        // Navigate to Jobs Show List and click More
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        findMoreButton(webDriver).click();
        table.waitAssertRows(1);
        List<List<SeleniumGWTTable.Cell>> rowData = table.get();

        // Assert 21 rows
        int i = 0;
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 20)), "120", "Fil20", "200");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
        assertTableRow(rowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 0)), "100", "Fil00", "220");
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
        final long jobCreationTime = new Date().getTime(); // Current time
        jobstoreFolder.createTestJob(jobCreationTime, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 1), "101", "Fil01", "219");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 2), "102", "Fil02", "218");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 3), "103", "Fil03", "217");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 4), "104", "Fil04", "216");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 5), "105", "Fil05", "215");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 6), "106", "Fil06", "214");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 7), "107", "Fil07", "213");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 8), "108", "Fil08", "212");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 9), "109", "Fil09", "211");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 10), "110", "Fil10", "210");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 11), "111", "Fil11", "209");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 12), "112", "Fil12", "208");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 13), "113", "Fil13", "207");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 14), "114", "Fil14", "206");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 15), "115", "Fil15", "205");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 16), "116", "Fil16", "204");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 17), "117", "Fil17", "203");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 18), "118", "Fil18", "202");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 19), "119", "Fil19", "201");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 20), "120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // No need to click on Job Creation Time Header Column - it is selected by default, and column is descending
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> firstRowData = firstSortTable.get();

        int i = 0;
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 20)), "120", "Fil20", "200");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");

        // Second click on Job Creation Time Column makes column ascending
        table.findColumnHeader(0).click();  // First column is JobCreationTime column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> secondRowData = secondSortTable.get();

        i = 0;
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 0)), "100", "Fil00", "220");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
    }

    @Test
    public void testClickJobIdColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        final long jobCreationTime = new Date().getTime(); // Current time
        jobstoreFolder.createTestJob(jobCreationTime, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 1), "101", "Fil01", "219");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 2), "102", "Fil02", "218");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 3), "103", "Fil03", "217");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 4), "104", "Fil04", "216");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 5), "105", "Fil05", "215");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 6), "106", "Fil06", "214");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 7), "107", "Fil07", "213");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 8), "108", "Fil08", "212");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 9), "109", "Fil09", "211");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 10), "110", "Fil10", "210");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 11), "111", "Fil11", "209");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 12), "112", "Fil12", "208");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 13), "113", "Fil13", "207");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 14), "114", "Fil14", "206");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 15), "115", "Fil15", "205");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 16), "116", "Fil16", "204");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 17), "117", "Fil17", "203");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 18), "118", "Fil18", "202");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 19), "119", "Fil19", "201");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 20), "120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        table.findColumnHeader(1).click();  // Second column is Filename column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending job id's
        int i = 0;
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 0)), "100", "Fil00", "220");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");

        // Second click on Job Id Header Column makes makes column descending
        table.findColumnHeader(0).click();  // First column is Job Id column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> secondRowData = secondSortTable.get();

        i = 0;
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 20)), "120", "Fil20", "200");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
    }

    @Test
    public void testClickFileNameColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        final long jobCreationTime = new Date().getTime(); // Current time
        jobstoreFolder.createTestJob(jobCreationTime, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 1), "101", "Fil01", "219");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 2), "102", "Fil02", "218");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 3), "103", "Fil03", "217");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 4), "104", "Fil04", "216");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 5), "105", "Fil05", "215");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 6), "106", "Fil06", "214");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 7), "107", "Fil07", "213");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 8), "108", "Fil08", "212");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 9), "109", "Fil09", "211");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 10), "110", "Fil10", "210");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 11), "111", "Fil11", "209");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 12), "112", "Fil12", "208");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 13), "113", "Fil13", "207");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 14), "114", "Fil14", "206");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 15), "115", "Fil15", "205");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 16), "116", "Fil16", "204");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 17), "117", "Fil17", "203");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 18), "118", "Fil18", "202");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 19), "119", "Fil19", "201");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 20), "120", "Fil20", "200");


        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Filename Header Column makes Filename column ascending ( => Job Id column is also ascending)
        table.findColumnHeader(2).click();  // third column is Filename column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending file names
        int i = 0;
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 0)), "100", "Fil00", "220");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");

        // Second click on Filename Header Column makes Filename column descending ( => Job Id column is also descending)
        table.findColumnHeader(2).click();  // Third column is Filename column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> secondRowData = secondSortTable.get();

        // Assert correct order of 20 rows - descending job Id's
        i = 0;
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 20)), "120", "Fil20", "200");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
    }

    @Test
    public void testClickSubmitterNumberColumnHeader_OrderAccordingly() throws IOException {
        // Insert 21 rows of job data
        final long jobCreationTime = new Date().getTime(); // Current time
        jobstoreFolder.createTestJob(jobCreationTime, "100", "Fil00", "220");
        //Adding one minute to the remaining timestamps to make sure the timestamps do not look identical.
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 1), "101", "Fil01", "219");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 2), "102", "Fil02", "218");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 3), "103", "Fil03", "217");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 4), "104", "Fil04", "216");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 5), "105", "Fil05", "215");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 6), "106", "Fil06", "214");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 7), "107", "Fil07", "213");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 8), "108", "Fil08", "212");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 9), "109", "Fil09", "211");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 10), "110", "Fil10", "210");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 11), "111", "Fil11", "209");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 12), "112", "Fil12", "208");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 13), "113", "Fil13", "207");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 14), "114", "Fil14", "206");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 15), "115", "Fil15", "205");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 16), "116", "Fil16", "204");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 17), "117", "Fil17", "203");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 18), "118", "Fil18", "202");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 19), "119", "Fil19", "201");
        jobstoreFolder.createTestJob(getModifiedDate(jobCreationTime, 20), "120", "Fil20", "200");

        // Navigate to Jobs Show List
        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(JOBS_SHOW_PAGE_SIZE);

        // First click on Submitternumber Header Column makes Submitternumber column ascending
        table.findColumnHeader(3).click();  // fourth column is Submitternumber column
        SeleniumGWTTable firstSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        firstSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> firstRowData = firstSortTable.get();

        // Assert correct order of 20 rows - ascending submitter numbers
        int i = 0;
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 20)), "120", "Fil20", "200");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(firstRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");

        // Second click on Submitternumber Header Column makes Submitternumber column descending
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column
        SeleniumGWTTable secondSortTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        secondSortTable.waitAssertRows(JOBS_SHOW_PAGE_SIZE);
        List<List<SeleniumGWTTable.Cell>> secondRowData = secondSortTable.get();

        // Assert correct order of 20 rows - descending submitter numbers
        i = 0;
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 0)), "100", "Fil00", "220");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 1)), "101", "Fil01", "219");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 2)), "102", "Fil02", "218");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 3)), "103", "Fil03", "217");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 4)), "104", "Fil04", "216");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 5)), "105", "Fil05", "215");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 6)), "106", "Fil06", "214");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 7)), "107", "Fil07", "213");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 8)), "108", "Fil08", "212");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 9)), "109", "Fil09", "211");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 10)), "110", "Fil10", "210");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 11)), "111", "Fil11", "209");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 12)), "112", "Fil12", "208");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 13)), "113", "Fil13", "207");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 14)), "114", "Fil14", "206");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 15)), "115", "Fil15", "205");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 16)), "116", "Fil16", "204");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 17)), "117", "Fil17", "203");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 18)), "118", "Fil18", "202");
        assertTableRow(secondRowData.get(i++), formatDate(getModifiedDate(jobCreationTime, 19)), "119", "Fil19", "201");
    }

    @Test
    public void testEqualFilenames_ClickOnceOnFileNameColumn_SortAccordingly() throws IOException {
        final long jobCreationTime1 = new Date().getTime();
        jobstoreFolder.createTestJob(jobCreationTime1, "1", "b", "2");

        final long jobCreationTime2 = getModifiedDate(jobCreationTime1, 1);
        jobstoreFolder.createTestJob(jobCreationTime2, "2", "a", "1");

        final long jobCreationTime3 = jobCreationTime1;
        jobstoreFolder.createTestJob(jobCreationTime3, "3", "a", "2");

        final long jobCreationTime4 = jobCreationTime2;
        jobstoreFolder.createTestJob(jobCreationTime4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(2).click();  // Third column is Filename column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<SeleniumGWTTable.Cell>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime2), "2", "a", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime3), "3", "a", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime4), "4", "b", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime1), "1", "b", "2");
    }

    @Test
    public void testEqualFilenames_ClickTwiceOnFileNameColumn_SortAccordingly() throws IOException {
        final long jobCreationTime1 = new Date().getTime();
        jobstoreFolder.createTestJob(jobCreationTime1, "1", "b", "2");

        final long jobCreationTime2 = getModifiedDate(jobCreationTime1, 1);
        jobstoreFolder.createTestJob(jobCreationTime2, "2", "a", "1");

        final long jobCreationTime3 = jobCreationTime1;
        jobstoreFolder.createTestJob(jobCreationTime3, "3", "a", "2");

        final long jobCreationTime4 = jobCreationTime2;
        jobstoreFolder.createTestJob(jobCreationTime4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(2).click();  // Third column is Filename column => Ascending
        table.findColumnHeader(2).click();  // Third column is Filename column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<SeleniumGWTTable.Cell>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime4), "4", "b", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime1), "1", "b", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime2), "2", "a", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime3), "3", "a", "2");
    }


    @Test
    public void testEqualJobCreationTime_ClickOnceOnJobCreationTimeColumn_SortAccordingly() throws IOException {
        final long jobCreationTime1 = new Date().getTime();
        jobstoreFolder.createTestJob(jobCreationTime1, "1", "b", "2");

        final long jobCreationTime2 = getModifiedDate(jobCreationTime1, 1);
        jobstoreFolder.createTestJob(jobCreationTime2, "2", "a", "1");

        final long jobCreationTime3 = jobCreationTime1;
        jobstoreFolder.createTestJob(jobCreationTime3, "3", "a", "2");

        final long jobCreationTime4 = jobCreationTime2;
        jobstoreFolder.createTestJob(jobCreationTime4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the JobCreationTime Column
        table.findColumnHeader(0).click();  // First column is JobCreationTime column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<SeleniumGWTTable.Cell>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime1), "1", "b", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime3), "3", "a", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime2), "2", "a", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime4), "4", "b", "1");
    }

    @Test
    public void testEqualJobCreationTime_ClickTwiceOnJobCreationTimeColumn_SortAccordingly() throws IOException {
        final long jobCreationTime1 = new Date().getTime();
        jobstoreFolder.createTestJob(jobCreationTime1, "1", "b", "2");

        final long jobCreationTime2 = getModifiedDate(jobCreationTime1, 1);
        jobstoreFolder.createTestJob(jobCreationTime2, "2", "a", "1");

        final long jobCreationTime3 = jobCreationTime1;
        jobstoreFolder.createTestJob(jobCreationTime3, "3", "a", "2");

        final long jobCreationTime4 = jobCreationTime2;
        jobstoreFolder.createTestJob(jobCreationTime4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the JobCreationTime Column
        table.findColumnHeader(0).click();  // first column is JobCreationTime column => Descending
        table.findColumnHeader(0).click();  // first column is JobCreationTime column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<SeleniumGWTTable.Cell>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime2), "2", "a", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime4), "4", "b", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime1), "1", "b", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime3), "3", "a", "2");
    }

    @Test
    public void testEqualSubmitterNumbers_ClickOnceOnSubmitterNumberColumn_SortAccordingly() throws IOException {
        final long jobCreationTime1 = new Date().getTime();
        jobstoreFolder.createTestJob(jobCreationTime1, "1", "b", "2");

        final long jobCreationTime2 = getModifiedDate(jobCreationTime1, 1);
        jobstoreFolder.createTestJob(jobCreationTime2, "2", "a", "1");

        final long jobCreationTime3 = jobCreationTime1;
        jobstoreFolder.createTestJob(jobCreationTime3, "3", "a", "2");

        final long jobCreationTime4 = jobCreationTime2;
        jobstoreFolder.createTestJob(jobCreationTime4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column => Ascending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<SeleniumGWTTable.Cell>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime2), "2", "a", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime4), "4", "b", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime1), "1", "b", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime3), "3", "a", "2");
    }

    @Test
    public void testEqualSubmitterNumbers_ClickTwiceOnSubmitterNumberColumn_SortAccordingly() throws IOException {
        final long jobCreationTime1 = new Date().getTime();
        jobstoreFolder.createTestJob(jobCreationTime1, "1", "b", "2");

        final long jobCreationTime2 = getModifiedDate(jobCreationTime1, 1);
        jobstoreFolder.createTestJob(jobCreationTime2, "2", "a", "1");

        final long jobCreationTime3 = jobCreationTime1;
        jobstoreFolder.createTestJob(jobCreationTime3, "3", "a", "2");

        final long jobCreationTime4 = jobCreationTime2;
        jobstoreFolder.createTestJob(jobCreationTime4, "4", "b", "1");

        navigateToJobsShowWidget(webDriver);
        SeleniumGWTTable table = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        table.waitAssertRows(4);

        // Click on the Filename Column
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column => Ascending
        table.findColumnHeader(3).click();  // Fourth column is Submitternumber column => Descending
        SeleniumGWTTable sortedTable = new SeleniumGWTTable(webDriver, JobsShowViewImpl.GUIID_JOBS_SHOW_WIDGET);
        sortedTable.waitAssertRows(4);
        List<List<SeleniumGWTTable.Cell>> tableData = sortedTable.get();

        // Assert that data is as expected
        int i = 0;
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime1), "1", "b", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime3), "3", "a", "2");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime2), "2", "a", "1");
        assertTableRow(tableData.get(i++), formatDate(jobCreationTime4), "4", "b", "1");
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

    private void assertTableRow(List<SeleniumGWTTable.Cell> row, String jobCreationTime, String jobId, String fileName, String submitterId) {
        assertTrue(row.get(0).getCellContent().equals(jobCreationTime));
        assertThat(row.get(1).getCellContent(), is(jobId));
        assertThat(row.get(2).getCellContent(), is(fileName));
        assertThat(row.get(3).getCellContent(), is(submitterId));
        assertThat(row.get(4).hasClassName(JobsShowViewImpl.GUICLASS_GREEN), is(true));
    }

    private long getModifiedDate(long currentDate, int minutesToAdd){
        final long ONE_MINUTE_IN_MILLIS = 60000; //one minute in milliseconds.
        return (new Date(currentDate + (ONE_MINUTE_IN_MILLIS * minutesToAdd))).getTime();
    }

    private String formatDate(long date){
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(new Date(date));
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
            createTestJob(jobCreationTime, jobId, fileName, submitterNumber, JobErrorCode.NO_ERROR, true, 0L, 1L, 0L);
        }

        private void createTestJob(long jobCreationTime, String jobId, final String fileName, String submitterNumber, JobErrorCode jobErrorCode, boolean isDone, long failure, long success, long ignore) throws IOException {
            // Build JobSpecification JSON
            JobSpecificationJsonBuilder jobSpecificationBuilder = new JobSpecificationJsonBuilder();
            jobSpecificationBuilder.setDataFile(fileName);
            jobSpecificationBuilder.setSubmitterId(Long.parseLong(submitterNumber));
            // Build  JobInfo JSON
            JobInfoJsonBuilder jobInfoBuilder = new JobInfoJsonBuilder();
            jobInfoBuilder.setJobId(Long.parseLong(jobId));
            jobInfoBuilder.setJobSpecification(jobSpecificationBuilder.build());
            jobInfoBuilder.setJobCreationTime(jobCreationTime);
            jobInfoBuilder.setJobErrorCode(jobErrorCode);

            if (isDone) {
                jobInfoBuilder.setChunkifyingChunkCounter(new ChunkCounterJsonBuilder()
                        .setItemResultCounter(new ItemResultCounterJsonBuilder().setFailure(failure).setIgnore(ignore).setSuccess(success).build())
                        .build());
                jobInfoBuilder.setProcessingChunkCounter(new ChunkCounterJsonBuilder()
                        .setItemResultCounter(new ItemResultCounterJsonBuilder().setFailure(failure).setIgnore(ignore).setSuccess(success).build())
                        .build());
                jobInfoBuilder.setDeliveringChunkCounter(new ChunkCounterJsonBuilder()
                        .setItemResultCounter(new ItemResultCounterJsonBuilder().setFailure(failure).setIgnore(ignore).setSuccess(success).build())
                        .build());
            } else {
                jobInfoBuilder.setChunkifyingChunkCounter(null).build();
                jobInfoBuilder.setProcessingChunkCounter(null).build();
                jobInfoBuilder.setDeliveringChunkCounter(null).build();
            }

            // Store JobInfo in file system based job-store
            String res = jobInfoBuilder.build();
            createFile(jobId, JOBINFO_FILE_NAME, res);
        }
    }
}
