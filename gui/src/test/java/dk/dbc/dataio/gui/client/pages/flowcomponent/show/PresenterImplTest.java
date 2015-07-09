
package dk.dbc.dataio.gui.client.pages.flowcomponent.show;


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
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
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
    @Mock SingleSelectionModel<FlowComponentModel> mockedSelectionModel;
    @Mock ListDataProvider<FlowComponentModel> mockedDataProvider;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getFlowComponentsShowView()).thenReturn(mockedView);
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
        public FetchFlowComponentsCallback fetchFlowComponentsCallback = new FetchFlowComponentsCallback();
    }

    // Test Data
    FlowComponentModel testModel1 = new FlowComponentModelBuilder().setName("FCName1").build();
    FlowComponentModel testModel2 = new FlowComponentModelBuilder().setName("FCName2").build();
    private List<FlowComponentModel> testModels = Arrays.asList(testModel1, testModel2);


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
        verify(mockedClientFactory).getFlowComponentsShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllFlowComponents(any(AsyncCallback.class));
    }

    @Test
    public void editFlowComponent_call_gotoEditPlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editFlowComponent(testModels.get(0));

        // Verify Test
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void createFlowComponent_call_gotoCreatePlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.createFlowComponent();

        // Verify Test
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void fetchFlowComponents_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.fetchFlowComponentsCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedClientFactory).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void fetchFlowComponents_callbackWithSuccess_flowComponentsAreFetchedInitialCallback() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchFlowComponentsCallback.onSuccess(testModels);

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setFlowComponents(testModels);
    }

    @Test
    public void fetchFlowComponents_callbackWithSuccess_flowComponentsAreFetchedNoChanges() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);

        // Test Subject Under Test
        presenterImpl.fetchFlowComponentsCallback.onSuccess(testModels);

        // Verify Test
        verifyZeroInteractions(mockedSelectionModel);
        verify(mockedView, times(0)).setFlowComponents(testModels);
    }

    @Test
    public void fetchFlowComponents_callbackWithSuccess_flowComponentsAreFetchedOneHasChangedSelectionIsSet() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(testModel1);

        FlowComponentModel editedFlowComponent = new FlowComponentModelBuilder().setName("editedName").build();
        List<FlowComponentModel> flowComponentModels = Arrays.asList(editedFlowComponent, testModel2);

        // Test Subject Under Test
        presenterImpl.fetchFlowComponentsCallback.onSuccess(flowComponentModels);

        // Verify Test
        verify(mockedSelectionModel).setSelected(editedFlowComponent, true);
        verify(mockedView).setFlowComponents(flowComponentModels);
    }

}
