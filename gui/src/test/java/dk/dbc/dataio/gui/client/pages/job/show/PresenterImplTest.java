package dk.dbc.dataio.gui.client.pages.job.show;


import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.jobfilter.JobFilter;
import dk.dbc.dataio.gui.client.components.log.LogPanel;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.components.popup.PopupSelectBox;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.StateModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.modelBuilders.WorkflowNoteModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JobRerunProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.jobstore.types.StateElement;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock
    private JobStoreProxyAsync mockedJobStore;
    @Mock
    private View mockedView;
    @Mock
    private JobFilter mockedJobFilter;
    @Mock
    private AbstractBasePlace mockedPlace;
    @Mock
    private ViewJobsGinjector mockedViewInjector;
    @Mock
    private Throwable mockedException;
    @Mock
    private SingleSelectionModel<JobModel> mockedSingleSelectionModel;
    @Mock
    private AsyncJobViewDataProvider mockedAsyncJobViewDataProvider;
    @Mock
    private CellTable mockedJobsTable;
    @Mock
    private TextBox mockedJobIdInputField;
    @Mock
    private PopupListBox changeColorSchemeListBox;
    @Mock
    private ContentPanel mockedContentPanel;
    @Mock
    private LogPanel mockedLogPanel;
    @Mock
    private Element mockedElement;
    @Mock
    private FlowStoreProxyAsync mockedFlowStore;
    @Mock
    private JobRerunProxyAsync mockedJobRerunProxyAsync;
    @Mock
    private PopupSelectBox mockedPopupSelectedBox;
    @Mock
    private Throwable mockedThrowable;
    @Mock
    private PushButton mockedLogButton;
    // Mocked Texts
    @Mock
    private Texts mockedText;
    private final static String MOCKED_INPUT_FIELD_VALIDATION_ERROR = "mocked error_InputFieldValidationError";
    private final static String MOCKED_NUMERIC_INPUT_FIELD_VALIDATION_ERROR = "mocked error_InputFieldValidationError";
    private final static String MOCKED_JOB_NOT_FOUND_ERROR = "mocked error_JobNotFound()";
    private final Map<String, String> testParameters = new HashMap<>();

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getJobStoreProxyAsync()).thenReturn(mockedJobStore);
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getJobRerunProxyAsync()).thenReturn(mockedJobRerunProxyAsync);
        when(mockedViewInjector.getView()).thenReturn(mockedView);
        when(mockedView.getTexts()).thenReturn(mockedText);
        when(mockedViewInjector.getTexts()).thenReturn(mockedText);
        mockedView.selectionModel = mockedSingleSelectionModel;
        mockedView.dataProvider = mockedAsyncJobViewDataProvider;
        mockedView.jobsTable = mockedJobsTable;
        mockedView.jobIdInputField = mockedJobIdInputField;
        mockedView.jobFilter = mockedJobFilter;
        mockedView.popupSelectBox = mockedPopupSelectedBox;
        mockedView.changeColorSchemeListBox = changeColorSchemeListBox;
        mockedView.logButton = mockedLogButton;
        when(mockedPlaceController.getWhere()).thenReturn(mockedPlace);
        when(mockedPlace.getParameters()).thenReturn(testParameters);
        when(mockedText.error_InputFieldValidationError()).thenReturn(MOCKED_INPUT_FIELD_VALIDATION_ERROR);
        when(mockedText.error_NumericInputFieldValidationError()).thenReturn(MOCKED_NUMERIC_INPUT_FIELD_VALIDATION_ERROR);
        when(mockedText.error_JobNotFound()).thenReturn(MOCKED_JOB_NOT_FOUND_ERROR);
        when(Document.get().getElementById(eq(ContentPanel.GUIID_CONTENT_PANEL))).thenReturn(mockedElement);
        when(mockedElement.getPropertyObject(eq(ContentPanel.GUIID_CONTENT_PANEL))).thenReturn(mockedContentPanel);
        when(mockedContentPanel.getLogPanel()).thenReturn(mockedLogPanel);
    }

    // Subject Under Test
    private PresenterImplConcrete presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        CountExistingJobsWithJobIdCallBack getJobCountCallback;
        SetWorkflowNoteCallBack getWorkflowNoteCallback;

        public PresenterImplConcrete(PlaceController placeController, String header) {
            super(placeController, mockedView, header);
            this.commonInjector = mockedCommonGinjector;
            this.getJobCountCallback = new CountExistingJobsWithJobIdCallBack();
            this.getWorkflowNoteCallback = new SetWorkflowNoteCallBack();
        }

        CreateJobRerunAsyncCallback setCreateJobRerunAsyncCallback(String jobId, boolean failedItemsOnly) {
            return new CreateJobRerunAsyncCallback(jobId, failedItemsOnly);
        }

        ReSubmitJobFilteredAsyncCallback setReSubmitJobFilteredAsyncCallback(String jobId) {
            return new ReSubmitJobFilteredAsyncCallback(jobId);
        }


        public void setIsMultipleRerun(boolean isMultipleRerun) {
            this.isMultipleRerun = isMultipleRerun;
        }

        @Override
        protected void updateBaseQuery() {

            JobListCriteria criteria = new JobListCriteria()
                    .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                    .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"))
                    .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"INFOMEDIA\"}"));

            view.dataProvider.setBaseCriteria(criteria);
        }
    }

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        setupPresenter();

    }

    @Test
    public void start_callStart_ok() {

        // Setup
        setupPresenter();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
    }

    @Test
    public void filterJobs_updateSelectedJobs() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.updateSelectedJobs();

        // Verify Test
        verify(mockedAsyncJobViewDataProvider).updateCurrentCriteria();
        verify(mockedAsyncJobViewDataProvider).setBaseCriteria(any(JobListCriteria.class));
    }

    @Test
    public void showJob_jobIdInputFieldIsEmpty_errorMessageInView() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedJobIdInputField.getValue()).thenReturn("");

        // Subject under test
        presenterImpl.showJob();

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_INPUT_FIELD_VALIDATION_ERROR);
        verify(mockedCommonGinjector.getJobStoreProxyAsync(), times(0)).countJobs(any(JobListCriteria.class), any(PresenterImpl.CountExistingJobsWithJobIdCallBack.class));
    }

    @Test
    public void showJob_jobIdInputFieldContainsNoneNumericValue_errorMessageInView() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedJobIdInputField.getValue()).thenReturn("test123");

        // Subject under test
        presenterImpl.showJob();

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_NUMERIC_INPUT_FIELD_VALIDATION_ERROR);
        verify(mockedCommonGinjector.getJobStoreProxyAsync(), times(0)).countJobs(any(JobListCriteria.class), any(PresenterImpl.CountExistingJobsWithJobIdCallBack.class));
    }

    @Test
    public void showJob_jobIdInputFieldContainsValidJobId_countJobsCalled() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedJobIdInputField.getValue()).thenReturn("140");

        // Subject under test
        presenterImpl.showJob();

        // Verify Test
        verify(mockedCommonGinjector.getJobStoreProxyAsync()).countJobs(any(JobListCriteria.class), any(PresenterImpl.CountExistingJobsWithJobIdCallBack.class));
    }

    @Test
    public void showJob_callbackWithError_errorMessageInView() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCountCallback.onFailure(mockedException);

        // Verify Test
        verifyNoInteractions(mockedView.jobIdInputField);
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void showJob_callbackWithSuccess_JobNotFound() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCountCallback.onSuccess(0L);

        // Verify Test
        verifyNoInteractions(mockedView.jobIdInputField);
        verify(mockedView).setErrorText(MOCKED_JOB_NOT_FOUND_ERROR);
    }

    @Test
    public void showJob_callbackWithSuccess_jobFound() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCountCallback.onSuccess(1L);

        // Verify Test
        verify(mockedView.jobIdInputField).setText("");
        verify(mockedPlaceController).goTo(any(dk.dbc.dataio.gui.client.pages.item.show.Place.class));
    }

    @Test
    public void setWorkflowNote_inputIsValid_setWorkflowNoteCalled() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        WorkflowNoteModel workflowNoteModel = new WorkflowNoteModelBuilder().build();
        presenterImpl.setWorkflowNote(workflowNoteModel, "1");

        // Verify Test
        verify(mockedCommonGinjector.getJobStoreProxyAsync()).setWorkflowNote(any(WorkflowNoteModel.class), anyInt(), any(PresenterImpl.SetWorkflowNoteCallBack.class));
    }

    @Test
    public void setWorkflowNote_callbackWithError_errorMessageInView() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getWorkflowNoteCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void setWorkflowNote_callbackWithSuccess_selectionModelUpdated() {

        // Setup
        setupPresenter();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        final JobModel jobModel = new JobModel().withWorkflowNoteModel(new WorkflowNoteModelBuilder().setAssignee("assignee").build());
        when(mockedSingleSelectionModel.getSelectedObject()).thenReturn(jobModel);

        // Test Subject Under Test
        presenterImpl.getWorkflowNoteCallback.onSuccess(mockedSingleSelectionModel.getSelectedObject());

        // Verify Test
        verify(mockedView.selectionModel).setSelected(jobModel, Boolean.TRUE);
    }

    @Test
    public void preProcessAssignee_assigneeIsEmptyValue_errorMessageInView() {

        // Setup
        setupPresenter();

        // Subject under test
        presenterImpl.preProcessAssignee(null, "");

        // Verify Test
        verify(mockedView).setErrorText(isNull());
    }

    @Test
    public void preProcessAssignee_assigneeIsValidAndWorkflowNoteIsNotNull_returnsWorkflowNoteModel() {

        // Setup
        setupPresenter();
        final String assignee = "assignee";

        final WorkflowNoteModel existingWorkflowNoteModel = new WorkflowNoteModelBuilder()
                .setAssignee("old")
                .setDescription("testDescription")
                .setProcessed(false)
                .build();

        // Subject under test
        WorkflowNoteModel updatedWorkflowNoteModel = presenterImpl.preProcessAssignee(existingWorkflowNoteModel, assignee);

        // Verify Test
        assertThat(updatedWorkflowNoteModel, is(notNullValue()));
        assertThat(updatedWorkflowNoteModel.getAssignee(), is(assignee));
        assertThat(updatedWorkflowNoteModel.getDescription(), is(existingWorkflowNoteModel.getDescription()));
        assertThat(updatedWorkflowNoteModel.isProcessed(), is(existingWorkflowNoteModel.isProcessed()));
    }

    @Test
    public void preProcessAssignee_assigneeIsValidAndWorkflowNoteIsNull_returnsWorkflowNoteModel() {

        // Setup
        setupPresenter();
        final String assignee = "assignee";

        final WorkflowNoteModel expectedWorkflowNoteModel = new WorkflowNoteModelBuilder()
                .setAssignee(assignee)
                .setDescription("")
                .setProcessed(false)
                .build();

        // Subject under test
        WorkflowNoteModel updatedWorkflowNoteModel = presenterImpl.preProcessAssignee(null, assignee);

        // Verify Test
        assertThat(updatedWorkflowNoteModel, is(expectedWorkflowNoteModel));
    }


    @Test
    public void rerunJobs_twoJobs_ok() {
        setupPresenter();
        presenterImpl.isMultipleRerun = true;

        // Subject under test
        List<JobModel> jobModelList = Arrays.asList(new JobModel().withJobId("1"), new JobModel().withJobId("2"));
        presenterImpl.rerunMultiple(jobModelList);

        // Verification
        verify(mockedJobRerunProxyAsync, times(2)).parse(any(JobModel.class), any(PresenterImpl.GetJobRerunSchemeFilteredAsyncCallback.class));
        verify(mockedLogPanel, times(1)).clear();
    }

    @Test
    public void rerunJobs_singleJobWithNoFailedButFailedOnlySelected_logMessageSet() {
        setupPresenter();
        presenterImpl.jobId = "42";
        final JobModel existingJobModel = new JobModel().withJobId("1")
                .withStateModel(new StateModel().withPartitioning(new StateElement().withSucceeded(10))).withNumberOfItems(10).withNumberOfChunks(1);

        // Subject under test
        presenterImpl.rerunMultiple(Collections.singletonList(existingJobModel));

        // Verification
        verifyNoInteractions(mockedFlowStore);
        verifyNoInteractions(mockedJobStore);
        verify(mockedLogPanel).clear();
    }

    @Test
    public void rerunJobs_noJobs_ok() {
        setupPresenter();

        // Subject under test
        presenterImpl.rerunMultiple(Collections.emptyList());

        // Verification
        verifyNoInteractions(mockedJobStore);
        verifyNoInteractions(mockedFlowStore);
    }

    @Test(expected = NullPointerException.class)
    public void rerunJobs_nullJobs_throws() {
        setupPresenter();

        // Subject under test
        presenterImpl.rerunMultiple(null);
    }

    @Test
    public void ReSubmitJobFilteredAsyncCallback_onFilteredFailure_logMessageSet() {
        setupPresenter();
        final PresenterImpl.ReSubmitJobFilteredAsyncCallback reSubmitJobFilteredAsyncCallback = presenterImpl.setReSubmitJobFilteredAsyncCallback("1");

        // Subject under test
        reSubmitJobFilteredAsyncCallback.onFilteredFailure(mockedThrowable);

        // Verification
        verify(mockedLogPanel).showMessage(isNull());
        verify(mockedThrowable).getMessage();
    }

    @Test
    public void CreateJobRerunAsyncCallback_onFailure_logMessageSet() {
        setupPresenter();
        final PresenterImpl.CreateJobRerunAsyncCallback createJobRerunAsyncCallback = presenterImpl.setCreateJobRerunAsyncCallback("1", false);

        // Subject under test
        createJobRerunAsyncCallback.onFailure(mockedThrowable);

        // Verification
        verify(mockedLogPanel).showMessage(isNull());
        verify(mockedThrowable).getMessage();
    }


    /*
     * Private methods
     */

    private void setupPresenter() {
        presenterImpl = new PresenterImplConcrete(mockedPlaceController, header);
        presenterImpl.commonInjector = mockedCommonGinjector;
        presenterImpl.logPanel = mockedLogPanel;
    }

}
