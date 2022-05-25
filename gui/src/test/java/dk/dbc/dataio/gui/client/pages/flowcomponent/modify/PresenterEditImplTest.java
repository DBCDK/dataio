package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterEditImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {

    @Mock
    private Texts mockedTexts;
    @Mock
    private EditPlace mockedPlace;
    @Mock
    private JavaScriptProjectFetcherAsync mockedJavaScriptProjectFetcher;
    @Mock
    dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    private ViewGinjector mockedViewInjector;
    private View editView;

    private PresenterEditImplConcrete presenterEditImpl;
    private final static long DEFAULT_FLOW_COMPONENT_ID = 426L;


    class PresenterEditImplConcrete<Place extends EditPlace> extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, String header) {
            super(place, header);
        }

        public GetFlowComponentModelFilteredAsyncCallback callback = new GetFlowComponentModelFilteredAsyncCallback();
    }

    @Before
    public void setupMockedObjects() {
        editView = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedCommonGinjector.getJavaScriptProjectFetcherAsync()).thenReturn(mockedJavaScriptProjectFetcher);
        when(mockedViewInjector.getView()).thenReturn(editView);
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedPlace.getFlowComponentId()).thenReturn(DEFAULT_FLOW_COMPONENT_ID);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowComponentEdit()).thenReturn("Header Text");
    }

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Setup
        setupPresenterEdit();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly

        verify(mockedPlace).getFlowComponentId();
    }


    @Test
    public void start_callStart_ok() {
        // Setup
        setupPresenterEdit();

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);

        // Verifications
        verify(editView.deleteButton).setVisible(true);
        verifyNoMoreInteractions(editView.deleteButton);
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {

        // Setup
        setupPresenterEdit();
        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getName(), is(""));

        // Subject Under Test
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // Verifications
        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getName(), is(""));
        assertThat(presenterEditImpl.model.getSvnProject(), is(""));
        assertThat(presenterEditImpl.model.getSvnRevision(), is(""));
        assertThat(presenterEditImpl.model.getInvocationJavascript(), is(""));
        assertThat(presenterEditImpl.model.getInvocationMethod(), is(""));
        assertThat(presenterEditImpl.model.getJavascriptModules(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getJavascriptModules().isEmpty(), is(true));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).getFlowComponent(eq(DEFAULT_FLOW_COMPONENT_ID), any(PresenterEditImpl.GetFlowComponentModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_callSaveModel_updateFlowComponentInFlowStoreCalled() throws Throwable {

        // Setup
        setupPresenterEdit();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_COMPONENT_MODEL_NAME = "Flow Component Model Name";
        FlowComponentModel flowComponentModel = new FlowComponentModel();
        flowComponentModel.setName(FLOW_COMPONENT_MODEL_NAME);
        presenterEditImpl.nameChanged(FLOW_COMPONENT_MODEL_NAME);

        // Subject Under Test
        presenterEditImpl.saveModel();

        // Verifications
        ArgumentCaptor<FlowComponentModel> flowComponentModelArgumentCaptor = ArgumentCaptor.forClass(FlowComponentModel.class);
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).updateFlowComponent(flowComponentModelArgumentCaptor.capture(), any(PresenterImpl.SaveFlowComponentModelFilteredAsyncCallback.class));
        assertThat(flowComponentModelArgumentCaptor.getValue().getName(), is(FLOW_COMPONENT_MODEL_NAME));
    }

    @Test
    public void getFlowComponentModelFilteredAsyncCallback_unSuccessfulCallback_errorMessage() {

        // Setup
        setupPresenterEdit();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        // Subject Under Test
        presenterEditImpl.callback.onFilteredFailure(mockedProxyException);

        // Verifications
        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    @Test
    public void getFlowComponentModelFilteredAsyncCallback_successfulCallback_modelUpdated() {

        // Setup
        setupPresenterEdit();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_COMPONENT_MODEL_NAME = "New Flow Component Model Name";
        FlowComponentModel flowComponentModel = new FlowComponentModel();
        flowComponentModel.setName(FLOW_COMPONENT_MODEL_NAME);

        // Subject Under Test
        presenterEditImpl.callback.onSuccess(flowComponentModel);

        // Verifications
        verify(editView.name).setText(FLOW_COMPONENT_MODEL_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
    }

    @Test
    public void deleteFlowComponentFilteredAsyncCallback_callback_invoked() {
        setupPresenterEdit();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);

        presenterEditImpl.deleteModel();

        // Verify that the proxy call is invoked... Cannot emulate the callback as the return type is Void
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).deleteFlowComponent(
                eq(presenterEditImpl.model.getId()),
                eq(presenterEditImpl.model.getVersion()),
                any(PresenterEditImpl.DeleteFlowComponentFilteredAsyncCallback.class));
    }

    private void setupPresenterEdit() {
        presenterEditImpl = new PresenterEditImplConcrete(mockedPlace, header);
        presenterEditImpl.viewInjector = mockedViewInjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;
    }
}
