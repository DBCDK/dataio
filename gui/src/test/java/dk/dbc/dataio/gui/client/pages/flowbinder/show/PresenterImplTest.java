
package dk.dbc.dataio.gui.client.pages.flowbinder.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.FlowBinderModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowComponentModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.FlowModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
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
        when(mockedClientFactory.getFlowBindersShowView()).thenReturn(mockedView);
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
        public FetchFlowBindersCallback fetchFlowBindersCallback = new FetchFlowBindersCallback();
    }

    // Test Data
    private FlowComponentModel flowComponentModel1 = new FlowComponentModelBuilder().build();
    private FlowModel flowModel1 = new FlowModelBuilder().setComponents(Collections.singletonList(flowComponentModel1)).build();
    private SinkModel sinkModel1 = new SinkModelBuilder().setName("SInam1").build();

    private FlowBinderModel flowBinderModel1 = new FlowBinderModelBuilder()
            .setName("FBnam1")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Collections.singletonList(new SubmitterModelBuilder().build()))
            .setSinkModel(sinkModel1)
            .build();

    private FlowBinderModel flowBinderModel2 = new FlowBinderModelBuilder()
            .setName("FBnam2")
            .setFlowModel(flowModel1)
            .setSubmitterModels(Collections.singletonList(new SubmitterModelBuilder().build()))
            .setSinkModel(sinkModel1)
            .build();

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
    public void createFlowBinder_call_gotoCreatePlace() {
        presenterImpl = new PresenterImpl(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.createFlowBinder();

        // Verify Test
        verify(mockedPlaceController).goTo(any(CreatePlace.class));
    }

    @Test
    public void fetchFlowBinders_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.SERVICE_NOT_FOUND);

        // Test Subject Under Test
        presenterImpl.fetchFlowBindersCallback.onFilteredFailure(mockedProxyException);

        // Verify Test
        verify(mockedClientFactory).getProxyErrorTexts();
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_serviceError();
        verify(mockedView).setErrorText(anyString());
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
