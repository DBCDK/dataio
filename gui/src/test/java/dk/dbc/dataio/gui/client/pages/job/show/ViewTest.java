package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwtmockito.GwtMock;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.panels.statuspopup.StatusPopupEvent;
import dk.dbc.dataio.gui.client.resources.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * PresenterImpl unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class ViewTest {
    @GwtMock View.MyEventBinder mockedStatusPopupEventBinder;
    @Mock Presenter mockedPresenter;
    @Mock Resources mockedResources;
    @Mock static ClickEvent mockedClickEvent;


    // Test Data
    private JobModel testModel1 = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014",
            true, JobErrorCode.NO_ERROR,
            15, 4, 5, 6,   // Chunkifying: total, success, failure, ignored
            18, 5, 6, 7,   // Processing:  total, success, failure, ignored
            21, 6, 7, 8);  // Delivering:  total, success, failure, ignored

    private JobModel testModel2 = new JobModel("2014-12-17 00:37:48", "1418773068083",
            "urn:dataio-fs:46551", "424242",
            true, JobErrorCode.NO_ERROR,
            6, 1, 2, 3,    // Chunkifying: total, success, failure, ignored
            9, 2, 3, 4,    // Processing:  total, success, failure, ignored
            12, 3, 4, 5);  // Delivering:  total, success, failure, ignored
    private List<JobModel> testModels = new ArrayList<JobModel>(Arrays.asList(testModel1, testModel2));

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_JOBS = "Mocked Label Jobs";
    final static String MOCKED_COLUMNHEADER_JOB_ID = "Mocked Column Header Job Id";
    final static String MOCKED_COLUMNHEADER_FILE_NAME = "Mocked Column Header File Name";
    final static String MOCKED_COLUMNHEADER_SUBMITTER_NUMBER = "Mocked Column Header Submitter Number";
    final static String MOCKED_COLUMNHEADER_JOB_CREATION_TIME = "Mocked Column Header Job Creation Time";
    final static String MOCKED_COLUMNHEADER_JOB_STATUS = "Mocked Column Header Job Status";
    final static String MOCKED_BUTTON_SHOW_MORE = "Mocked Button Show More";
    final static String MOCKED_LINK_MORE_INFO = "Mocked Link More Info";
    final static String MOCKED_LINK_FAILED_ITEMS = "Mocked Link Failed Items";
    final static String MOCKED_TEXT_CHUNKIFYING = "Mocked Text Chunkifying";
    final static String MOCKED_TEXT_PROCESSING = "Mocked Text Processing";
    final static String MOCKED_TEXT_DELIVERING = "Mocked Text Delivering";
    final static String MOCKED_TEXT_DONE = "Mocked Text Done";
    final static String MOCKED_TEXT_RECORD = "Mocked Text Record";
    final static String MOCKED_TEXT_RECORDS = "Mocked Text RecordS";
    final static String MOCKED_TEXT_FAILED = "Mocked Text Failed";
    final static String MOCKED_TEXT_PENDING = "Mocked Text Pending";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedTexts.label_Jobs()).thenReturn(MOCKED_LABEL_JOBS);
        when(mockedTexts.columnHeader_JobId()).thenReturn(MOCKED_COLUMNHEADER_JOB_ID);
        when(mockedTexts.columnHeader_FileName()).thenReturn(MOCKED_COLUMNHEADER_FILE_NAME);
        when(mockedTexts.columnHeader_SubmitterNumber()).thenReturn(MOCKED_COLUMNHEADER_SUBMITTER_NUMBER);
        when(mockedTexts.columnHeader_JobCreationTime()).thenReturn(MOCKED_COLUMNHEADER_JOB_CREATION_TIME);
        when(mockedTexts.columnHeader_JobStatus()).thenReturn(MOCKED_COLUMNHEADER_JOB_STATUS);
        when(mockedTexts.button_ShowMore()).thenReturn(MOCKED_BUTTON_SHOW_MORE);
        when(mockedTexts.link_MoreInfo()).thenReturn(MOCKED_LINK_MORE_INFO);
        when(mockedTexts.link_FailedItems()).thenReturn(MOCKED_LINK_FAILED_ITEMS);
        when(mockedTexts.text_chunkifying()).thenReturn(MOCKED_TEXT_CHUNKIFYING);
        when(mockedTexts.text_processing()).thenReturn(MOCKED_TEXT_PROCESSING);
        when(mockedTexts.text_delivering()).thenReturn(MOCKED_TEXT_DELIVERING);
        when(mockedTexts.text_done()).thenReturn(MOCKED_TEXT_DONE);
        when(mockedTexts.text_record()).thenReturn(MOCKED_TEXT_RECORD);
        when(mockedTexts.text_records()).thenReturn(MOCKED_TEXT_RECORDS);
        when(mockedTexts.text_failed()).thenReturn(MOCKED_TEXT_FAILED);
        when(mockedTexts.text_pending()).thenReturn(MOCKED_TEXT_PENDING);
    }

    /*
     * Testing starts here...
     */
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View("Header Text", mockedTexts, mockedResources);

        // Verify invocations
        verify(mockedStatusPopupEventBinder).bindEventHandlers(eq(view), isA(EventBus.class));
        verify(view.jobsTable).addColumnSortHandler(isA(ColumnSortEvent.ListHandler.class));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_JOB_CREATION_TIME));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_JOB_ID));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FILE_NAME));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SUBMITTER_NUMBER));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_JOB_STATUS));
        verify(view.pagerTop).setDisplay(view.jobsTable);
        verify(view.pagerBottom).setDisplay(view.jobsTable);
    }


    @Test
    public void constructor_setupData_dataSetupCorrect() {
        view = new View("Header Text", mockedTexts, mockedResources);

        // Subject Under Test
        view.setJobs(testModels);

        verify(view.jobsTable).getColumnSortList();
        verify(view.jobsTable).setPageSize(20);
        verify(view.jobsTable).setRowCount(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJobCreationTimeColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts, mockedResources);

        // Subject Under Test
        Column column = view.constructJobCreationTimeColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getJobCreationTime()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(true));

        // Test that correct comparator has been setup
        // JobCreationTime for testModel1 is earlier than for testModel2
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel1), is(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel2), lessThan(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel2, testModel1), greaterThan(0));

        // Test that column is set to descending sorting
        assertThat(column.isDefaultSortAscending(), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJobIdColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts, mockedResources);

        // Subject Under Test
        Column column = view.constructJobIdColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getJobId()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(true));

        // Test that correct comparator has been setup
        // JobId for testModel1 is less than testModel2
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel1), is(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel2), lessThan(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel2, testModel1), greaterThan(0));

        // Test that column is set to ascending sorting
        assertThat(column.isDefaultSortAscending(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFileNameColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts, mockedResources);

        // Subject Under Test
        Column column = view.constructFileNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getFileName()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(true));

        // Test that correct comparator has been setup
        // Filename for testModel1 comes before testModel2
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel1), is(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel2), lessThan(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel2, testModel1), greaterThan(0));

        // Test that column is set to ascending sorting
        assertThat(column.isDefaultSortAscending(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmitterNumberColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts, mockedResources);

        // Subject Under Test
        Column column = view.constructSubmitterNumberColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getSubmitterNumber()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(true));

        // Test that correct comparator has been setup
        // Submitternumber for testModel1 is less than testModel2
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel1), is(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel2), lessThan(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel2, testModel1), greaterThan(0));

        // Test that column is set to ascending sorting
        assertThat(column.isDefaultSortAscending(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructChunkCountColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts, mockedResources);

        // Subject Under Test
        Column column = view.constructChunkCountColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(String.valueOf(testModel1.getChunkifyingTotalCounter())));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(true));

        // Test that correct comparator has been setup
        // ChunkifyingTotalCounter for testModel1 (15) is larger than testModel2 (6)
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel1), is(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel1, testModel2), greaterThan(0));
        assertThat(view.columnSortHandler.getComparator(column).compare(testModel2, testModel1), lessThan(0));

        // Test that column is set to ascending sorting
        assertThat(column.isDefaultSortAscending(), is(true));
    }

    @Test
    public void constructJobStateColumn_call_correctlySetup() {
        view = new View("Header Text", mockedTexts, mockedResources);

        // Subject Under Test
        StatusColumn column = (StatusColumn) view.constructJobStateColumn();

        // Test that correct getConsumedEvents handler has been setup
        ImageResourceCell cell = (ImageResourceCell) column.getCell();
        Set<String> events = cell.getConsumedEvents();
        assertThat(events, notNullValue());
        assertThat(events.size(), is(1));
        assertThat(events.contains("click"), is(true));
    }


    /*
     * Testing Event Handlers
     */
    @Test
    public void statusPopupEvent_totalStatusInfo_showFailedItemsCalled() {
        view = new View("Header Text", mockedTexts, mockedResources);
        view.setJobs(testModels);
        view.setPresenter(mockedPresenter);

        // Subject Under Test
        view.statusPopupEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.TOTAL_STATUS_INFO, "1234"));

        // Verify correct behavior
        verify(mockedPresenter).showFailedItems("1234", null, null);
    }

    @Test
    public void statusPopupEvent_moreInformationRequested_showMoreInformationCalled() {
        view = new View("Header Text", mockedTexts, mockedResources);
        view.setJobs(testModels);
        view.setPresenter(mockedPresenter);

        // Subject Under Test
        view.statusPopupEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.MORE_INFORMATION_REQUESTED, "2345"));

        // Verify correct behavior
        verify(mockedPresenter).showMoreInformation("2345");
    }

    @Test
    public void statusPopupEvent_detailedStatus_showFailedItemsCalled() {
        view = new View("Header Text", mockedTexts, mockedResources);
        view.setJobs(testModels);
        view.setPresenter(mockedPresenter);

        // Subject Under Test
        view.statusPopupEvent(new StatusPopupEvent(StatusPopupEvent.StatusPopupEventType.DETAILED_STATUS,
                "789",
                JobState.OperationalState.PROCESSING,
                ItemCompletionState.State.FAILURE));

        // Verify correct behavior
        verify(mockedPresenter).showFailedItems("789", JobState.OperationalState.PROCESSING, ItemCompletionState.State.FAILURE);
    }

}