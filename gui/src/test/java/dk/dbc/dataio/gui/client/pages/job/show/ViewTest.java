package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.modelBuilders.WorkflowNoteModelBuilder;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Mock
    private CommonGinjector mockedCommonInjector;
    @Mock
    private ViewJobsGinjector mockedViewInjector;
    @Mock
    private Presenter mockedPresenter;
    @Mock
    private dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    private AsyncJobViewDataProvider mockedDataProvider;
    @Mock
    private ImageResource mockedImageResource;
    @Mock
    private Cell.Context mockedContext;

    private static final String VISIBLE = "visible";
    private static final String INVISIBLE = "invisible";

    // Test Data
    private JobModel testModel1 = new JobModel().withWorkflowNoteModel(new WorkflowNoteModelBuilder().build());

    // Subject Under Test
    private ViewConcrete view;

    // Mocked Texts
    @Mock
    private static Texts mockedTexts;
    private final static String MOCKED_LABEL_RERUNJOB = "Mocked Text: label_RerunJob";
    private final static String MOCKED_LABEL_RERUNJOBS = "Mocked Text: label_RerunJobs - count = @COUNT@";
    private final static String MOCKED_LABEL_RERUNJOBNO = "Mocked Text: label_RerunJobNo";
    private final static String MOCKED_LABEL_RERUNJOBCONFIRMATION = "Mocked Text: label_RerunJobConfirmation";
    private final static String MOCKED_LABEL_RERUNJOBSCONFIRMATION = "Mocked Text: label_RerunJobsConfirmation";
    private final static String MOCKED_COLUMNHEADER_JOBID = "Mocked Text: columnHeader_JobId";
    private final static String MOCKED_COLUMNHEADER_SUBMITTER = "Mocked Text: columnHeader_Submitter";
    private final static String MOCKED_COLUMNHEADER_FLOWBINDERNAME = "Mocked Text: columnHeader_FlowBinderName";
    private final static String MOCKED_COLUMNHEADER_SINKNAME = "Mocked Text: columnHeader_SinkName";
    private final static String MOCKED_COLUMNHEADER_TOTALCHUNKCOUNT = "Mocked Text: columnHeader_TotalChunkCount";
    private final static String MOCKED_COLUMNHEADER_JOBCREATIONTIME = "Mocked Text: columnHeader_JobCreationTime";
    private final static String MOCKED_COLUMNHEADER_FAILURECOUNTER = "Mocked Text: columnHeader_FailureCounter";
    private final static String MOCKED_COLUMNHEADER_IGNOREDCOUNTER = "Mocked Text: columnHeader_IgnoredCounter";
    private final static String MOCKED_COLUMNHEADER_PROGRESSBAR = "Mocked Text: columnHeader_ProgressBar";
    private final static String MOCKED_COLUMNHEADER_JOBSTATUS = "Mocked Text: columnHeader_JobStatus";
    private final static String MOCKED_BUTTON_RERUNJOB = "Mocked Text: button_RerunJob";
    private final static String MOCKED_ERROR_NOJOBSTORERUN = "Mocked Text: error_NoJobsToRerun";

    public class ViewConcrete extends View {

        public ViewConcrete() {
            super("header", false);
            this.commonInjector = mockedCommonInjector;
            this.viewInjector = mockedViewInjector;
        }

        @Override
        Texts getTexts() {
            return mockedTexts;
        }
    }


    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonInjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_Jobs()).thenReturn("Header Text");

        when(mockedTexts.label_RerunJob()).thenReturn(MOCKED_LABEL_RERUNJOB);
        when(mockedTexts.label_RerunJobs()).thenReturn(MOCKED_LABEL_RERUNJOBS);
        when(mockedTexts.label_RerunJobNo()).thenReturn(MOCKED_LABEL_RERUNJOBNO);
        when(mockedTexts.label_RerunJobConfirmation()).thenReturn(MOCKED_LABEL_RERUNJOBCONFIRMATION);
        when(mockedTexts.label_RerunJobsConfirmation()).thenReturn(MOCKED_LABEL_RERUNJOBSCONFIRMATION);
        when(mockedTexts.columnHeader_JobId()).thenReturn(MOCKED_COLUMNHEADER_JOBID);
        when(mockedTexts.columnHeader_Submitter()).thenReturn(MOCKED_COLUMNHEADER_SUBMITTER);
        when(mockedTexts.columnHeader_FlowBinderName()).thenReturn(MOCKED_COLUMNHEADER_FLOWBINDERNAME);
        when(mockedTexts.columnHeader_SinkName()).thenReturn(MOCKED_COLUMNHEADER_SINKNAME);
        when(mockedTexts.columnHeader_TotalChunkCount()).thenReturn(MOCKED_COLUMNHEADER_TOTALCHUNKCOUNT);
        when(mockedTexts.columnHeader_JobCreationTime()).thenReturn(MOCKED_COLUMNHEADER_JOBCREATIONTIME);
        when(mockedTexts.columnHeader_FailureCounter()).thenReturn(MOCKED_COLUMNHEADER_FAILURECOUNTER);
        when(mockedTexts.columnHeader_IgnoredCounter()).thenReturn(MOCKED_COLUMNHEADER_IGNOREDCOUNTER);
        when(mockedTexts.columnHeader_ProgressBar()).thenReturn(MOCKED_COLUMNHEADER_PROGRESSBAR);
        when(mockedTexts.columnHeader_JobStatus()).thenReturn(MOCKED_COLUMNHEADER_JOBSTATUS);
        when(mockedTexts.button_RerunJob()).thenReturn(MOCKED_BUTTON_RERUNJOB);
        when(mockedTexts.error_NoJobsToRerun()).thenReturn(MOCKED_ERROR_NOJOBSTORERUN);
    }

    //Testing starts here...
    @Test
    @SuppressWarnings("unchecked")
    public void constructor_instantiate_objectCorrectInitialized() {
        // Subject Under Test
        ViewConcrete viewConcrete = new ViewConcrete();
        viewConcrete.setupColumns();

        // Verify invocations
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), isA(View.HideShowColumnHeader.class));
        verify(viewConcrete.jobsTable, times(4)).addColumn(isA(Column.class), isA(View.HidableColumnHeader.class));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_JOBCREATIONTIME));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_JOBID));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SUBMITTER));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FLOWBINDERNAME));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_SINKNAME));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_TOTALCHUNKCOUNT));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_FAILURECOUNTER));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_IGNOREDCOUNTER));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_PROGRESSBAR));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMNHEADER_JOBSTATUS));
        verify(viewConcrete.jobsTable).setSelectionModel(isA(SingleSelectionModel.class));
        verify(viewConcrete.jobsTable).addDomHandler(isA(DoubleClickHandler.class), isA(DomEvent.Type.class));
        verifyNoMoreInteractions(viewConcrete.jobsTable);
        verify(viewConcrete.pagerTop).setDisplay(viewConcrete.jobsTable);
        verifyNoMoreInteractions(viewConcrete.pagerTop);
        verify(viewConcrete.pagerBottom).setDisplay(viewConcrete.jobsTable);
        verifyNoMoreInteractions(viewConcrete.pagerBottom);
        assertThat(viewConcrete.refreshButton, not(nullValue()));
        assertThat(viewConcrete.showJobButton, not(nullValue()));
        assertThat(viewConcrete.jobIdInputField, not(nullValue()));
    }

    @Test
    public void refreshJobsTable_call_ok() {
        // Subject Under Test
        view = new ViewConcrete();

        // Subject Under Test
        view.refreshJobsTable();

        // Verify test
        verify(view.jobsTable).getVisibleRange();
        verify(view.jobsTable).setVisibleRangeAndClearData(any(Range.class), eq(true));
        verifyNoMoreInteractions(view.jobsTable);
    }

    @Test
    public void loadJobsTable_dataProviderNotSetup_nop() {
        // Subject Under Test
        view = new ViewConcrete();
        view.dataProvider = null;

        // Subject Under Test
        view.loadJobsTable();

        // Verify test
        verifyNoMoreInteractions(view.jobsTable);
    }

    @Test
    public void loadJobsTable_oneLoad_setVisibleRangeAndClearData() {
        // Subject Under Test
        view = new ViewConcrete();

        // Subject Under Test
        view.loadJobsTable();

        // Verify test
        verify(view.jobsTable, times(1)).setVisibleRangeAndClearData(any(Range.class), eq(true));
        verify(view.jobsTable).getVisibleRange();
        verifyNoMoreInteractions(view.jobsTable);
    }

    @Test
    public void loadJobsTable_twoLoads_setVisibleRangeAndClearData() {
        // Subject Under Test
        view = new ViewConcrete();

        // Subject Under Test
        view.loadJobsTable();
        view.loadJobsTable();  // This one does not generate new calls to setVisibleRangeAndClearData !

        // Verify test
        verify(view.jobsTable, times(1)).setVisibleRangeAndClearData(any(Range.class), eq(true));
        verify(view.jobsTable).getVisibleRange();
        verifyNoMoreInteractions(view.jobsTable);
    }

    @Test
    public void rerunAllShownJobs_countIsZero_noJobsToRerunDialogBox() {
        // Subject Under Test
        view = new ViewConcrete();
        view.presenter = mockedPresenter;
        when(view.presenter.validRerunJobsFilter(anyList())).thenReturn(Arrays.asList());

        // Subject Under Test
        view.rerunAllShownJobs();

        // Verify test
        verifyDialogBoxOperations("", MOCKED_ERROR_NOJOBSTORERUN, "", false);
        verifyNoMoreInteractions(view.jobsTable);
    }

    @Test
    public void rerunAllShownJobs_countIsThree_threeJobsToRerunDialogBox() {
        view = new ViewConcrete();
        view.presenter = mockedPresenter;

        final JobModel jobModel1 = new JobModel().withJobId("1");
        final JobModel jobModel2 = new JobModel().withJobId("2");
        final JobModel jobModel3 = new JobModel().withJobId("3");

        when(mockedPresenter.validRerunJobsFilter(anyList())).thenReturn(Arrays.asList(jobModel1, jobModel2, jobModel3));

        // Subject Under Test
        view.rerunAllShownJobs();

        // Verification
        verifyDialogBoxOperations("Mocked Text: label_RerunJobs - count = 3", "1, 2, 3", MOCKED_LABEL_RERUNJOBSCONFIRMATION, true);
        verifyNoMoreInteractions(view.jobsTable);
    }


    @Test
    @SuppressWarnings("unchecked")
    public void constructHideShowColumn_jobModelIsNull_calculateCorrectHtmlSnippet() {
        view = new ViewConcrete();

        View.HideShowCell hideShowCell = (View.HideShowCell) view.constructHideShowWorkflow();
        when(mockedContext.getKey()).thenReturn(null);
        Cell<ImageResource> cell = hideShowCell.getCell();
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        when(mockedImageResource.getWidth()).thenReturn(11);
        when(mockedImageResource.getHeight()).thenReturn(22);
        when(mockedImageResource.getSafeUri()).thenReturn(() -> "[Test Image]");

        // Subject Under Test
        cell.render(mockedContext, mockedImageResource, safeHtmlBuilder);

        // Verify Test
        assertThat(safeHtmlBuilder.toSafeHtml().asString(), is("image([Test Image], 11, 22)"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructHideShowColumn_jobModelIsValidWithZeroValuedPreviousJobId_calculateCorrectHtmlSnippet() {
        view = new ViewConcrete();

        View.HideShowCell hideShowCell = (View.HideShowCell) view.constructHideShowWorkflow();
        JobModel jobModel = new JobModel().withPreviousJobIdAncestry(0);
        when(mockedContext.getKey()).thenReturn(jobModel);
        Cell<ImageResource> cell = hideShowCell.getCell();
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        when(mockedImageResource.getWidth()).thenReturn(11);
        when(mockedImageResource.getHeight()).thenReturn(22);
        when(mockedImageResource.getSafeUri()).thenReturn(() -> "[Test Image]");

        // Subject Under Test
        cell.render(mockedContext, mockedImageResource, safeHtmlBuilder);

        // Verify Test
        assertThat(safeHtmlBuilder.toSafeHtml().asString(), is("image([Test Image], 11, 22)"));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void constructHideShowColumn_jobModelIsValidWithZeroValuedAncestry_calculateCorrectHtmlSnippet() {
        view = new ViewConcrete();

        View.HideShowCell hideShowCell = (View.HideShowCell) view.constructHideShowWorkflow();
        when(mockedContext.getKey()).thenReturn(new JobModel());
        Cell<ImageResource> cell = hideShowCell.getCell();
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        when(mockedImageResource.getWidth()).thenReturn(11);
        when(mockedImageResource.getHeight()).thenReturn(22);
        when(mockedImageResource.getSafeUri()).thenReturn(() -> "[Test Image]");

        // Subject Under Test
        cell.render(mockedContext, mockedImageResource, safeHtmlBuilder);

        // Verify Test
        assertThat(safeHtmlBuilder.toSafeHtml().asString(), is("image([Test Image], 11, 22)"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructHideShowColumn_jobModelIsValidWithValidPreviousJobId_calculateCorrectHtmlSnippet() {
        view = new ViewConcrete();

        View.HideShowCell hideShowCell = (View.HideShowCell) view.constructHideShowWorkflow();
        when(mockedContext.getKey()).thenReturn(new JobModel().withPreviousJobIdAncestry(1234));
        Cell<ImageResource> cell = hideShowCell.getCell();
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        when(mockedImageResource.getWidth()).thenReturn(11);
        when(mockedImageResource.getHeight()).thenReturn(22);
        when(mockedImageResource.getSafeUri()).thenReturn(() -> "[Test Image]");

        // Subject Under Test
        cell.render(mockedContext, mockedImageResource, safeHtmlBuilder);

        // Verify Test
        assertThat(safeHtmlBuilder.toSafeHtml().asString(), is("<span title='Mocked Text: label_RerunJobNo 1234'>image([Test Image], 11, 22)</span>"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructIsFixedColumn_callWhileVisible_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructIsFixedColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getWorkflowNoteModel().isProcessed()));
        assertThat(column.getCellStyleNames(null, null), is(VISIBLE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructIsFixedColumn_callWhileNotVisible_correctlySetup() {
        view = new ViewConcrete();
        view.HideColumn(true);

        // Subject Under Test
        Column column = view.constructIsFixedColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getWorkflowNoteModel().isProcessed()));
        assertThat(column.getCellStyleNames(null, null), is(INVISIBLE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructAssigneeColumn_callWhileVisible_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructAssigneeColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getWorkflowNoteModel().getAssignee()));
        assertThat(column.getCellStyleNames(null, null), is(VISIBLE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructAssigneeColumn_callWhileNotVisible_correctlySetup() {
        view = new ViewConcrete();
        view.HideColumn(true);

        // Subject Under Test
        Column column = view.constructAssigneeColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getWorkflowNoteModel().getAssignee()));
        assertThat(column.getCellStyleNames(null, null), is(INVISIBLE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJobCreationTimeColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructJobCreationTimeColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getJobCreationTime()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructJobIdColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructJobIdColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getJobId()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSubmitterNumberColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructSubmitterColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getSubmitterNumber() + " (" + testModel1.getSubmitterName() + ")"));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructFlowBinderNameColumn_call_correctlySetup() {
        view = new ViewConcrete();
        view.dataProvider = mockedDataProvider;

        // Subject Under Test
        Column column = view.constructFlowBinderNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getFlowBinderName()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructSinkNameColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructSinkNameColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getSinkName()));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));
    }


    @Test
    @SuppressWarnings("unchecked")
    public void constructFailedCounterColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructFailedCounterColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(String.valueOf(testModel1.getStateModel().getFailedCounter())));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructIgnoredCounterColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructIgnoredCounterColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(String.valueOf(testModel1.getStateModel().getProcessing().getIgnored())));

        // Test that column is set to sortable
        assertThat(column.isSortable(), is(false));

        // Test that column is set to ascending sorting
        assertThat(column.isDefaultSortAscending(), is(true));
    }

    @Test
    public void constructProgressColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        ProgressColumn column = (ProgressColumn) view.constructProgressBarColumn();
        assertThat(column.getCell(), is(notNullValue()));
        assertThat(column.getCell() instanceof ProgressColumn.ProgressCell, is(true));
    }

    @Test
    public void constructJobStateColumn_call_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        StatusColumn column = (StatusColumn) view.constructJobStateColumn();
        assertThat(column.getCell(), is(notNullValue()));
        assertThat(column.getCell() instanceof ImageResourceCell, is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRerunColumn_callWhileVisible_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructRerunColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(null), is(MOCKED_BUTTON_RERUNJOB));
        assertThat(column.getCellStyleNames(null, null), is(VISIBLE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRerunColumn_callWhileNotVisible_correctlySetup() {
        view = new ViewConcrete();
        view.HideColumn(true);

        // Subject Under Test
        Column column = view.constructRerunColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(null), is(MOCKED_BUTTON_RERUNJOB));
        assertThat(column.getCellStyleNames(null, null), is(INVISIBLE));

        // Test that setFieldUpdater is setup correctly, by calling it and verify behaviour
        JobModel testModel = new JobModel().withJobCompletionTime("time");
        view.setPresenter(mockedPresenter);

        column.getFieldUpdater().update(0, testModel, "");
        verify(mockedPresenter).getJobRerunScheme(testModel);
        verify(mockedPresenter).setIsMultipleRerun(false);
        verifyNoMoreInteractions(mockedPresenter);
    }

    public void verifyDialogBoxOperations(String count, String list, String confirmation, Boolean hideOkButton) {
        verify(view.rerunJobsCount).setText(count);
        verify(view.rerunJobsList).setText(list);
        verify(view.rerunJobsConfirmation).setText(confirmation);
        verify(view.rerunOkButton).setVisible(hideOkButton);
        verify(view.rerunAllShownJobsConfirmationDialog, times(2)).show();
    }

}
