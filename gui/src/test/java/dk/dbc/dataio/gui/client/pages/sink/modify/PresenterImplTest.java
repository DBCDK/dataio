package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class PresenterImplTest {
    private ClientFactory mockedClientFactory;
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    private SinkServiceProxyAsync mockedSinkServiceProxy;
    private Texts mockedTexts;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedView;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;

    private final static long DEFAULT_ID = 0;
    private final static long DEFAULT_VERSION = 0;
    private final static String DEFAULT_NAME = "SinkName";
    private final static String DEFAULT_RESOURCE = "SinkResource";
    private final static String FAILURE_TEXT = "FailureText";
    private final static String SUCCESS_TEXT = "SuccessText";

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory, Texts texts) {
            super(clientFactory, texts);
            view = mockedView;
        }

        @Override
        void initializeModel() {
            model = new SinkModel(DEFAULT_ID, DEFAULT_VERSION, DEFAULT_NAME, DEFAULT_RESOURCE);
        }

        @Override
        void saveModel() {
            saveModelHasBeenCalled = true;
        }

        public SaveSinkModelFilteredAsyncCallback saveSinkCallback = new SaveSinkModelFilteredAsyncCallback();
        public PingSinkServiceFilteredAsyncCallback pingSinkCallback = new PingSinkServiceFilteredAsyncCallback();

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
        mockedClientFactory = mock(ClientFactory.class);
        mockedFlowStoreProxy = mock(FlowStoreProxyAsync.class);
        mockedSinkServiceProxy = mock(SinkServiceProxyAsync.class);
        mockedTexts = mock(Texts.class);
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getSinkServiceProxyAsync()).thenReturn(mockedSinkServiceProxy);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedView = mock(View.class);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStoreProxy));
        assertThat(presenterImpl.getSinkServiceProxy(), is(mockedSinkServiceProxy));
        assertThat(presenterImpl.getSinkModifyConstants(), is(mockedTexts));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        createAndInitializePresenterImpl();

        verify(mockedView, times(1)).setPresenter(presenterImpl);
        verify(mockedView, times(1)).asWidget();
        verify(mockedContainerWidget, times(1)).setWidget(any(IsWidget.class));
    }

    @Test
    public void nameChanged_callName_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "UpdatedName";

        createAndInitializePresenterImpl();

        presenterImpl.nameChanged(CHANGED_NAME);

        assertThat(presenterImpl.model.getSinkName(), is(CHANGED_NAME));
    }

    @Test
    public void resourceChanged_callResourceChanged_recourceIsChangedAccordingly() {
        final String CHANGED_RESOURCE = "UpdatedResource";

        createAndInitializePresenterImpl();

        presenterImpl.resourceChanged(CHANGED_RESOURCE);

        assertThat(presenterImpl.model.getResourceName(), is(CHANGED_RESOURCE));
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {
        createAndInitializePresenterImpl();

        presenterImpl.keyPressed();

        verify(mockedView, times(1)).setStatusText("");
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressed_pingIsCalled() {
        createAndInitializePresenterImpl();
        presenterImpl.saveButtonPressed();
        verify(mockedSinkServiceProxy, times(1)).ping(any(SinkModel.class), any(PresenterImpl.PingSinkServiceFilteredAsyncCallback.class));
    }

    @Test(expected = NullPointerException.class)
    public void getErrorText_callGetErrorTextWithNullException_throwsNullPointerException() {
        createAndInitializePresenterImpl();

        presenterImpl.getErrorText(null);
    }

    @Test
    public void getErrorText_callGetErrorTextWithNotAcceptableProxyException_returnsErrorStringOrNullString() {
        final String PROXY_KEY_VIOLATION_ERROR_TEXT = "Proxy Key Violation Error Text";
        final String PROXY_DATA_VALIDATION_ERROR_TEXT = "Proxy Data Validation Error Text";

        createAndInitializePresenterImpl();

        when(mockedTexts.error_ProxyKeyViolationError()).thenReturn(PROXY_KEY_VIOLATION_ERROR_TEXT);
        when(mockedTexts.error_ProxyDataValidationError()).thenReturn(PROXY_DATA_VALIDATION_ERROR_TEXT);

        // Empty Proxy Exception
        assertThat(presenterImpl.getErrorText(new ProxyException()), is(nullValue()));

        // Proxy Exception instantiated with null String
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.SERVICE_NOT_FOUND, (String) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.BAD_REQUEST, (String) null)), is(PROXY_DATA_VALIDATION_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.NOT_ACCEPTABLE, (String) null)), is(PROXY_KEY_VIOLATION_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.ENTITY_NOT_FOUND, (String) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.CONFLICT_ERROR, (String) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.INTERNAL_SERVER_ERROR, (String) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, (String) null)), is(nullValue()));

        // Proxy Exception instantiated with null Throwable
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.SERVICE_NOT_FOUND, (Throwable) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.BAD_REQUEST, (Throwable) null)), is(PROXY_DATA_VALIDATION_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.NOT_ACCEPTABLE, (Throwable) null)), is(PROXY_KEY_VIOLATION_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.ENTITY_NOT_FOUND, (Throwable) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.CONFLICT_ERROR, (Throwable) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.INTERNAL_SERVER_ERROR, (Throwable) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, (Throwable) null)), is(nullValue()));
    }

    @Test
    public void getErrorText_callGetErrorTextWithProxyExceptionWithCorrectProxyError_returnsPredefinedErrorTexts() {
        final String PROXY_ERROR_TEXT = "Proxy Error Text";
        final String PROXY_KEY_VIOLATION_ERROR_TEXT = "Proxy Key Violation Error Text";
        final String PROXY_DATA_VALIDATION_ERROR_TEXT = "Proxy Data Validation Error Text";

        createAndInitializePresenterImpl();

        when(mockedTexts.error_ProxyKeyViolationError()).thenReturn(PROXY_KEY_VIOLATION_ERROR_TEXT);
        when(mockedTexts.error_ProxyDataValidationError()).thenReturn(PROXY_DATA_VALIDATION_ERROR_TEXT);

        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.SERVICE_NOT_FOUND, PROXY_ERROR_TEXT)), is(PROXY_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.BAD_REQUEST, PROXY_ERROR_TEXT)), is(PROXY_DATA_VALIDATION_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.NOT_ACCEPTABLE, PROXY_ERROR_TEXT)), is(PROXY_KEY_VIOLATION_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.ENTITY_NOT_FOUND, PROXY_ERROR_TEXT)), is(PROXY_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.CONFLICT_ERROR, PROXY_ERROR_TEXT)), is(PROXY_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.INTERNAL_SERVER_ERROR, PROXY_ERROR_TEXT)), is(PROXY_ERROR_TEXT));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.MODEL_MAPPER_EMPTY_FIELDS, PROXY_ERROR_TEXT)), is(PROXY_ERROR_TEXT));
    }

    @Test
    public void getErrorText_callGetErrorTextWithNonProxyException_returnsExceptionErrorText() {
        final String EXCEPTION_ERROR_TEXT = "Exception Error Text";
        createAndInitializePresenterImpl();

        assertThat(presenterImpl.getErrorText(new IllegalArgumentException(EXCEPTION_ERROR_TEXT)), is(EXCEPTION_ERROR_TEXT));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_successfulCallbackStatusOk_saveModelIsCalled() {
        createAndInitializePresenterImpl();
        saveModelHasBeenCalled = false;
        presenterImpl.pingSinkCallback.onSuccess(new PingResponse(PingResponse.Status.OK, Arrays.asList("log")));
        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_successfulCallbackStatusFailed_setStatusTextCalledInView() {
        createAndInitializePresenterImpl();
        when(mockedTexts.error_ResourceNameNotValid()).thenReturn(FAILURE_TEXT);
        presenterImpl.pingSinkCallback.onSuccess(new PingResponse(PingResponse.Status.FAILED, Arrays.asList("log")));
        verify(mockedView, times(1)).setErrorText(FAILURE_TEXT);
    }

    @Test
    public void pingSinkServiceFilteredAsyncCallback_unsuccessfulCallback_setStatusTextCalledInView() {
        createAndInitializePresenterImpl();
        when(mockedTexts.error_PingCommunicationError()).thenReturn(FAILURE_TEXT);
        presenterImpl.pingSinkCallback.onFilteredFailure(new Throwable(FAILURE_TEXT));
        verify(mockedView, times(1)).setErrorText(FAILURE_TEXT);
    }

    @Test
    public void saveSinkModelFilteredAsyncCallback_successfulCallback_setStatusTextCalledInView() {
        createAndInitializePresenterImpl();
        when(mockedTexts.status_SinkSuccessfullySaved()).thenReturn(SUCCESS_TEXT);
        presenterImpl.saveSinkCallback.onSuccess(new SinkModel());
        verify(mockedView, times(1)).setStatusText(SUCCESS_TEXT);
    }

    @Test
    public void sinkModelFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {
        createAndInitializePresenterImpl();
        presenterImpl.saveSinkCallback.onFailure(new Throwable(FAILURE_TEXT));
        verify(mockedView, times(1)).setErrorText(FAILURE_TEXT);
    }


    /*
     * Private methods
     */
    private void createAndInitializePresenterImpl() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedTexts);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
    }

}