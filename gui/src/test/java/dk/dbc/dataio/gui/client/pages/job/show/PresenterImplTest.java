
package dk.dbc.dataio.gui.client.pages.job.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest {
    @Mock ClientFactory mockedClientFactory;
    @Mock PlaceController mockedPlaceController;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;
    @Mock RadioButton mockedAllJobsButton;
    @Mock RadioButton mockedProcessingFailedJobsButton;
    @Mock RadioButton mockedDeliveringFailedJobsButton;
    @Mock SingleSelectionModel<JobModel> mockedSingleSelectionModel;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getJobsShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);

        mockedView.allJobsButton = mockedAllJobsButton;
        mockedView.processingFailedJobsButton = mockedProcessingFailedJobsButton;
        mockedView.deliveringFailedJobsButton = mockedDeliveringFailedJobsButton;
        mockedView.selectionModel = mockedSingleSelectionModel;
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
        }
        @Override
        protected void fetchJobsFromJobStore(JobListCriteriaModel model) {
        }
        public FetchJobsCallback fetchJobsCallback = new FetchJobsCallback();
    }


    // Test Data
    private JobModel testModel1 = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014.5000_records.xml3473603508877630498.tmp", "150014", "SubmitterName",
            "FlowBinderName", 6789L, "SinkName",
            true, 20, 20, 5, 5, 31, 32, 33,
            "packagingA", "formatA", "charsetA", "destinationA", "mailNotificationA", "mailProcessingA", "resultMailInitialsA");

    private JobModel testModel2 = new JobModel("2014-12-17 00:37:48", "1418773068083",
            "urn:dataio-fs:46551", "424242", "SubmitterName", "FlowBinderName", 6789L, "SinkName",
            true, 10, 10, 0, 5, 34, 35, 36,
            "packagingB", "formatB", "charsetB", "destinationB", "mailNotificationB", "mailProcessingB", "resultMailInitialsB");
    private List<JobModel> testModels = new ArrayList<JobModel>(Arrays.asList(testModel1, testModel2));

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        // Verify Test
        verify(mockedClientFactory).getJobStoreProxyAsync();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedClientFactory).getJobsShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterJobs_processingFailedSelected_jobsFailedInProcessingRequested() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(true);

        // Subject under test
        presenterImpl.fetchSelectedJobs();

        // Verify Test
        verify(mockedView.selectionModel).clear();
        verify(mockedProcessingFailedJobsButton, times(2)).getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterJobs_deliveringFailedSelected_jobsFailedInDeliveringRequested() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(false);
        when(mockedDeliveringFailedJobsButton.getValue()).thenReturn(true);

        // Subject under test
        presenterImpl.fetchSelectedJobs();

        // Verify Test
        verify(mockedView.selectionModel).clear();
        verify(mockedProcessingFailedJobsButton, times(2)).getValue();
        verify(mockedDeliveringFailedJobsButton, times(2)).getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterJobs_allJobsSelected_allJobsRequested() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProcessingFailedJobsButton.getValue()).thenReturn(false);
        when(mockedDeliveringFailedJobsButton.getValue()).thenReturn(false);

        // Subject under test
        presenterImpl.fetchSelectedJobs();

        // Verify Test
        verify(mockedView.selectionModel).clear();
        verify(mockedProcessingFailedJobsButton, times(2)).getValue();
        verify(mockedDeliveringFailedJobsButton, times(2)).getValue();
        verifyZeroInteractions(mockedAllJobsButton);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void fetchJobs_jobSelected_viewNotRepopulated() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedView.selectionModel.getSelectedObject()).thenReturn(new JobModel());

        // Subject under test
        presenterImpl.fetchJobs();

        // Verify Test
        // Verify only one invocation during call to start
        verify(mockedProcessingFailedJobsButton).getValue();
        verify(mockedDeliveringFailedJobsButton).getValue();
    }

    @Test
    public void fetchJob_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchJobsCallback.onFilteredFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(any(String.class));
    }

    @Test
    public void fetchJob_callbackWithSuccess_jobsAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchJobsCallback.onSuccess(testModels);

        // Verify Test
        verify(mockedView).setJobs(testModels);
    }

}
