package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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

    private View view;
    private PresenterEditImpl presenterEditImpl;
    private final static long DEFAULT_SUBMITTER_ID = 426L;

    class PresenterEditImplConcrete extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, ClientFactory clientFactory, Texts constants) {
            super(place, clientFactory, constants);
        }

        public GetSubmitterModelFilteredAsyncCallback getSubmitterModelFilteredAsyncCallback = new GetSubmitterModelFilteredAsyncCallback();
    }
        //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getSubmitterEditView()).thenReturn(view);
        when(mockedEditPlace.getSubmitterId()).thenReturn(DEFAULT_SUBMITTER_ID);
    }

    @Before
    public void setupView() {
        view = new View("Header Text");  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    @After
    public void tearDownMockedData() {
        reset(mockedClientFactory);
        reset(mockedFlowStoreProxy);
        reset(mockedTexts);
        reset(mockedContainerWidget);
        reset(mockedEventBus);
        reset(mockedEditPlace);
        reset(view.number);
        reset(view.name);
        reset(view.description);
        reset(view.status);
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        verify(mockedEditPlace, times(1)).getSubmitterId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_getSubmitterIsInvoked() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedFlowStoreProxy).getSubmitter(any(Long.class), any(PresenterEditImpl.SaveSubmitterModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_submitterContentOk_updateSubmitterCalled() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SubmitterModel();

        presenterEditImpl.numberChanged("1");                 // Number is ok
        presenterEditImpl.nameChanged("a");                   // Name is ok
        presenterEditImpl.descriptionChanged("description");  // Description is ok

        presenterEditImpl.saveModel();

        verify(mockedFlowStoreProxy).updateSubmitter(eq(presenterEditImpl.model), any(PresenterImpl.SaveSubmitterModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_submitterContentInvalidNumber_updateSubmitterNotCalled() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SubmitterModel();

        presenterEditImpl.numberChanged("notANumber");            // Number is not ok
        presenterEditImpl.nameChanged("aNewName");                // Name is ok
        presenterEditImpl.descriptionChanged("aNewDescription");  // Description is ok

        presenterEditImpl.saveModel();
        assertThat(presenterEditImpl.model.isNumberValid(), is(false));
        verify(mockedFlowStoreProxy, times(0)).updateSubmitter(eq(presenterEditImpl.model), any(PresenterImpl.SaveSubmitterModelFilteredAsyncCallback.class));
    }

    @Test
    public void getSubmitterModelFilteredAsyncCallback_successfulCallback_modelUpdated() {

        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String SUBMITTER_NAME = "New Submitter Name";
        SubmitterModel submitterModel = new SubmitterModel();
        submitterModel.setName(SUBMITTER_NAME);

        presenterEditImpl.getSubmitterModelFilteredAsyncCallback.onSuccess(submitterModel);

        verify(view.name).setText(SUBMITTER_NAME);  // view is not mocked, but view.name is - we therefore do verify, that the model has been updated, by verifying view.name
    }

    @Test
    public void getSubmitterModelFilteredAsyncCallback_unSuccessfulCallback_errorMessage() {
        PresenterEditImplConcrete presenterEditImpl = new PresenterEditImplConcrete(mockedEditPlace, mockedClientFactory, mockedTexts);
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);

        presenterEditImpl.getSubmitterModelFilteredAsyncCallback.onFilteredFailure(new Throwable());

        verify(mockedTexts).error_CannotFetchSubmitter();  // We cannot verify view, since it is not mocked - however, we know, that the error text shall be fetched - and we therefore verify on that
    }

}
