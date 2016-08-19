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
    final static String MOCKED_LABEL_JOBS = "Mocked Text: label_Jobs";
    final static String MOCKED_LABEL_JOBID = "Mocked Text: label_JobId";
    final static String MOCKED_LABEL_RERUNJOBNO = "Mocked Text: label_RerunJobNo";
    final static String MOCKED_LABEL_RERUNJOB = "Mocked Text: label_RerunJob";
    final static String MOCKED_LABEL_RERUNJOBS = "Mocked Text: label_RerunJobs - count = @COUNT@";
    final static String MOCKED_LABEL_RERUNCONFIRMATIONCAPTION = "Mocked Text: label_RerunConfirmationCaption";
    final static String MOCKED_LABEL_RERUNJOBCONFIRMATION = "Mocked Text: label_RerunJobConfirmation";
    final static String MOCKED_LABEL_RERUNJOBSCONFIRMATION = "Mocked Text: label_RerunJobsConfirmation";
    final static String MOCKED_COLUMNHEADER_JOBID = "Mocked Text: columnHeader_JobId";
    final static String MOCKED_COLUMNHEADER_SUBMITTER = "Mocked Text: columnHeader_Submitter";
    final static String MOCKED_COLUMNHEADER_FLOWBINDERNAME = "Mocked Text: columnHeader_FlowBinderName";
    final static String MOCKED_COLUMNHEADER_SINKNAME = "Mocked Text: columnHeader_SinkName";
    final static String MOCKED_COLUMNHEADER_TOTALCHUNKCOUNT = "Mocked Text: columnHeader_TotalChunkCount";
    final static String MOCKED_COLUMNHEADER_JOBCREATIONTIME = "Mocked Text: columnHeader_JobCreationTime";
    final static String MOCKED_COLUMNHEADER_FAILURECOUNTER = "Mocked Text: columnHeader_FailureCounter";
    final static String MOCKED_COLUMNHEADER_IGNOREDCOUNTER = "Mocked Text: columnHeader_IgnoredCounter";
    final static String MOCKED_COLUMNHEADER_PROGRESSBAR = "Mocked Text: columnHeader_ProgressBar";
    final static String MOCKED_COLUMNHEADER_JOBSTATUS = "Mocked Text: columnHeader_JobStatus";
    final static String MOCKED_COLUMNHEADER_ACTION = "Mocked Text: columnHeader_Action";
    final static String MOCKED_COLUMNHEADER_FIXED = "Mocked Text: columnHeader_Fixed";
    final static String MOCKED_COLUMNHEADER_ASSIGNEE = "Mocked Text: columnHeader_Assignee";
    final static String MOCKED_BUTTON_ALLJOBS = "Mocked Text: button_AllJobs";
    final static String MOCKED_BUTTON_PROCESSINGFAILEDJOBS = "Mocked Text: button_ProcessingFailedJobs";
    final static String MOCKED_BUTTON_DELIVERINGFAILEDJOBS = "Mocked Text: button_DeliveringFailedJobs";
    final static String MOCKED_BUTTON_FATALJOBS = "Mocked Text: button_FatalJobs";
    final static String MOCKED_BUTTON_REFRESH = "Mocked Text: button_Refresh";
    final static String MOCKED_BUTTON_SHOWJOB = "Mocked Text: button_ShowJob";
    final static String MOCKED_BUTTON_RERUNJOB = "Mocked Text: button_RerunJob";
    final static String MOCKED_BUTTON_RERUNALLSHOWNJOBS = "Mocked Text: button_RerunAllShownJobs";
    final static String MOCKED_BUTTON_RERUNOK = "Mocked Text: button_RerunOk";
    final static String MOCKED_BUTTON_RERUNCANCEL = "Mocked Text: button_RerunCancel";
    final static String MOCKED_ERROR_INPUTFIELDVALIDATIONERROR = "Mocked Text: error_InputFieldValidationError";
    final static String MOCKED_ERROR_NUMERICINPUTFIELDVALIDATIONERROR = "Mocked Text: error_NumericInputFieldValidationError";
    final static String MOCKED_ERROR_JOBNOTFOUND = "Mocked Text: error_JobNotFound";
    final static String MOCKED_ERROR_INPUTCELLVALIDATIONERROR = "Mocked Text: error_InputCellValidationError";
    final static String MOCKED_ERROR_CHECKBOXCELLVALIDATIONERROR = "Mocked Text: error_CheckboxCellValidationError";
    final static String MOCKED_ERROR_NOJOBSTORERUN = "Mocked Text: error_NoJobsToRerun";

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

        when(mockedTexts.label_Jobs()).thenReturn(MOCKED_LABEL_JOBS);
        when(mockedTexts.label_JobId()).thenReturn(MOCKED_LABEL_JOBID);
        when(mockedTexts.label_RerunJobNo()).thenReturn(MOCKED_LABEL_RERUNJOBNO);
        when(mockedTexts.label_RerunJob()).thenReturn(MOCKED_LABEL_RERUNJOB);
        when(mockedTexts.label_RerunJobs()).thenReturn(MOCKED_LABEL_RERUNJOBS);
        when(mockedTexts.label_RerunConfirmationCaption()).thenReturn(MOCKED_LABEL_RERUNCONFIRMATIONCAPTION);
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
        when(mockedTexts.columnHeader_Action()).thenReturn(MOCKED_COLUMNHEADER_ACTION);
        when(mockedTexts.columnHeader_Fixed()).thenReturn(MOCKED_COLUMNHEADER_FIXED);
        when(mockedTexts.columnHeader_Assignee()).thenReturn(MOCKED_COLUMNHEADER_ASSIGNEE);
        when(mockedTexts.button_AllJobs()).thenReturn(MOCKED_BUTTON_ALLJOBS);
        when(mockedTexts.button_ProcessingFailedJobs()).thenReturn(MOCKED_BUTTON_PROCESSINGFAILEDJOBS);
        when(mockedTexts.button_DeliveringFailedJobs()).thenReturn(MOCKED_BUTTON_DELIVERINGFAILEDJOBS);
        when(mockedTexts.button_FatalJobs()).thenReturn(MOCKED_BUTTON_FATALJOBS);
        when(mockedTexts.button_Refresh()).thenReturn(MOCKED_BUTTON_REFRESH);
        when(mockedTexts.button_ShowJob()).thenReturn(MOCKED_BUTTON_SHOWJOB);
        when(mockedTexts.button_RerunJob()).thenReturn(MOCKED_BUTTON_RERUNJOB);
        when(mockedTexts.button_RerunAllShownJobs()).thenReturn(MOCKED_BUTTON_RERUNALLSHOWNJOBS);
        when(mockedTexts.button_RerunOk()).thenReturn(MOCKED_BUTTON_RERUNOK);
        when(mockedTexts.button_RerunCancel()).thenReturn(MOCKED_BUTTON_RERUNCANCEL);
        when(mockedTexts.error_InputFieldValidationError()).thenReturn(MOCKED_ERROR_INPUTFIELDVALIDATIONERROR);
        when(mockedTexts.error_NumericInputFieldValidationError()).thenReturn(MOCKED_ERROR_NUMERICINPUTFIELDVALIDATIONERROR);
        when(mockedTexts.error_JobNotFound()).thenReturn(MOCKED_ERROR_JOBNOTFOUND);
        when(mockedTexts.error_InputCellValidationError()).thenReturn(MOCKED_ERROR_INPUTCELLVALIDATIONERROR);
        when(mockedTexts.error_CheckboxCellValidationError()).thenReturn(MOCKED_ERROR_CHECKBOXCELLVALIDATIONERROR);
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
        verify(viewConcrete.jobsTable, times(3)).addColumn(isA(Column.class), isA(View.HidableColumnHeader.class));
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
        when(view.jobsTable.getVisibleItemCount()).thenReturn(0);

        // Subject Under Test
        view.rerunAllShownJobs();

        // Verify test
        verify(view.jobsTable).getVisibleItemCount();
        verifyDialogBoxOperations("", MOCKED_ERROR_NOJOBSTORERUN, "", false);
        verifyNoMoreInteractions(view.jobsTable);
    }

    @Test
    public void rerunAllShownJobs_countIsOne_oneJobToRerunDialogBox() {
        // Subject Under Test
        view = new ViewConcrete();
        when(view.jobsTable.getVisibleItemCount()).thenReturn(1);
        when(view.jobsTable.getVisibleItem(0)).thenReturn(new JobModelBuilder().setJobId("124").build());

        // Subject Under Test
        view.rerunAllShownJobs();

        // Verify test
        verify(view.jobsTable, times(2)).getVisibleItemCount();
        verifyDialogBoxOperations(MOCKED_LABEL_RERUNJOB, "124", MOCKED_LABEL_RERUNJOBCONFIRMATION, true);
        verify(view.jobsTable).getVisibleItem(0);
        verifyNoMoreInteractions(view.jobsTable);
    }

    @Test
    public void rerunAllShownJobs_countIsThree_threeJobsToRerunDialogBox() {
        // Subject Under Test
        view = new ViewConcrete();
        when(view.jobsTable.getVisibleItemCount()).thenReturn(3);
        when(view.jobsTable.getVisibleItem(0)).thenReturn(new JobModelBuilder().setJobId("635").build());
        when(view.jobsTable.getVisibleItem(1)).thenReturn(new JobModelBuilder().setJobId("124").build());
        when(view.jobsTable.getVisibleItem(2)).thenReturn(new JobModelBuilder().setJobId("784").build());

        // Subject Under Test
        view.rerunAllShownJobs();

        // Verify test
        verify(view.jobsTable, times(2)).getVisibleItemCount();
        verifyDialogBoxOperations("Mocked Text: label_RerunJobs - count = 3", "124, 635, 784", MOCKED_LABEL_RERUNJOBSCONFIRMATION, true);
        verify(view.jobsTable).getVisibleItem(0);
        verify(view.jobsTable).getVisibleItem(1);
        verify(view.jobsTable).getVisibleItem(2);
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
        assertThat(safeHtmlBuilder.toSafeHtml().asString(), is("<span title='Mocked Text: label_RerunJobNo '>image([Test Image], 11, 22)</span>"));
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
        assertThat(column.getValue(null), is(MOCKED_BUTTON_RERUNJOB));
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
        assertThat(column.getValue(null), is(MOCKED_BUTTON_RERUNJOB));
        assertThat(column.getCellStyleNames(null, null), is("invisible"));

        // Test that setFieldUpdater is setup correctly, by calling it and verify behaviour
        JobModel testModel = new JobModel();
        view.setPresenter(mockedPresenter);

        column.getFieldUpdater().update(0, testModel, "bla");
        verify(mockedPresenter).editJob(testModel);
        verifyNoMoreInteractions(mockedPresenter);
    }


    /*
     * Private methods
     */

    private void verifyDialogBoxOperations(String count, String list, String confirmation, Boolean hideOkButton) {
        verify(view.rerunJobsCount).setText(count);
        verify(view.rerunJobsList).setText(list);
        verify(view.rerunJobsConfirmation).setText(confirmation);
        verify(view.rerunOkButton).setVisible(hideOkButton);
        verify(view.rerunAllShownJobsConfirmationDialog, times(2)).show();
    }

}