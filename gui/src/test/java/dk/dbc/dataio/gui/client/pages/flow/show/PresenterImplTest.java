package dk.dbc.dataio.gui.client.pages.flow.show;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.pages.flow.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flow.modify.EditPlace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    View mockedView;
    @Mock
    Widget mockedViewWidget;
    @Mock
    SingleSelectionModel<FlowModel> mockedSelectionModel;
    @Mock
    ListDataProvider<FlowModel> mockedDataProvider;
    @Mock
    ViewGinjector mockedViewGinjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {

        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(mockedView);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.dataProvider = mockedDataProvider;
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;

    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete() {
            super(mockedPlaceController);
            commonInjector = mockedCommonGinjector;
            viewInjector = mockedViewGinjector;
        }

        public FetchFlowsCallback fetchFlowsCallback = new FetchFlowsCallback();
        public RefreshFlowComponentsCallback refreshFlowComponentsCallback = new RefreshFlowComponentsCallback();
    }

    // Test Data
    private FlowModel flowModel1 = new FlowModelBuilder()
            .setName("Fnam1")
            .setComponents(Collections.singletonList(new FlowComponentModelBuilder().build()))
            .build();

    private FlowModel flowModel2 = new FlowModelBuilder()
            .setName("Fnam2")
            .setComponents(Arrays.asList(new FlowComponentModelBuilder().setName("FCnam1").build(), new FlowComponentModelBuilder().setName("FCnam2").build()))
            .build();

    private List<FlowModel> flowModels = Arrays.asList(flowModel1, flowModel2);

    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {
        setupPresenterImpl();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedViewGinjector, times(3)).getView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllFlows(any(AsyncCallback.class));
    }

    @Test
    public void editFlow_call_gotoEditPlace() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editFlow(flowModel1);

        // Verify Test
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void createFlow_call_gotoCreatePlace() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.createFlow();

        // Verify Test
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void refreshFlowComponents_call_refreshFlowComponentsInFlowStore() {
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.refreshFlowComponents(flowModel1);

        // Verify Test
        verify(mockedFlowStore).refreshFlowComponents(eq(flowModel1.getId()), eq(flowModel1.getVersion()), any(PresenterImpl.RefreshFlowComponentsCallback.class));
    }

    @Test
    public void fetchFlows_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.fetchFlowsCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedCommonGinjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void refreshFlowComponents_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);

        // Test Subject Under Test
        presenterImpl.refreshFlowComponentsCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedCommonGinjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void refreshFlowComponents_callbackWithSuccess_fetchFlowsIsRequested() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.refreshFlowComponentsCallback.onSuccess(flowModel1);

        // Verify Test
        verify(mockedFlowStore, times(2)).findAllFlows(any(PresenterImpl.FetchFlowsCallback.class));
        // findAllFlows is requested both from presenter.start() and from the callback - therefore it is called twice
    }

    @Test
    public void fetchFlows_callbackWithSuccess_flowsAreFetchedInitialCallback() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchFlowsCallback.onSuccess(flowModels);

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setFlows(flowModels);
    }

    @Test
    public void fetchFlows_callbackWithSuccess_flowsAreFetchedNoChanges() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(flowModels);

        // Test Subject Under Test
        presenterImpl.fetchFlowsCallback.onSuccess(flowModels);

        // Verify Test
        verifyNoInteractions(mockedSelectionModel);
        verify(mockedView, times(0)).setFlows(flowModels);
    }

    @Test
    public void fetchFlows_callbackWithSuccess_flowsAreFetchedOneHasChangedSelectionIsSet() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(flowModels);
        when(mockedSelectionModel.getSelectedObject()).thenReturn(flowModel1);

        FlowModel editedFlow = new FlowModelBuilder().setName("editedName").build();
        List<FlowModel> flowModels = Arrays.asList(editedFlow, flowModel2);

        // Test Subject Under Test
        presenterImpl.fetchFlowsCallback.onSuccess(flowModels);

        // Verify Test
        verify(mockedSelectionModel).setSelected(editedFlow, true);
        verify(mockedView).setFlows(flowModels);
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImpl(mockedPlaceController);
        presenterImpl.viewInjector = mockedViewGinjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
    }
}
