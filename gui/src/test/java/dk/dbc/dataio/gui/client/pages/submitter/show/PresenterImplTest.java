
package dk.dbc.dataio.gui.client.pages.submitter.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.submitter.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
    @Mock FlowStoreProxyAsync mockedFlowStore;
    @Mock PlaceController mockedPlaceController;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock ProxyException mockedProxyException;
    @Mock ProxyErrorTexts mockedProxyErrorTexts;
    @Mock SingleSelectionModel<SubmitterModel> mockedSelectionModel;
    @Mock ListDataProvider<SubmitterModel> mockedDataProvider;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getSubmittersShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedClientFactory.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.dataProvider = mockedDataProvider;
    }


    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
        }
        public FetchSubmittersCallback fetchSubmittersCallback = new FetchSubmittersCallback();
    }

    // Test Data
    private SubmitterModel testModel1 = new SubmitterModelBuilder().setId(1L).setName("model1").build();
    private SubmitterModel testModel2 = new SubmitterModelBuilder().setId(2L).setName("model2").build();
    private List<SubmitterModel> testModels = new ArrayList<SubmitterModel>(Arrays.asList(testModel1, testModel2));


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        presenterImpl = new PresenterImpl(mockedClientFactory);

        // Verify Test
        verify(mockedClientFactory).getFlowStoreProxyAsync();
        verify(mockedClientFactory).getPlaceController();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        presenterImpl = new PresenterImpl(mockedClientFactory);

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedClientFactory).getSubmittersShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllSubmitters(any(AsyncCallback.class));
    }

    @Test
    public void editSubmitter_call_gotoEditPlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editSubmitter(testModels.get(0));

        // Verify Test
        verifyZeroInteractions(mockedSelectionModel);
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void createSubmitter_call_gotoCreatePlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.createSubmitter();

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void fetchSubmitters_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedClientFactory).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmittersAreFetchedInitialCallback() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(new ArrayList<SubmitterModel>());

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(testModels);

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setSubmitters(testModels);
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmittersAreFetchedNoChanges() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(testModels);

        // Verify Test
        verifyZeroInteractions(mockedSelectionModel);
        verify(mockedView, times(0)).setSubmitters(testModels);
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmittersAreFetchedOneHasChangedSelectionIsSet() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(testModel1);

        SubmitterModel editedSubmitter = new SubmitterModelBuilder().setName("editedName").build();
        List<SubmitterModel> submitterModels = Arrays.asList(editedSubmitter, testModel2);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(submitterModels);

        // Verify Test
        verify(mockedSelectionModel).setSelected(editedSubmitter, true);
        verify(mockedView).setSubmitters(submitterModels);
    }

    @Test
    public void fetchSubmitters_callbackWithSuccess_SubmitterAreFetchedOneWasDeletedSelectionIsCleared() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);

        // Test Subject Under Test
        presenterImpl.fetchSubmittersCallback.onSuccess(Collections.singletonList(testModel1));

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setSubmitters(Collections.singletonList(testModel1));
    }

}
