
package dk.dbc.dataio.gui.client.pages.flow.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace;
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
import static org.mockito.Matchers.eq;
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
    @Mock ClientFactory mockedClientFactory;
    @Mock FlowStoreProxyAsync mockedFlowStore;
    @Mock PlaceController mockedPlaceController;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock Widget mockedViewWidget;
    @Mock ProxyException mockedProxyException;
    @Mock ProxyErrorTexts mockedProxyErrorTexts;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getFlowsShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedClientFactory.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
        }
        public FetchFlowsCallback fetchFlowsCallback = new FetchFlowsCallback();
        public RefreshFlowComponentsCallback refreshFlowComponentsCallback = new RefreshFlowComponentsCallback();
    }

    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModel(58L, 485L, "FCnam1", "FCspr1", "FCsrv1", "FCijs1", "FCmet1", Arrays.asList("Java Script 1"), "description");
    private FlowComponentModel flowComponentModel2 = new FlowComponentModel(59L, 486L, "FCnam2", "FCspr2", "FCsrv2", "FCijs2", "FCmet2", Arrays.asList("Java Script 2", "Java Script 3"), "description");
    private FlowComponentModel flowComponentModel3 = new FlowComponentModel(60L, 487L, "FCnam3", "FCspr3", "FCsrv3", "FCijs3", "FCmet3", Arrays.asList("Java Script 4", "Java Script 5", "Java Script 6"), "description");
    private FlowModel flowModel1 = new FlowModel(14L, 343L, "Fnam1", "Fdsc1", Arrays.asList(flowComponentModel1));
    private FlowModel flowModel2 = new FlowModel(15L, 344L, "Fnam2", "Fdsc2", Arrays.asList(flowComponentModel2, flowComponentModel3));
    private List<FlowModel> flowModels = Arrays.asList(flowModel1, flowModel2);


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
        verify(mockedClientFactory).getFlowsShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllFlows(any(AsyncCallback.class));
    }

    @Test
    public void editFlow_call_gotoEditPlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editFlow(flowModel1);

        // Verify Test
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void createFlow_call_gotoCreatePlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.createFlow();

        // Verify Test
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void refreshFlowComponents_call_refreshFlowComponentsInFlowStore() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.refreshFlowComponents(flowModel1);

        // Verify Test
        verify(mockedFlowStore).refreshFlowComponents(eq(flowModel1.getId()), eq(flowModel1.getVersion()), any(PresenterImpl.RefreshFlowComponentsCallback.class));
    }

    @Test
    public void fetchFlows_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.fetchFlowsCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedClientFactory).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void fetchFlows_callbackWithSuccess_flowsAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchFlowsCallback.onSuccess(flowModels);

        // Verify Test
        verify(mockedView).setFlows(flowModels);
    }

    @Test
    public void refreshFlowComponents_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);

        // Test Subject Under Test
        presenterImpl.refreshFlowComponentsCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedClientFactory).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void refreshFlowComponents_callbackWithSuccess_fetchFlowsIsRequested() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.refreshFlowComponentsCallback.onSuccess(flowModel1);

        // Verify Test
        verify(mockedFlowStore, times(2)).findAllFlows(any(PresenterImpl.FetchFlowsCallback.class));
        // findAllFlows is requested both from presenter.start() and from the callback - therefore it is called twice
    }

}
