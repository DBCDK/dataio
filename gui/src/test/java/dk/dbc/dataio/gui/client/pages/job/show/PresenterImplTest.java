
package dk.dbc.dataio.gui.client.pages.job.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.pages.faileditems.ShowPlace;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
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
    @Mock JobStoreProxyAsync mockedJobStore;
    @Mock PlaceController mockedPlaceController;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getJobStoreProxyAsync()).thenReturn(mockedJobStore);
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getJobsShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
        }
        public FetchJobsCallback fetchJobsCallback = new FetchJobsCallback();
        public GetJobstoreLinkCallback getJobstoreLinkCallback = new GetJobstoreLinkCallback();
    }

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



    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        presenterImpl = new PresenterImpl(mockedClientFactory);

        // Verify Test
        verify(mockedClientFactory).getJobStoreProxyAsync();
        verify(mockedClientFactory).getPlaceController();
    }

    @Test
    public void start_callStart_ok() {
        presenterImpl = new PresenterImpl(mockedClientFactory);

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedClientFactory).getJobsShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedJobStore).getJobStoreFilesystemUrl(any(AsyncCallback.class));
        verify(mockedJobStore).findAllJobsNew(any(AsyncCallback.class));
    }

    @Test
    public void showFailedItems_call_gotoShowPlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.showFailedItems("1234", JobState.OperationalState.PROCESSING, ItemCompletionState.State.SUCCESS);

        // Verify Test
        verify(mockedPlaceController).goTo(any(ShowPlace.class));
    }

    @Test
    public void showMoreInformation_call_openNewUrl() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.showMoreInformation("1234");

        // Verify Test
        // Here I would have liked to verify, that a Window.open call is being made, but this is not possible
        // However, this test verifies, that no exception is being thrown
    }

    @Test
    public void getJobstoreLink_callWithId_returnsUrl() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.jobStoreFilesystemUrl = "http://base.url.dk";

        // Test Subject Under Test
        String url = presenterImpl.getJobstoreLink("1234567");

        // Verify Test
        assertThat(url, is("http://base.url.dk/1234567"));
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

    @Test
    public void getJobStoreFilesystemUrl_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobstoreLinkCallback.onFilteredFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(any(String.class));
    }

    @Test
    public void getJobStoreFilesystemUrl_callbackWithSuccess_jobStoreFilesystemUrlIsSet() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        assertThat(presenterImpl.jobStoreFilesystemUrl, is(""));

        // Test Subject Under Test
        presenterImpl.getJobstoreLinkCallback.onSuccess("http://job.store.url.dk");

        // Verify Test
        assertThat(presenterImpl.jobStoreFilesystemUrl, is("http://job.store.url.dk"));
    }

}
