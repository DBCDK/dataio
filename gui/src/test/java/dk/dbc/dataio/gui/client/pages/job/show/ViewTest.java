package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.modelBuilders.JobModelBuilder;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
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
    @Mock Presenter mockedPresenter;
    @Mock Resources mockedResources;
    @Mock static ClickEvent mockedClickEvent;
    @Mock SingleSelectionModel mockedSelectionModel;
    @Mock JobModel mockedJobModel;
    @Mock CellPreviewEvent<JobModel> mockedCellPreviewEvent;
    @Mock NativeEvent mockedNativeEvent;
    @Mock ClientFactory mockedClientFactory;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;

    @Mock AsyncJobViewDataProvider mockedDataProvider;

    // Test Data
    private JobModel testModel1 = new JobModelBuilder()
            .setJobCreationTime("2014-12-16 08:51:17")
            .setJobId("1418716277429")
            .setSubmitterNumber("150014")
            .setSubmitterName("SubmitterNameA")
            .setFlowBinderName("FlowBinderNameA")
            .setSinkId(3456L)
            .setSinkName("SinkNameA")
            .setItemCounter(12)
            .setFailedCounter(2)
            .setIgnoredCounter(3)
            .setPartitionedCounter(51)
            .setProcessedCounter(52)
            .setDeliveredCounter(53)
            .build();

    // Subject Under Test
    private View view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_JOBS = "Mocked Label Jobs";
    final static String MOCKED_COLUMN_HEADER_JOB_CREATION_TIME = "Mocked Column Header Job Creation Time";
    final static String MOCKED_COLUMN_HEADER_JOB_ID = "Mocked Column Header Job Id";
    final static String MOCKED_COLUMN_HEADER_SUBMITTER_NUMBER = "Mocked Column Header Submitter Number";
    final static String MOCKED_COLUMN_HEADER_SUBMITTER_NAME = "Mocked Column Header Submitter Name";
    final static String MOCKED_COLUMN_HEADER_FLOW_BINDER_NAME = "Mocked Column Header Flow Binder Name";
    final static String MOCKED_COLUMN_HEADER_SINK_NAME = "Mocked Column Header Sink Name";
    final static String MOCKED_COLUMN_HEADER_ITEM_COUNTER = "Mocked Column Header Item Counter";
    final static String MOCKED_COLUMN_HEADER_FAILED = "Mocked Column Header Failed";
    final static String MOCKED_COLUMN_HEADER_IGNORED = "Mocked Column Header Ignored";
    final static String MOCKED_COLUMN_HEADER_PROGRESS = "Mocked Column Header Progress";
    final static String MOCKED_COLUMN_HEADER_JOB_STATUS = "Mocked Column Header Job Status";

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getJobsShowTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Jobs()).thenReturn("Header Text");

        when(mockedTexts.label_Jobs()).thenReturn(MOCKED_LABEL_JOBS);
        when(mockedTexts.columnHeader_JobCreationTime()).thenReturn(MOCKED_COLUMN_HEADER_JOB_CREATION_TIME);
        when(mockedTexts.columnHeader_JobId()).thenReturn(MOCKED_COLUMN_HEADER_JOB_ID);
        when(mockedTexts.columnHeader_SubmitterNumber()).thenReturn(MOCKED_COLUMN_HEADER_SUBMITTER_NUMBER);
        when(mockedTexts.columnHeader_SubmitterName()).thenReturn(MOCKED_COLUMN_HEADER_SUBMITTER_NAME);
        when(mockedTexts.columnHeader_FlowBinderName()).thenReturn(MOCKED_COLUMN_HEADER_FLOW_BINDER_NAME);
        when(mockedTexts.columnHeader_SinkName()).thenReturn(MOCKED_COLUMN_HEADER_SINK_NAME);
        when(mockedTexts.columnHeader_TotalChunkCount()).thenReturn(MOCKED_COLUMN_HEADER_ITEM_COUNTER);
        when(mockedTexts.columnHeader_FailureCounter()).thenReturn(MOCKED_COLUMN_HEADER_FAILED);
        when(mockedTexts.columnHeader_IgnoredCounter()).thenReturn(MOCKED_COLUMN_HEADER_IGNORED);
        when(mockedTexts.columnHeader_ProgressBar()).thenReturn(MOCKED_COLUMN_HEADER_PROGRESS);
        when(mockedTexts.columnHeader_JobStatus()).thenReturn(MOCKED_COLUMN_HEADER_JOB_STATUS);
    }

    /*
     * Testing starts here...
     */
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        view = new View(mockedClientFactory, "Header Text", false, false);

        view.setupColumns();
        // Verify invocations
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_JOB_CREATION_TIME));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_JOB_ID));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_SUBMITTER_NUMBER));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_SUBMITTER_NAME));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_FLOW_BINDER_NAME));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_SINK_NAME));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_ITEM_COUNTER));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_FAILED));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_IGNORED));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_PROGRESS));
        verify(view.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_JOB_STATUS));
        verify(view.pagerTop).setDisplay(view.jobsTable);
        verify(view.pagerBottom).setDisplay(view.jobsTable);
        assertThat(view.refreshButton, not(nullValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJobCreationTimeColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        Column column = view.constructJobCreationTimeColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getJobCreationTime()));



    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJobIdColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        Column column = view.constructJobIdColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getJobId()));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmitterNumberColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        Column column = view.constructSubmitterNumberColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getSubmitterNumber()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmitterNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        Column column = view.constructSubmitterNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getSubmitterName()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowBinderNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);
        view.dataProvider = mockedDataProvider;

        // Subject Under Test
        Column column = view.constructFlowBinderNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getFlowBinderName()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSinkNameColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        Column column = view.constructSinkNameColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(testModel1.getSinkName()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));

    }



    @Test
    @SuppressWarnings("unchecked")
    public void constructFailedCounterColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        Column column = view.constructFailedCounterColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(String.valueOf(testModel1.getFailedCounter())));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructIgnoredCounterColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        Column column = view.constructIgnoredCounterColumn();

        // Test that correct getValue handler has been setup
        assertThat((String) column.getValue(testModel1), is(String.valueOf(testModel1.getProcessingIgnoredCounter())));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));

        // Test that column is set to ascending sorting
        assertThat(column.isDefaultSortAscending(), is(true));
    }

    @Test
    public void constructJobStateColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        StatusColumn column = (StatusColumn) view.constructJobStateColumn();
        assertThat(column.getCell(), is(notNullValue()));
        assertThat(column.getCell() instanceof ImageResourceCell, is(true));
    }

    @Test
    public void constructProgressColumn_call_correctlySetup() {
        view = new View(mockedClientFactory, "Header Text", false, false);

        // Subject Under Test
        ProgressColumn column = (ProgressColumn) view.constructProgressBarColumn();
        assertThat(column.getCell(), is(notNullValue()));
        assertThat(column.getCell() instanceof ProgressColumn.ProgressCell, is(true));
    }

}