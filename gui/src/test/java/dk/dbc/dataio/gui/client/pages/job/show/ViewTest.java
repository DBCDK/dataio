/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.modelBuilders.JobModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.WorkflowNoteModelBuilder;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
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

    @Mock CommonGinjector mockedCommonInjector;
    @Mock ViewJobsGinjector mockedViewInjector;
    @Mock Presenter mockedPresenter;
    @Mock Resources mockedResources;
    @Mock SingleSelectionModel mockedSelectionModel;
    @Mock JobModel mockedJobModel;
    @Mock WorkflowNoteModel mockedWorkflowNoteModel;
    @Mock CellPreviewEvent<JobModel> mockedCellPreviewEvent;
    @Mock NativeEvent mockedNativeEvent;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock AsyncJobViewDataProvider mockedDataProvider;
    @Mock ImageResource mockedImageResource;
    @Mock Cell.Context mockedContext;


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
            .setWorkflowNoteModel(new WorkflowNoteModelBuilder().setAssignee("testAssignee").build())
            .build();

    // Subject Under Test
    private ViewConcrete view;

    // Mocked Texts
    @Mock static Texts mockedTexts;
    final static String MOCKED_LABEL_JOBS = "Mocked Label Jobs";
    final static String MOCKED_COLUMN_HEADER_JOB_CREATION_TIME = "Mocked Column Header Job Creation Time";
    final static String MOCKED_COLUMN_HEADER_JOB_ID = "Mocked Column Header Job Id";
    final static String MOCKED_COLUMN_HEADER_SUBMITTER = "Mocked Column Header Submitter";
    final static String MOCKED_COLUMN_HEADER_FLOW_BINDER_NAME = "Mocked Column Header Flow Binder Name";
    final static String MOCKED_COLUMN_HEADER_SINK_NAME = "Mocked Column Header Sink Name";
    final static String MOCKED_COLUMN_HEADER_ITEM_COUNTER = "Mocked Column Header Item Counter";
    final static String MOCKED_COLUMN_HEADER_FAILED = "Mocked Column Header Failed";
    final static String MOCKED_COLUMN_HEADER_IGNORED = "Mocked Column Header Ignored";
    final static String MOCKED_COLUMN_HEADER_PROGRESS = "Mocked Column Header Progress";
    final static String MOCKED_COLUMN_HEADER_JOB_STATUS = "Mocked Column Header Job Status";
    final static String MOCKED_COLUMN_HEADER_IS_FIXED = "Mocked Column Header Fixed";
    final static String MOCKED_COLUMN_HEADER_ASSIGNEE = "Mocked Column Header Assignee";
    final static String MOCKED_BUTTON_RERUN_JOB = "Mocked Button Rerun Job";
    final static String MOCKED_LABEL_RERUN_JOB_NO = "Mocked Label Rerun Job No";


    public class ViewConcrete extends View {

        public ViewConcrete() {
            super("header", false, false);
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

        when(mockedTexts.label_Jobs()).thenReturn(MOCKED_LABEL_JOBS);
        when(mockedTexts.columnHeader_JobCreationTime()).thenReturn(MOCKED_COLUMN_HEADER_JOB_CREATION_TIME);
        when(mockedTexts.columnHeader_JobId()).thenReturn(MOCKED_COLUMN_HEADER_JOB_ID);
        when(mockedTexts.columnHeader_Submitter()).thenReturn(MOCKED_COLUMN_HEADER_SUBMITTER);
        when(mockedTexts.columnHeader_FlowBinderName()).thenReturn(MOCKED_COLUMN_HEADER_FLOW_BINDER_NAME);
        when(mockedTexts.columnHeader_SinkName()).thenReturn(MOCKED_COLUMN_HEADER_SINK_NAME);
        when(mockedTexts.columnHeader_TotalChunkCount()).thenReturn(MOCKED_COLUMN_HEADER_ITEM_COUNTER);
        when(mockedTexts.columnHeader_FailureCounter()).thenReturn(MOCKED_COLUMN_HEADER_FAILED);
        when(mockedTexts.columnHeader_IgnoredCounter()).thenReturn(MOCKED_COLUMN_HEADER_IGNORED);
        when(mockedTexts.columnHeader_ProgressBar()).thenReturn(MOCKED_COLUMN_HEADER_PROGRESS);
        when(mockedTexts.columnHeader_JobStatus()).thenReturn(MOCKED_COLUMN_HEADER_JOB_STATUS);
        when(mockedTexts.columnHeader_Fixed()).thenReturn(MOCKED_COLUMN_HEADER_IS_FIXED);
        when(mockedTexts.columnHeader_Assignee()).thenReturn(MOCKED_COLUMN_HEADER_ASSIGNEE);
        when(mockedTexts.button_RerunJob()).thenReturn(MOCKED_BUTTON_RERUN_JOB);
        when(mockedTexts.label_ReRunJobNo()).thenReturn(MOCKED_LABEL_RERUN_JOB_NO);
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
        verify(viewConcrete.jobsTable, times(3)).addColumn(isA(Column.class), isA(View.HidableColumnHeader.class));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_JOB_CREATION_TIME));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_JOB_ID));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_SUBMITTER));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_FLOW_BINDER_NAME));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_SINK_NAME));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_ITEM_COUNTER));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_FAILED));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_IGNORED));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_PROGRESS));
        verify(viewConcrete.jobsTable).addColumn(isA(Column.class), eq(MOCKED_COLUMN_HEADER_JOB_STATUS));
        verify(viewConcrete.jobsTable).setSelectionModel(isA(SingleSelectionModel.class));
        verify(viewConcrete.jobsTable).addDomHandler(isA(DoubleClickHandler.class), isA(DomEvent.Type.class));
        verify(viewConcrete.jobsTable).setVisibleRange(0, 20);
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
    public void constructHideShowColumn_jobModelIsValidWithNullPreviousJobId_calculateCorrectHtmlSnippet() {
        view = new ViewConcrete();

        View.HideShowCell hideShowCell = (View.HideShowCell) view.constructHideShowWorkflow();
        when(mockedContext.getKey()).thenReturn(testModel1);
        testModel1.setPreviousJobIdAncestry(null);
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
        when(mockedContext.getKey()).thenReturn(testModel1);
        testModel1.setPreviousJobIdAncestry("0");
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
    public void constructHideShowColumn_jobModelIsValidWithEmptyPreviousJobId_calculateCorrectHtmlSnippet() {
        view = new ViewConcrete();

        View.HideShowCell hideShowCell = (View.HideShowCell) view.constructHideShowWorkflow();
        when(mockedContext.getKey()).thenReturn(testModel1);
        testModel1.setPreviousJobIdAncestry("");
        Cell<ImageResource> cell = hideShowCell.getCell();
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        when(mockedImageResource.getWidth()).thenReturn(11);
        when(mockedImageResource.getHeight()).thenReturn(22);
        when(mockedImageResource.getSafeUri()).thenReturn(() -> "[Test Image]");

        // Subject Under Test
        cell.render(mockedContext, mockedImageResource, safeHtmlBuilder);

        // Verify Test
        assertThat(safeHtmlBuilder.toSafeHtml().asString(), is("<span title='Mocked Label Rerun Job No '>image([Test Image], 11, 22)</span>"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructHideShowColumn_jobModelIsValidWithValidPreviousJobId_calculateCorrectHtmlSnippet() {
        view = new ViewConcrete();

        View.HideShowCell hideShowCell = (View.HideShowCell) view.constructHideShowWorkflow();
        when(mockedContext.getKey()).thenReturn(testModel1);
        testModel1.setPreviousJobIdAncestry("1234");
        Cell<ImageResource> cell = hideShowCell.getCell();
        SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder();
        when(mockedImageResource.getWidth()).thenReturn(11);
        when(mockedImageResource.getHeight()).thenReturn(22);
        when(mockedImageResource.getSafeUri()).thenReturn(() -> "[Test Image]");

        // Subject Under Test
        cell.render(mockedContext, mockedImageResource, safeHtmlBuilder);

        // Verify Test
        assertThat(safeHtmlBuilder.toSafeHtml().asString(), is("<span title='Mocked Label Rerun Job No 1234'>image([Test Image], 11, 22)</span>"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructIsFixedColumn_callWhileVisible_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructIsFixedColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getWorkflowNoteModel().isProcessed()));
        assertThat(column.getCellStyleNames(null, null), is("visible"));
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
        assertThat(column.getCellStyleNames(null, null), is("invisible"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructAssigneeColumn_callWhileVisible_correctlySetup() {
        view = new ViewConcrete();

        // Subject Under Test
        Column column = view.constructAssigneeColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(testModel1), is(testModel1.getWorkflowNoteModel().getAssignee()));
        assertThat(column.getCellStyleNames(null, null), is("visible"));
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
        assertThat(column.getCellStyleNames(null, null), is("invisible"));
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
        assertThat(column.getValue(testModel1), is(String.valueOf(testModel1.getFailedCounter())));

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
        assertThat(column.getValue(testModel1), is(String.valueOf(testModel1.getProcessingIgnoredCounter())));

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
        assertThat(column.getValue(null), is(MOCKED_BUTTON_RERUN_JOB));
        assertThat(column.getCellStyleNames(null, null), is("visible"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructRerunColumn_callWhileNotVisible_correctlySetup() {
        view = new ViewConcrete();
        view.HideColumn(true);

        // Subject Under Test
        Column column = view.constructRerunColumn();

        // Test that correct getValue handler has been setup
        assertThat(column.getValue(null), is(MOCKED_BUTTON_RERUN_JOB));
        assertThat(column.getCellStyleNames(null, null), is("invisible"));

        // Test that setFieldUpdater is setup correctly, by calling it and verify behaviour
        JobModel testModel = new JobModel();
        view.setPresenter(mockedPresenter);

        column.getFieldUpdater().update(0, testModel, "bla");
        verify(mockedPresenter).editJob(testModel);
        verifyNoMoreInteractions(mockedPresenter);
    }

}