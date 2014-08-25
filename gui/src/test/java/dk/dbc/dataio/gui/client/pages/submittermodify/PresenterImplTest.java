
package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
    private SubmitterModifyConstants mockedConstants;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedView;
    private SubmitterModel replicatedModel;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;

    private final static long DEFAULT_ID = 0;
    private final static long DEFAULT_VERSION = 0;
    private final static String DEFAULT_NUMBER = "123";
    private final static String DEFAULT_NAME = "Hello";
    private final static String DEFAULT_DESCRIPTION = "Note";
    private final static String EMPTY = "";

    class PresenterImplConcrete extends PresenterImpl {
        public PresenterImplConcrete(ClientFactory clientFactory, SubmitterModifyConstants constants) {
            super(clientFactory, constants);
            view = mockedView;
        }

        @Override
        void initializeModel() {
            model = new SubmitterModel(DEFAULT_ID, DEFAULT_VERSION, DEFAULT_NUMBER, DEFAULT_NAME, DEFAULT_DESCRIPTION);
            replicatedModel = model;  // Since model is not immutable, this is a reference, meaning that changes in model is reflected in replicatedModel
        }

        @Override
        void saveModel() {
            saveModelHasBeenCalled = true;
        }

        // Test method for reading flowStoreProxy
        public FlowStoreProxyAsync getFlowStoreProxy() {
            return flowStoreProxy;
        }

        // Test method for reading constants
        public SubmitterModifyConstants getSubmitterModifyConstants() {
            return constants;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        mockedClientFactory = mock(ClientFactory.class);
        mockedFlowStoreProxy = mock(FlowStoreProxyAsync.class);
        mockedConstants = mock(SubmitterModifyConstants.class);
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedView = mock(View.class);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        assertThat(presenterImpl.getFlowStoreProxy(), is(mockedFlowStoreProxy));
        assertThat(presenterImpl.getSubmitterModifyConstants(), is(mockedConstants));
    }

    @Test
    public void start_instantiateAndCallStart_objectCorrectInitializedAndViewAndModelInitializedCorrectly() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        verify(mockedView, times(1)).setPresenter(presenterImpl);
        verify(mockedView, times(1)).asWidget();
        verify(mockedContainerWidget, times(1)).setWidget(Matchers.any(IsWidget.class));
        verify(mockedView, times(1)).setNumber(DEFAULT_NUMBER);
        verify(mockedView, times(1)).setName(DEFAULT_NAME);
        verify(mockedView, times(1)).setDescription(DEFAULT_DESCRIPTION);
        verify(mockedView, times(1)).setStatusText(EMPTY);
        assertThat(replicatedModel.getId(), is(DEFAULT_ID));
        assertThat(replicatedModel.getVersion(), is(DEFAULT_VERSION));
        assertThat(replicatedModel.getNumber(), is(DEFAULT_NUMBER));
        assertThat(replicatedModel.getName(), is(DEFAULT_NAME));
        assertThat(replicatedModel.getDescription(), is(DEFAULT_DESCRIPTION));
    }

    @Test
    public void numberChanged_callNumber_numberIsChangedAccordingly() {
        final String CHANGED_NUMBER = "3435";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.numberChanged(CHANGED_NUMBER);

        assertThat(replicatedModel.getNumber(), is(CHANGED_NUMBER));
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "3435";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.nameChanged(CHANGED_NAME);

        assertThat(replicatedModel.getName(), is(CHANGED_NAME));
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "3435";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        assertThat(replicatedModel.getDescription(), is(CHANGED_DESCRIPTION));
    }

    @Test
    public void keyPressed_callKeyPressed_statusFieldIsCleared() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.keyPressed();

        verify(mockedView, times(2)).setStatusText("");  // Please note, that setStatusText is called once from both the start method and from keyPressed()
    }

    @Test
    public void saveButtonPressed_callSaveButtonPressed_saveModelIsCalled() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        saveModelHasBeenCalled = false;
        presenterImpl.saveButtonPressed();

        assertThat(saveModelHasBeenCalled, is(true));
    }

    @Test(expected = NullPointerException.class)
    public void getErrorText_callGetErrorTextWithNullException_throwsNullPointerException() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.getErrorText(null);
    }

    @Test
    public void getErrorText_callGetErrorTextWithNotAcceptableProxyException_returnsErrorStringOrNullString() {
        final String PROXY_ERROR_TEXT = "Proxy Error Text";
        final String PROXY_KEY_VIOLATION_ERROR_TEXT = "Proxy Key Violation Error Text";
        final String PROXY_DATA_VALIDATION_ERROR_TEXT = "Proxy Data Validation Error Text";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedConstants.error_ProxyKeyViolationError()).thenReturn(PROXY_KEY_VIOLATION_ERROR_TEXT);
        when(mockedConstants.error_ProxyDataValidationError()).thenReturn(PROXY_DATA_VALIDATION_ERROR_TEXT);

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

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        when(mockedConstants.error_ProxyKeyViolationError()).thenReturn(PROXY_KEY_VIOLATION_ERROR_TEXT);
        when(mockedConstants.error_ProxyDataValidationError()).thenReturn(PROXY_DATA_VALIDATION_ERROR_TEXT);

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
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        assertThat(presenterImpl.getErrorText(new IllegalArgumentException(EXCEPTION_ERROR_TEXT)), is(EXCEPTION_ERROR_TEXT));
    }

}