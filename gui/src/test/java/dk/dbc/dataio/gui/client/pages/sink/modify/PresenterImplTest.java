package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.modelBuilders.SinkModelBuilder;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
    @Mock private ClientFactory mockedClientFactory;
    @Mock private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock private SinkServiceProxyAsync mockedSinkServiceProxy;
    @Mock private Texts mockedTexts;
    @Mock private AcceptsOneWidget mockedContainerWidget;
    @Mock private EventBus mockedEventBus;
    @Mock private Exception mockedException;
    @Mock ProxyErrorTexts mockedProxyErrorTexts;

    private View view;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;
    private static boolean initializeModelHasBeenCalled;

    private final SinkModel sinkModel = new SinkModelBuilder().build();

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory) {
            super(clientFactory);
            view = PresenterImplTest.this.view;
            model = sinkModel;
        }

        @Override
        void initializeModel() {
            initializeModelHasBeenCalled = true;
        }

        @Override
        void saveModel() {
            saveModelHasBeenCalled = true;
        }

        public SaveSinkModelFilteredAsyncCallback saveSinkModelFilteredAsyncCallback = new SaveSinkModelFilteredAsyncCallback();
        public PingSinkServiceFilteredAsyncCallback pingSinkServiceFilteredAsyncCallback = new PingSinkServiceFilteredAsyncCallback();

        // Test method for reading flowStoreProxy
        public FlowStoreProxyAsync getFlowStoreProxy() {
            return flowStoreProxy;
        }

        // Test method for reading sinkServiceProxy
        public SinkServiceProxyAsync getSinkServiceProxy() {
            return sinkServiceProxy;
        }

        // Test method for reading constants
        public Texts getSinkModifyConstants() {
            return texts;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getSinkServiceProxyAsync()).thenReturn(mockedSinkServiceProxy);
        when(mockedClientFactory.getSinkModifyTexts()).thenReturn(mockedTexts);
        when(mockedClientFactory.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        mock(ContentPanel.class);

    }

    @Before
    public void setupView() {
        view = new View("Header Text");  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStoreProxy));
        assertThat(presenterImpl.getSinkServiceProxy(), is(mockedSinkServiceProxy));
        assertThat(presenterImpl.getSinkModifyConstants(), is(mockedTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        verify(mockedContainerWidget).setWidget(Matchers.any(IsWidget.class));
        assertThat(initializeModelHasBeenCalled, is(true));
    }

    @Test
    public void nameChanged_callName_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "UpdatedName";
        initializeAndStartPresenter();
        presenterImpl.nameChanged(CHANGED_NAME);
        assertThat(presenterImpl.model.getSinkName(), is(CHANGED_NAME));
    }

    @Test
    public void resourceChanged_callResourceChanged_recourceIsChangedAccordingly() {
        final String CHANGED_RESOURCE = "UpdatedResource";
        initializeAndStartPresenter();
        presenterImpl.resourceChanged(CHANGED_RESOURCE);
        assertThat(presenterImpl.model.getResourceName(), is(CHANGED_RESOURCE));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "UpdatedDescription";
        initializeAndStartPresenter();
        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);
        assertThat(presenterImpl.model.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithNameFieldEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setSinkName("");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressedWithDescriptionFieldEmpty_ErrorTextIsDisplayed() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.model.setResourceName("");

        presenterImpl.saveButtonPressed();

        verify(mockedTexts).error_InputFieldValidationError();
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {
        initializeAndStartPresenter();
        presenterImpl.keyPressed();
        verify(view.status).setText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressed_pingIsCalled() {
        initializeAndStartPresenter();
        presenterImpl.saveButtonPressed();
        verify(mockedSinkServiceProxy).ping(any(SinkModel.class), any(PresenterImpl.PingSinkServiceFilteredAsyncCallback.class));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_successfulCallbackStatusOk_saveModelIsCalled() {
        initializeAndStartPresenter();
        saveModelHasBeenCalled = false;
        presenterImpl.pingSinkServiceFilteredAsyncCallback.onSuccess(new PingResponseModel(PingResponseModel.Status.OK));
        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_successfulCallbackStatusFailed_saveModelNotCalled() {
        initializeAndStartPresenter();
        saveModelHasBeenCalled = false;
        presenterImpl.pingSinkServiceFilteredAsyncCallback.onSuccess(new PingResponseModel(PingResponseModel.Status.FAILED));
        assertThat(saveModelHasBeenCalled, is(false));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_unsuccessfulCallback_setStatusTextCalledInView() {
        initializeAndStartPresenter();
        presenterImpl.saveSinkModelFilteredAsyncCallback.onFailure(mockedException); // Emulate an unsuccessful callback from flowstore
        verify(mockedException).getMessage();  // Is called before calling view.setErrorText, which we cannot verify, since view is not mocked
    }

    @Test
    public void saveSinkModelFilteredAsyncCallback_successfulCallback_setStatusTextCalledInView() {
        final String SUCCESS_TEXT = "SuccessText";
        initializeAndStartPresenter();
        when(mockedTexts.status_SinkSuccessfullySaved()).thenReturn(SUCCESS_TEXT);

        presenterImpl.saveSinkModelFilteredAsyncCallback.onSuccess(sinkModel);  // Emulate a successful callback from flowstore

        verify(view.status).setText(SUCCESS_TEXT);  // Expect the status text to be set in View
        assertThat(presenterImpl.model, is(sinkModel));

    }

    @Test
    public void sinkModelFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {
        initializeAndStartPresenter();
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.CONFLICT_ERROR);

        presenterImpl.saveSinkModelFilteredAsyncCallback.onFailure(mockedProxyException); // Emulate an unsuccessful callback from flowstore

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_conflictError();
    }


    /*
     * Private methods
     */
    private void initializeAndStartPresenter() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

}