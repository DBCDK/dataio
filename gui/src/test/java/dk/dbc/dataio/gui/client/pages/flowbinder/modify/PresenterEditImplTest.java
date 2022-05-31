package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.RecordSplitterConstants;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {

    @Mock
    private Texts mockedTexts;
    @Mock
    private EditPlace mockedPlace;
    @Mock
    dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    ViewGinjector mockedViewInjector;

    private View editView;

    private PresenterEditImplConcrete presenterEditImpl;

    private final static String INPUT_FIELD_VALIDATION_ERROR = "InputFieldValidationError";
    private final static long DEFAULT_FLOWBINDER_ID = 776L;

    class PresenterEditImplConcrete<Place extends EditPlace> extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, String header) {
            super(place, header);
        }

        public View getView() {
            return editView;
        }

        public GetFlowBinderModelFilteredAsyncCallback callback = new GetFlowBinderModelFilteredAsyncCallback();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupView() {
        editView = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewInjector.getView()).thenReturn(editView);
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedTexts.error_InputFieldValidationError()).thenReturn(INPUT_FIELD_VALIDATION_ERROR);
        when(mockedPlace.getFlowBinderId()).thenReturn(DEFAULT_FLOWBINDER_ID);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowBinderEdit()).thenReturn("Header Text");
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterEditImplConcrete();
        // The instanitation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly

        verify(mockedPlace).getFlowBinderId();
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        setupPresenterEditImplConcrete();
        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getName(), is(""));
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getName(), is(""));
        assertThat(presenterEditImpl.model.getDescription(), is(""));
        assertThat(presenterEditImpl.model.getPackaging(), is(""));
        assertThat(presenterEditImpl.model.getFormat(), is(""));
        assertThat(presenterEditImpl.model.getCharset(), is(""));
        assertThat(presenterEditImpl.model.getDestination(), is(""));
        assertThat(presenterEditImpl.model.getPriority(), is(Priority.NORMAL.getValue()));
        assertThat(presenterEditImpl.model.getRecordSplitter(), is(RecordSplitterConstants.getRecordSplitters().get(0).name()));
        assertThat(presenterEditImpl.model.getFlowModel(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getFlowModel().getFlowName(), is(""));
        assertThat(presenterEditImpl.model.getSubmitterModels(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getSubmitterModels().size(), is(0));
        assertThat(presenterEditImpl.model.getSinkModel(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getSinkModel().getSinkName(), is(""));

        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllSubmitters(any(PresenterEditImpl.FetchAvailableSubmittersCallback.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllFlows(any(PresenterEditImpl.FetchAvailableFlowsCallback.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).findAllSinks(any(PresenterEditImpl.FetchAvailableSinksCallback.class));
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).getFlowBinder(eq(DEFAULT_FLOWBINDER_ID), any(PresenterEditImpl.GetFlowBinderModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_callSaveModel_updateFlowBinderMethodInFlowStoreCalled() {
        setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_BINDER_MODEL_NAME = "Completely New Flow Binder Model Name";
        FlowBinderModel flowBinderModel = new FlowBinderModel();
        flowBinderModel.setName(FLOW_BINDER_MODEL_NAME);
        presenterEditImpl.nameChanged(FLOW_BINDER_MODEL_NAME);

        presenterEditImpl.saveModel();

        ArgumentCaptor<FlowBinderModel> flowBinderModelArgumentCaptor = ArgumentCaptor.forClass(FlowBinderModel.class);
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).updateFlowBinder(flowBinderModelArgumentCaptor.capture(), any(PresenterImpl.SaveFlowBinderModelFilteredAsyncCallback.class));
        assertThat(flowBinderModelArgumentCaptor.getValue().getName(), is(FLOW_BINDER_MODEL_NAME));
    }

    @Test
    public void getFlowBinderModelFilteredAsyncCallback_unSuccessfullCalback_errorMessage() {
        setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        presenterEditImpl.callback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    @Test
    public void getFlowBinderModelFilteredAsyncCallback_successfullCalback_modelUpdated() {
        setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_BINDER_MODEL_NAME = "New Flow Binder Model Name";
        FlowBinderModel flowBinderModel = new FlowBinderModel();
        flowBinderModel.setName(FLOW_BINDER_MODEL_NAME);

        presenterEditImpl.callback.onSuccess(flowBinderModel);

        verify(editView.name).setText(FLOW_BINDER_MODEL_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
    }

    @Test
    public void deleteFlowBinderModelFilteredAsyncCallback_callback_invoked() {
        setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);

        presenterEditImpl.deleteModel();

        // Verify that the proxy call is invoked... Cannot emulate the callback as the return type is Void
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).deleteFlowBinder(
                eq(presenterEditImpl.model.getId()),
                eq(presenterEditImpl.model.getVersion()),
                any(PresenterEditImpl.DeleteFlowBinderModelFilteredAsyncCallback.class));
    }

    private void setupPresenterEditImplConcrete() {
        this.presenterEditImpl = new PresenterEditImplConcrete(mockedPlace, header);
        this.presenterEditImpl.commonInjector = mockedCommonGinjector;
        this.presenterEditImpl.viewInjector = mockedViewInjector;
    }
}
