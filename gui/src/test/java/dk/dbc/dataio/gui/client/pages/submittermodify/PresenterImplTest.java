
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
    private SubmitterModel mockedModel;

    private PresenterImplConcrete presenterImpl;
    private static boolean saveModelHasBeenCalled;

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
            model = mockedModel;
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
        mockedModel = mock(SubmitterModel.class);
        when(mockedModel.getNumber()).thenReturn(DEFAULT_NUMBER);
        when(mockedModel.getName()).thenReturn(DEFAULT_NAME);
        when(mockedModel.getDescription()).thenReturn(DEFAULT_DESCRIPTION);
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
        verify(mockedModel, times(1)).getNumber();
        verify(mockedView, times(1)).setNumber(DEFAULT_NUMBER);
        verify(mockedModel, times(1)).getName();
        verify(mockedView, times(1)).setName(DEFAULT_NAME);
        verify(mockedModel, times(1)).getDescription();
        verify(mockedView, times(1)).setDescription(DEFAULT_DESCRIPTION);
        verify(mockedView, times(1)).setStatusText(EMPTY);
    }

    @Test
    public void numberChanged_callNumber_numberIsChangedAccordingly() {
        final String CHANGED_NUMBER = "3435";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.numberChanged(CHANGED_NUMBER);

        verify(mockedModel, times(1)).setNumber(CHANGED_NUMBER);
    }

    @Test
    public void nameChanged_callNameChanged_nameIsChangedAccordingly() {
        final String CHANGED_NAME = "3435";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.nameChanged(CHANGED_NAME);

        verify(mockedModel, times(1)).setName(CHANGED_NAME);
    }

    @Test
    public void descriptionChanged_callDescriptionChanged_descriptionIsChangedAccordingly() {
        final String CHANGED_DESCRIPTION = "3435";

        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        presenterImpl.descriptionChanged(CHANGED_DESCRIPTION);

        verify(mockedModel, times(1)).setDescription(CHANGED_DESCRIPTION);
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
    public void getErrorText_callGetErrorTextWithEmptyProxyException_returnsEmptyString() {
        presenterImpl = new PresenterImplConcrete(mockedClientFactory, mockedConstants);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        assertThat(presenterImpl.getErrorText(new ProxyException()), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.NOT_ACCEPTABLE, (String) null)), is(nullValue()));
        assertThat(presenterImpl.getErrorText(new ProxyException(ProxyError.CONFLICT_ERROR, (Throwable) null)), is(nullValue()));
    }

    @Test
    public void getErrorText_callGetErrorTextWithProxyExceptionWithProxyError_returnsPredefinedErrorTexts() {
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