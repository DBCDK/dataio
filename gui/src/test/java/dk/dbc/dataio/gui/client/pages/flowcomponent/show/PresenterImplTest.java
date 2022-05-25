package dk.dbc.dataio.gui.client.pages.flowcomponent.show;


import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.EditPlace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    SingleSelectionModel<FlowComponentModel> mockedSelectionModel;
    @Mock
    ListDataProvider<FlowComponentModel> mockedDataProvider;
    @Mock
    ViewGinjector mockedViewInjector;

    // Setup mocked data
    @Before
    public void setupMockedData() {

        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewInjector.getView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        mockedView.selectionModel = mockedSelectionModel;
        mockedView.dataProvider = mockedDataProvider;
    }


    // Subject Under Test
    private PresenterImplConcrete presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(PlaceController placeController, String header) {
            super(placeController, header);
            this.viewGinjector = mockedViewInjector;
            this.commonGinjector = mockedCommonGinjector;
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
        setupPresenterImpl();
    }


    @Test
    @SuppressWarnings("unchecked")
    public void start_callStart_ok() {

        // Setup
        setupPresenterImpl();

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedViewInjector, times(3)).getView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllFlowComponents(any(AsyncCallback.class));
    }

    @Test
    public void editFlowComponent_call_gotoEditPlace() {

        // Setup
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editFlowComponent(testModels.get(0));

        // Verify Test
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void createFlowComponent_call_gotoCreatePlace() {

        // Setup
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.createFlowComponent();

        // Verify Test
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void fetchFlowComponents_callbackWithError_errorMessageInView() {


        // Setup
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.fetchFlowComponentsCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedCommonGinjector).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
    }

    @Test
    public void fetchFlowComponents_callbackWithSuccess_flowComponentsAreFetchedInitialCallback() {


        // Setup
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchFlowComponentsCallback.onSuccess(testModels);

        // Verify Test
        verify(mockedSelectionModel).clear();
        verify(mockedView).setFlowComponents(testModels);
    }

    @Test
    public void fetchFlowComponents_callbackWithSuccess_flowComponentsAreFetchedNoChanges() {


        // Setup
        setupPresenterImpl();
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedDataProvider.getList()).thenReturn(testModels);

        // Test Subject Under Test
        presenterImpl.fetchFlowComponentsCallback.onSuccess(testModels);

        // Verify Test
        verifyNoInteractions(mockedSelectionModel);
        verify(mockedView, times(0)).setFlowComponents(testModels);
    }

    @Test
    public void fetchFlowComponents_callbackWithSuccess_flowComponentsAreFetchedOneHasChangedSelectionIsSet() {

        // Setup
        setupPresenterImpl();
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

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImplConcrete(mockedPlaceController, header);
        presenterImpl.commonGinjector = mockedCommonGinjector;
        presenterImpl.viewGinjector = mockedViewInjector;
    }
}
