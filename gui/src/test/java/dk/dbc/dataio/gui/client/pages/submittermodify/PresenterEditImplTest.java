package dk.dbc.dataio.gui.client.pages.submittermodify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PresenterEditImplTest {
    private ClientFactory mockedClientFactory;
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    private SubmitterModifyConstants mockedConstants;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedEditView;
    private EditPlace mockedEditPlace;

    private PresenterEditImpl presenterEditImpl;

    private final static String NUMBER_INPUT_FIELD_VALIDATION_ERROR = "NumberInputFieldValidationError";

    private PresenterEditImplConcrete presenterEditImplConcrete;

    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, ClientFactory clientFactory, SubmitterModifyConstants constants) {
            super(place, clientFactory, constants);
            view = mockedEditView;
        }

        public GetSubmitterModelFilteredAsyncCallback callback = new GetSubmitterModelFilteredAsyncCallback();
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
        mockedEditView = mock(View.class);
        mockedEditPlace = mock(EditPlace.class);
        when(mockedClientFactory.getSubmitterEditView()).thenReturn(mockedEditView);
        when(mockedConstants.error_NumberInputFieldValidationError()).thenReturn(NUMBER_INPUT_FIELD_VALIDATION_ERROR);
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedConstants);
        verify(mockedEditPlace, times(1)).getSubmitterId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_getSubmitterIsInvoked() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedConstants);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedFlowStoreProxy, times(1)).getSubmitter(any(Long.class), any(PresenterEditImpl.SaveSubmitterModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_modelValidationError_errorTextIsDisplayedOnView() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedConstants);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SubmitterModel();

        presenterEditImpl.numberChanged("a");                 // Number must be a valid number
        presenterEditImpl.nameChanged("name");                // Name is ok
        presenterEditImpl.descriptionChanged("description");  // Description is ok
        presenterEditImpl.saveModel();

        verify(mockedEditView, times(1)).setErrorText(NUMBER_INPUT_FIELD_VALIDATION_ERROR);
    }

    @Test
    public void saveModel_submitterContentOk_updateSubmitterCalled() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedConstants);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SubmitterModel();

        presenterEditImpl.numberChanged("1");                 // Number is ok
        presenterEditImpl.nameChanged("a");                   // Name is ok
        presenterEditImpl.descriptionChanged("description");  // Description is ok

        presenterEditImpl.saveModel();

        verify(mockedFlowStoreProxy, times(1)).updateSubmitter(eq(presenterEditImpl.model), any(PresenterImpl.SaveSubmitterModelFilteredAsyncCallback.class));
    }

    @Test
    public void getSubmitterModelFilteredAsyncCallback_successfulCallback_modelIsInitializedCorrectly() {
        presenterEditImplConcrete = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedConstants);
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);
        SubmitterModel submitterModel = new SubmitterModel(4453, 1L, "7466", "Name", "Description");

        assertThat(presenterEditImplConcrete.model, is(nullValue())); // Assert that the model has not yet been initialized

        presenterEditImplConcrete.callback.onSuccess(submitterModel);  // Emulate a successful callback from flowstore

        // Assert that the submitter model has been updated correctly
        assertThat(presenterEditImplConcrete.model, is(notNullValue()));
        assertThat(presenterEditImplConcrete.model.getId(), is(submitterModel.getId()));
        assertThat(presenterEditImplConcrete.model.getVersion(), is(submitterModel.getVersion()));
        assertThat(presenterEditImplConcrete.model.getNumber(), is(submitterModel.getNumber()));
        assertThat(presenterEditImplConcrete.model.getName(), is(submitterModel.getName()));
        assertThat(presenterEditImplConcrete.model.getDescription(), is(submitterModel.getDescription()));

        // Assert that the view is displaying the correct values
        verify(mockedEditView, times(1)).setNumber(submitterModel.getNumber());
        verify(mockedEditView, times(1)).setName(submitterModel.getName());
        verify(mockedEditView, times(1)).setDescription(submitterModel.getDescription());
    }

    @Test
    public void getSubmitterModelFilteredAsyncCallback_unsuccessfulCallback_setErrorTextCalledInView() {
        presenterEditImplConcrete = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedConstants);
        presenterEditImplConcrete.start(mockedContainerWidget, mockedEventBus);

        // Emulate an unsuccessful callback from flowstore
        presenterEditImplConcrete.callback.onFailure(new Throwable(mockedConstants.error_CannotFetchSubmitter()));
        // Expect the error text to be set in View
        verify(mockedEditView, times(1)).setErrorText(mockedConstants.error_CannotFetchSubmitter());
    }

}
