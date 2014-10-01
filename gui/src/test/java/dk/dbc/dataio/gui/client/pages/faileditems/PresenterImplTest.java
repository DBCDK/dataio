
package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.utils.test.model.ChunkCompletionStateBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobCompletionStateBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobInfoBuilder;
import dk.dbc.dataio.gui.client.pages.javascriptlog.JavaScriptLogPlace;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
    private ClientFactory mockedClientFactory;
    private Texts mockedTexts;
    private ShowPlace mockedPlace;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedView;
    private PlaceController mockedPlaceController;
    private JobStoreProxyAsync mockedJobStoreProxy;
    private Widget mockedWidget;

    private PresenterImplConcrete presenterImpl;

    // Test data
    private final String STATUS_IGNORED = "Ignore Status";
    private final String STATUS_SUCCESS = "Success Status";
    private final String STATUS_FAILURE = "Failure Status";
    private final String STATUS_INCOMPLETE = "Incomplete Status";
    private final String ERROR_COULD_NOT_FETCH_JOBS = "Could not fetch jobs";
    private final String ERROR_COULD_NOT_FETCH_JOB_COMPLETIONS_STATUS_FOR = "Could not fetch job completion status for: ";

    private final long JOB_ID = 1456L;
    private final long CHUNK_ID = 848L;
    private final long ITEM_ID = 44L;
    private final long ITEM_SUCCESS = 111L;
    private final long ITEM_FAILURE = 222L;
    private final long ITEM_IGNORED = 333L;
    private final long ITEM_INCOMPLETE = 444L;
    private final String FAILED_ITEM = "Failed Item";
    private FailedItemModel defaultFailedItemModel =
            new FailedItemModel(
                    Long.toString(JOB_ID),
                    Long.toString(CHUNK_ID),
                    Long.toString(ITEM_ID),
                    FAILED_ITEM);
    private final JobCompletionState jobCompletionState =
            new JobCompletionStateBuilder()
                    .setJobId(JOB_ID)
                    .addChunk(
                            new ChunkCompletionStateBuilder()
                                    .setChunkId(CHUNK_ID)
                                    .addItem(new ItemCompletionState(ITEM_SUCCESS, ItemCompletionState.State.SUCCESS))
                                    .addItem(new ItemCompletionState(ITEM_FAILURE, ItemCompletionState.State.FAILURE))
                                    .addItem(new ItemCompletionState(ITEM_IGNORED, ItemCompletionState.State.IGNORED))
                                    .addItem(new ItemCompletionState(ITEM_INCOMPLETE, ItemCompletionState.State.INCOMPLETE))
                                    .build())
                    .build();
    final long JOB_ID_EXTRA = 11L;
    List<JobInfo> jobInfos;

    //------------------------------------------------------------------------------------------------------------------

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(Place place, ClientFactory clientFactory, Texts texts) {
            super(place, clientFactory, texts);
        }
        public GetAllFailedItemsCallback getAllFailedItemsCallback = new GetAllFailedItemsCallback();
        public GetJobCompletionStatusCallback getJobCompletionStatusCallback = new GetJobCompletionStatusCallback();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        mockedClientFactory = mock(ClientFactory.class);
        mockedTexts = mock(Texts.class);
        mockedPlace = mock(ShowPlace.class);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedView = mock(View.class);
        mockedPlaceController = mock(PlaceController.class);
        mockedJobStoreProxy = mock(JobStoreProxyAsync.class);
        mockedWidget = mock(Widget.class);
        when(mockedPlace.getjobId()).thenReturn(JOB_ID);
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        when(mockedClientFactory.getFaileditemsView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedWidget);
        when(mockedTexts.status_ignored()).thenReturn(STATUS_IGNORED);
        when(mockedTexts.status_success()).thenReturn(STATUS_SUCCESS);
        when(mockedTexts.status_failure()).thenReturn(STATUS_FAILURE);
        when(mockedTexts.status_incomplete()).thenReturn(STATUS_INCOMPLETE);
        when(mockedTexts.error_CouldNotFetchJobs()).thenReturn(ERROR_COULD_NOT_FETCH_JOBS);
        when(mockedTexts.error_CouldNotFetchJobCompletionStatusFor()).thenReturn(ERROR_COULD_NOT_FETCH_JOB_COMPLETIONS_STATUS_FOR);
        jobInfos = new ArrayList<JobInfo>();
        jobInfos.add(new JobInfoBuilder().setJobId(JOB_ID_EXTRA).build());  // This is NOT the job
        jobInfos.add(new JobInfoBuilder().setJobId(JOB_ID).build());    // This is the job
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        PresenterImpl presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedTexts);

        assertThat(presenterImpl.jobId, is(JOB_ID));
        assertThat(presenterImpl.clientFactory, is(mockedClientFactory));
        assertThat(presenterImpl.texts, is(mockedTexts));
    }

    @Test
    public void start_callStart_objectStartedAndInitialized() {
        PresenterImpl presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedTexts);

        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        assertThat(presenterImpl.view, is(mockedView));
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedWidget);
        verify(mockedJobStoreProxy).findAllJobs(isA(AsyncCallback.class));
    }

    @Test
    public void failedItemSelected_callFailedItemSelected_placeControllerCalled() {
        createAndInitializePresenter();

        presenterImpl.failedItemSelected(defaultFailedItemModel);

        verify(mockedPlaceController).goTo(isA(JavaScriptLogPlace.class));
    }

    @Test
    public void getAllFailedItems_callGetAllFailedItems_proxyCalled() {
        PresenterImpl presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedTexts);

        presenterImpl.getAllFailedItems();

        verify(mockedJobStoreProxy).findAllJobs(isA(PresenterImpl.GetAllFailedItemsCallback.class));
    }

    @Test
    public void constructJavaScriptLogPlace_callConstructJavaScriptLogPlace_placeConstructed() {
        createAndInitializePresenter();

        JavaScriptLogPlace place = presenterImpl.constructJavaScriptLogPlace(defaultFailedItemModel);

        assertThat(place.getJobId(), is(JOB_ID));
        assertThat(place.getChunkId(), is(CHUNK_ID));
        assertThat(place.getFailedItemId(), is(ITEM_ID));
    }

    @Test
    public void getAllJobCompletionStatusAndGetJobCompletionStatus_callGetAllJobCompletionStatus_proxyCalled() {
        createAndInitializePresenter();

        presenterImpl.getAllJobCompletionStatus(jobInfos);

        verify(mockedView).clearFailedItemsList();
        verify(mockedJobStoreProxy).getJobCompletionState(eq(JOB_ID), isA(PresenterImpl.GetJobCompletionStatusCallback.class));
    }

    @Test
    public void addJobCompletionStateToView_calladdJobCompletionStateToView_viewAddFailedItemCalled() {
        createAndInitializePresenter();

        presenterImpl.addJobCompletionStateToView(jobCompletionState);

        ArgumentCaptor<FailedItemModel> argument = ArgumentCaptor.forClass(FailedItemModel.class);
        verify(mockedView, times(3)).addFailedItem(argument.capture());
        // We don't know the order of the captured arguments - therefore copy them into a map
        Map<String, FailedItemModel> modelMap = new HashMap<String, FailedItemModel>(3);
        for (FailedItemModel model: argument.getAllValues()) {
            modelMap.put(model.getFailedItem(), model);
        }
        assertThat(modelMap.size(), is(3));
        FailedItemModel ignoredModel = modelMap.get(STATUS_IGNORED);
        assertThat(ignoredModel.getJobId(), is(Long.toString(JOB_ID)));
        assertThat(ignoredModel.getChunkId(), is(Long.toString(CHUNK_ID)));
        assertThat(ignoredModel.getItemId(), is(Long.toString(ITEM_IGNORED)));
        assertThat(ignoredModel.getFailedItem(), is(STATUS_IGNORED));
        FailedItemModel failedModel = modelMap.get(STATUS_FAILURE);
        assertThat(failedModel.getJobId(), is(Long.toString(JOB_ID)));
        assertThat(failedModel.getChunkId(), is(Long.toString(CHUNK_ID)));
        assertThat(failedModel.getItemId(), is(Long.toString(ITEM_FAILURE)));
        assertThat(failedModel.getFailedItem(), is(STATUS_FAILURE));
        FailedItemModel incompletedModel = modelMap.get(STATUS_INCOMPLETE);
        assertThat(incompletedModel.getJobId(), is(Long.toString(JOB_ID)));
        assertThat(incompletedModel.getChunkId(), is(Long.toString(CHUNK_ID)));
        assertThat(incompletedModel.getItemId(), is(Long.toString(ITEM_INCOMPLETE)));
        assertThat(incompletedModel.getFailedItem(), is(STATUS_INCOMPLETE));
    }

    @Test
    public void getAllFailedItemsCallbackClass_callOnFilteredFailure_setErrorText() {
        createAndInitializePresenter();

        presenterImpl.getAllFailedItemsCallback.onFilteredFailure(new Throwable());

        verify(mockedView).setErrorText(ERROR_COULD_NOT_FETCH_JOBS);
    }

    @Test
    public void getAllFailedItemsCallbackClass_callOnSuccess_jobStoreProxyGetJobCompletionStateCalled() {
        createAndInitializePresenter();

        presenterImpl.getAllFailedItemsCallback.onSuccess(jobInfos);

        verify(mockedView).clearFailedItemsList();
        verify(mockedJobStoreProxy).getJobCompletionState(eq(JOB_ID), isA(PresenterImpl.GetJobCompletionStatusCallback.class));
    }

    @Test
    public void getJobCompletionStatusCallback_callOnFilteredFailure_setErrorText() {
        createAndInitializePresenter();

        presenterImpl.getJobCompletionStatusCallback.onFilteredFailure(new Throwable());

        verify(mockedView).setErrorText(ERROR_COULD_NOT_FETCH_JOB_COMPLETIONS_STATUS_FOR + Long.toString(JOB_ID));
    }

    @Test
    public void getJobCompletionStatusCallback_callOnSuccess_viewAddFailedItemsCalled3Times() {
        createAndInitializePresenter();

        presenterImpl.getJobCompletionStatusCallback.onSuccess(jobCompletionState);

        verify(mockedView, times(3)).addFailedItem(isA(FailedItemModel.class));  // In fact a call to addJobCompletionStateToView, that has already been tested
    }


    /*
     * Private test methods
     */

    private void createAndInitializePresenter() {
        presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedTexts);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

}