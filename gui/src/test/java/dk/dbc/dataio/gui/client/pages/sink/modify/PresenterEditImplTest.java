package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest {
    @Mock private ClientFactory mockedClientFactory;
    @Mock private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock private Texts mockedTexts;
    @Mock private AcceptsOneWidget mockedContainerWidget;
    @Mock private EventBus mockedEventBus;
    @Mock private EditPlace mockedEditPlace;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;

    private EditView editView;
    private PresenterEditImpl presenterEditImpl;
    private final static long DEFAULT_SINK_ID = 433L;

    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, ClientFactory clientFactory) {
            super(place, clientFactory);
        }

        public GetSinkModelFilteredAsyncCallback getSinkModelFilteredAsyncCallback = new GetSinkModelFilteredAsyncCallback();
    }
    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getSinkEditView()).thenReturn(editView);
        when(mockedClientFactory.getSinkModifyTexts()).thenReturn(mockedTexts);
        when(mockedEditPlace.getSinkId()).thenReturn(DEFAULT_SINK_ID);
    }

    @Before
    public void setupView() {
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_SinkEdit()).thenReturn("Header Text");
        editView = new EditView(mockedClientFactory);  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory);
        verify(mockedEditPlace).getSinkId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_getSinkIsInvoked() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedFlowStoreProxy).getSink(any(Long.class), any(PresenterEditImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_sinkContentOk_updateSinkCalled() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SinkModel();

        presenterEditImpl.nameChanged("a");                   // Name is ok
        presenterEditImpl.resourceChanged("resource");        // Resource is ok

        presenterEditImpl.saveModel();

        verify(mockedFlowStoreProxy).updateSink(eq(presenterEditImpl.model), any(PresenterImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelUpdated() {
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String SINK_NAME = "New Sink Name";
        SinkModel sinkModel = new SinkModel();
        sinkModel.setSinkName(SINK_NAME);

        assertThat(presenterEditImpl.model, is(notNullValue()));
        assertThat(presenterEditImpl.model.getSinkName(), is(""));

        presenterEditImpl.getSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        // Assert that the sink model has been updated correctly
        assertThat(presenterEditImpl.model.getSinkName(), is(sinkModel.getSinkName()));

        // Assert that the view is displaying the correct values
        verify(editView.name).setText(SINK_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_unsuccessfulCallback_errorMessage() {
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);

        // Emulate an unsuccessful callback from flowstore
        presenterEditImpl.getSinkModelFilteredAsyncCallback.onFailure(new Throwable());
        verify(mockedTexts).error_CannotFetchSink(); // We cannot verify view, since it is not mocked - however, we know, that the error text shall be fetched - and we therefore verify on that
    }

}
