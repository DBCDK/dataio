package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest {
    @Mock private ClientFactory mockedClientFactory;
    @Mock private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock private Texts mockedTexts;
    @Mock private AcceptsOneWidget mockedContainerWidget;
    @Mock private EventBus mockedEventBus;
    @Mock private EditPlace mockedPlace;

    private View view;

    private PresenterEditImpl presenterEditImpl;

    private final static String INPUT_FIELD_VALIDATION_ERROR = "InputFieldValidationError";
    private final static String DEFAULT_RECORD_SPLITTER = "Default Record Splitter";
    private final static long DEFAULT_FLOWBINDER_ID = 776L;

    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, ClientFactory clientFactory, Texts constants) {
            super(place, clientFactory, constants);
        }

        public GetFlowBinderModelFilteredAsyncCallback callback = new GetFlowBinderModelFilteredAsyncCallback();
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getFlowBinderEditView()).thenReturn(view);
        when(mockedTexts.error_InputFieldValidationError()).thenReturn(INPUT_FIELD_VALIDATION_ERROR);
        when(mockedTexts.label_DefaultRecordSplitter()).thenReturn(DEFAULT_RECORD_SPLITTER);
        when(mockedPlace.getFlowBinderId()).thenReturn(DEFAULT_FLOWBINDER_ID);
    }

    @Before
    public void setupView() {
        view = new View("Header Text");  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    @After
    public void tearDownMockedObjects() {
        reset(mockedClientFactory);
        reset(mockedFlowStoreProxy);
        reset(mockedTexts);
        reset(mockedContainerWidget);
        reset(mockedEventBus);
        reset(mockedPlace);
        reset(view.name);
        reset(view.description);
        reset(view.frame);
        reset(view.format);
        reset(view.charset);
        reset(view.destination);
        reset(view.recordSplitter);
        reset(view.sequenceAnalysis);
        reset(view.submitters);
        reset(view.flow);
        reset(view.sink);
        reset(view.status);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterEditImpl = new PresenterEditImpl(mockedPlace, mockedClientFactory, mockedTexts);
        // The instanitation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly

        verify(mockedClientFactory).getFlowBinderEditView();
        verify(mockedPlace).getFlowBinderId();
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        presenterEditImpl = new PresenterEditImpl(mockedPlace, mockedClientFactory, mockedTexts);
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
        assertThat(presenterEditImpl.model.getRecordSplitter(), is(""));
        assertThat(presenterEditImpl.model.getFlowModel(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getFlowModel().getFlowName(), is(""));
        assertThat(presenterEditImpl.model.getSubmitterModels(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getSubmitterModels().size(), is(0));
        assertThat(presenterEditImpl.model.getSinkModel(), is(notNullValue()));
        assertThat(presenterEditImpl.model.getSinkModel().getSinkName(), is(""));

        verify(mockedFlowStoreProxy).findAllSubmitters(any(PresenterEditImpl.FetchAvailableSubmittersCallback.class));
        verify(mockedFlowStoreProxy).findAllFlows(any(PresenterEditImpl.FetchAvailableFlowsCallback.class));
        verify(mockedFlowStoreProxy).findAllSinks(any(PresenterEditImpl.FetchAvailableSinksCallback.class));
        verify(mockedFlowStoreProxy).getFlowBinder(eq(DEFAULT_FLOWBINDER_ID), any(PresenterEditImpl.GetFlowBinderModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_callSaveModel_updateFlowBinderMethodInFlowStoreCalled() {
        presenterEditImpl = new PresenterEditImpl(mockedPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_BINDER_MODEL_NAME = "Completely New Flow Binder Model Name";
        FlowBinderModel flowBinderModel = new FlowBinderModel();
        flowBinderModel.setName(FLOW_BINDER_MODEL_NAME);
        presenterEditImpl.nameChanged(FLOW_BINDER_MODEL_NAME);

        presenterEditImpl.saveModel();

        ArgumentCaptor<FlowBinderModel> flowBinderModelArgumentCaptor = ArgumentCaptor.forClass(FlowBinderModel.class);
        verify(mockedFlowStoreProxy).updateFlowBinder(flowBinderModelArgumentCaptor.capture(), any(PresenterImpl.SaveFlowBinderModelFilteredAsyncCallback.class));
        assertThat(flowBinderModelArgumentCaptor.getValue().getName(), is(FLOW_BINDER_MODEL_NAME));
    }

    @Test
    public void getFlowBinderModelFilteredAsyncCallback_unSuccessfullCalback_errorMessage() {
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);

        presenterEditImpl.callback.onFilteredFailure(new Throwable());

        verify(mockedTexts).error_CannotFetchFlowBinder();  // We cannot verify view, since it is not mocked - however, we know, that the error text shall be fetched - and we therefore verify on that
    }

    @Test
    public void getFlowBinderModelFilteredAsyncCallback_successfullCalback_modelUpdated() {
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String FLOW_BINDER_MODEL_NAME = "New Flow Binder Model Name";
        FlowBinderModel flowBinderModel = new FlowBinderModel();
        flowBinderModel.setName(FLOW_BINDER_MODEL_NAME);

        presenterEditImpl.callback.onSuccess(flowBinderModel);

        verify(view.name).setText(FLOW_BINDER_MODEL_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
    }


}
