
package dk.dbc.dataio.gui.client.pages.flowbinder.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
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
    @Mock Throwable mockedException;

    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedClientFactory.getPlaceController()).thenReturn(mockedPlaceController);
        when(mockedClientFactory.getFlowBindersShowView()).thenReturn(mockedView);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
    }

    @After
    public void tearDownMockedData() {
        reset(mockedClientFactory);
        reset(mockedFlowStore);
        reset(mockedPlaceController);
        reset(mockedContainerWidget);
        reset(mockedEventBus);
        reset(mockedView);
        reset(mockedViewWidget);
        reset(mockedException);
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
        }
        public FetchFlowBindersCallback fetchFlowBindersCallback = new FetchFlowBindersCallback();
    }

    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModel(58L, 485L, "FCnam1", "FCspr1", "FCsrv1", "FCijs1", "FCmet1", Arrays.asList("Java Script 1"));
    private FlowModel flowModel1 = new FlowModel(14L, 343L, "Fnam1", "Fdsc1", Arrays.asList(flowComponentModel1));
    private SubmitterModel submitterModel1 = new SubmitterModel(85L, 843957L, "SUnum1", "SUnam1", "SUdes1");
    private SinkModel sinkModel1 = new SinkModel(543L, 352L, "SInam1", "SIres1");
    private FlowBinderModel flowBinderModel1 = new FlowBinderModel(123L, 111L, "FBnam1", "FBdsc1", "FBpac1", "FBfor1", "FBchr1", "FBdes1", "FBrec1", true, flowModel1, Arrays.asList(submitterModel1), sinkModel1);
    private FlowBinderModel flowBinderModel2 = new FlowBinderModel(124L, 112L, "FBnam2", "FBdsc2", "FBpac2", "FBfor2", "FBchr2", "FBdes2", "FBrec2", true, flowModel1, Arrays.asList(submitterModel1), sinkModel1);
    private List<FlowBinderModel> flowBinderModels = Arrays.asList(flowBinderModel1, flowBinderModel2);


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
        verify(mockedClientFactory).getFlowBindersShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedFlowStore).findAllFlowBinders(any(AsyncCallback.class));
    }

    @Test
    public void editFlowBinder_call_gotoEditPlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.editFlowBinder(flowBinderModel1);

        // Verify Test
        verify(mockedPlaceController).goTo(any(EditPlace.class));
    }

    @Test
    public void fetchFlowBinders_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchFlowBindersCallback.onFilteredFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(any(String.class));
    }

    @Test
    public void fetchFlowBinders_callbackWithSuccess_flowBindersAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.fetchFlowBindersCallback.onSuccess(flowBinderModels);

        // Verify Test
        verify(mockedView).setFlowBinders(flowBinderModels);
    }

}
