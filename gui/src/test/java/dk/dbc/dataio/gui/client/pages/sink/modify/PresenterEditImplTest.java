package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PresenterEditImplTest {
    private ClientFactory mockedClientFactory;
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    private Texts mockedTexts;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedEditView;
    private EditPlace mockedEditPlace;

    private PresenterEditImpl presenterEditImpl;

    private PresenterEditImplConcrete presenterEditImplConcrete;

    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, ClientFactory clientFactory, Texts texts) {
            super(place, clientFactory, texts);
            view = mockedEditView;
        }

        public GetSinkModelFilteredAsyncCallback getSinkCallback = new GetSinkModelFilteredAsyncCallback();
    }
    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        mockedClientFactory = mock(ClientFactory.class);
        mockedFlowStoreProxy = mock(FlowStoreProxyAsync.class);
        mockedTexts = mock(Texts.class);
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedEditView = mock(View.class);
        mockedEditPlace = mock(EditPlace.class);
        when(mockedClientFactory.getSinkEditView()).thenReturn(mockedEditView);
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        verify(mockedEditPlace, times(1)).getSinkId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_getSinkIsInvoked() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedFlowStoreProxy, times(1)).getSink(any(Long.class), any(PresenterEditImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_sinkContentOk_updateSinkCalled() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SinkModel();

        presenterEditImpl.nameChanged("a");                   // Name is ok
        presenterEditImpl.resourceChanged("resource");        // Resource is ok

        presenterEditImpl.saveModel();

        verify(mockedFlowStoreProxy, times(1)).updateSink(eq(presenterEditImpl.model), any(PresenterImpl.SaveSinkModelFilteredAsyncCallback.class));
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_successfulCallback_modelIsInitializedCorrectly() {
        presenterEditImplConcrete = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);
        SinkModel model = new SinkModel(4453, 1L, "Name", "Resource");

        assertThat(presenterEditImplConcrete.model, is(notNullValue()));
        assertThat(presenterEditImplConcrete.model.getSinkName(), is(""));

        presenterEditImplConcrete.getSinkCallback.onSuccess(model);  // Emulate a successful callback from flowstore

        // Assert that the sink model has been updated correctly
        assertThat(presenterEditImplConcrete.model, is(notNullValue()));
        assertThat(presenterEditImplConcrete.model.getId(), is(model.getId()));
        assertThat(presenterEditImplConcrete.model.getVersion(), is(model.getVersion()));
        assertThat(presenterEditImplConcrete.model.getSinkName(), is(model.getSinkName()));
        assertThat(presenterEditImplConcrete.model.getResourceName(), is(model.getResourceName()));

        // Assert that the view is displaying the correct values
        verify(mockedEditView, times(1)).setName(model.getSinkName());
        verify(mockedEditView, times(1)).setResource(model.getResourceName());
    }

    @Test
    public void getSinkModelFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {
        presenterEditImplConcrete = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);
        // Emulate an unsuccessful callback from flowstore
        presenterEditImplConcrete.getSinkCallback.onFailure(new Throwable(mockedTexts.error_CannotFetchSink()));
        // Expect the error text to be set in View
        verify(mockedEditView, times(1)).setErrorText(mockedTexts.error_CannotFetchSink());
    }

}
