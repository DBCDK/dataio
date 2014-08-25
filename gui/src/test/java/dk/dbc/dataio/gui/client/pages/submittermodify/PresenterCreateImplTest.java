
package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;

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
public class PresenterCreateImplTest {
    private ClientFactory mockedClientFactory;
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    private SubmitterModifyConstants mockedConstants;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedCreateView;

    private PresenterCreateImpl presenterCreateImpl;

    private final static String INPUT_FIELD_VALIDATION_ERROR = "InputFieldValidationError";
    private final static String NUMBER_INPUT_FIELD_VALIDATION_ERROR = "NumberInputFieldValidationError";
    private final static String PROXY_DATA_VALIDATION_ERROR = "ProxyDataValidationError";
    private final static String PROXY_KEY_VIOLATION_ERROR = "ProxyKeyViolationError";


    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        mockedClientFactory = mock(ClientFactory.class);
        mockedFlowStoreProxy = mock(FlowStoreProxyAsync.class);
        mockedConstants = mock(SubmitterModifyConstants.class);
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedCreateView = mock(View.class);
        when(mockedClientFactory.getSubmitterCreateView()).thenReturn(mockedCreateView);
        when(mockedConstants.error_InputFieldValidationError()).thenReturn(INPUT_FIELD_VALIDATION_ERROR);
        when(mockedConstants.error_NumberInputFieldValidationError()).thenReturn(NUMBER_INPUT_FIELD_VALIDATION_ERROR);
        when(mockedConstants.error_ProxyDataValidationError()).thenReturn(PROXY_DATA_VALIDATION_ERROR);
        when(mockedConstants.error_ProxyKeyViolationError()).thenReturn(PROXY_KEY_VIOLATION_ERROR);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedConstants);
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly
        // Since we cannot access the protected view attribute in presenterCreateImpl, we can only test, that no exceptions are thrown
        // We must therefore rely on the following tests to assure, that view has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedConstants);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized to an empty model - and this is exactly what we would like to verify
        verify(mockedCreateView, times(1)).setNumber("");
        verify(mockedCreateView, times(1)).setName("");
        verify(mockedCreateView, times(1)).setDescription("");
    }

    @Test
    public void saveModel_modelValidationError_errorTextIsDisplayedOnView() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedConstants);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);

        presenterCreateImpl.numberChanged("a");                 // Number must be a valid number
        presenterCreateImpl.nameChanged("name");                // Name is ok
        presenterCreateImpl.descriptionChanged("description");  // Description is ok
        presenterCreateImpl.saveModel();

        verify(mockedCreateView, times(1)).setErrorText(NUMBER_INPUT_FIELD_VALIDATION_ERROR);
    }

    @Test
    public void saveModel_submitterContentNumberLowerBoundError_errorTextIsDisplayedOnView() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedConstants);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);

        presenterCreateImpl.numberChanged("0");                 // Number must be larger than 0
        presenterCreateImpl.nameChanged("name");                // Name is ok
        presenterCreateImpl.descriptionChanged("description");  // Description is ok
        presenterCreateImpl.saveModel();

        verify(mockedCreateView, times(1)).setErrorText("Value of parameter 'number' must be larger than or equal to 1");
    }

    @Test
    public void saveModel_submitterContentNameEmptyError_errorTextIsDisplayedOnView() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedConstants);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);

        presenterCreateImpl.numberChanged("1");                 // Number is ok
        presenterCreateImpl.nameChanged("");                    // Name must not be empty
        presenterCreateImpl.descriptionChanged("description");  // Description is ok
        presenterCreateImpl.saveModel();

        verify(mockedCreateView, times(1)).setErrorText("Value of parameter 'name' cannot be empty");
    }

    @Test
    public void saveModel_submitterContentDescriptionEmptyError_errorTextIsDisplayedOnView() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedConstants);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);

        presenterCreateImpl.numberChanged("1");                 // Number is ok
        presenterCreateImpl.nameChanged("name");                // Name is ok
        presenterCreateImpl.descriptionChanged("");             // Description must not be empty
        presenterCreateImpl.saveModel();

        verify(mockedCreateView, times(1)).setErrorText("Value of parameter 'description' cannot be empty");
    }

//    @Test
//    public void saveModel_submitterContentOk_xxx() {
//        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedConstants);
//        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);
//
//        FilteredAsyncCallback<Submitter>   // Hmmm.... Todo: Den nuværende version af Flowstore bruger Submitter og SubmitterContent. Denne er ved at blive lavet om, og er færdig om en halv time. Derfor tjekker jeg denne version ind uden at færdiggøre Happy Path'en af saveModel() metoden...
//
//        presenterCreateImpl.numberChanged("1");                 // Number is ok
//        presenterCreateImpl.nameChanged("name");                // Name is ok
//        presenterCreateImpl.descriptionChanged("description");  // Description must not be empty
//        presenterCreateImpl.saveModel();
//
//    }

}