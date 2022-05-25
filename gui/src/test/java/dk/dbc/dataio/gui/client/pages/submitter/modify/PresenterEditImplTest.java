package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.databinder.DataBinder;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class PresenterEditImplTest extends PresenterImplTestBase {
    @Mock
    private Texts mockedTexts;
    @Mock
    private EditPlace mockedEditPlace;
    @Mock
    private ViewGinjector mockedViewGinjector;
    @Mock
    private DataBinder mockedDataBinder;

    private View editView;
    private PresenterEditImpl presenterEditImpl;
    private final static long DEFAULT_SUBMITTER_ID = 426L;

    class PresenterEditImplConcrete<Place extends EditPlace> extends PresenterEditImpl {
        public PresenterEditImplConcrete(Place place, String header) {
            super(place, header);
            commonInjector = mockedCommonGinjector;
            viewInjector = mockedViewGinjector;
        }

        public GetSubmitterModelFilteredAsyncCallback getSubmitterModelFilteredAsyncCallback = new GetSubmitterModelFilteredAsyncCallback();
    }
    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {

        editView = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view

        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewGinjector.getView()).thenReturn(editView);
        when(mockedViewGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getProxyErrorTexts()).thenReturn(mockedProxyErrorTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedEditPlace.getSubmitterId()).thenReturn(DEFAULT_SUBMITTER_ID);
        when(mockedMenuTexts.menu_SubmitterEdit()).thenReturn("Header Text");
    }

    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterEditImpl();
        verify(mockedEditPlace, times(1)).getSubmitterId();
        // The instantiation of presenterEditImpl instantiates the "Edit version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Edit specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }

    @Test
    public void initializeModel_callPresenterStart_getSubmitterIsInvoked() {
        setupPresenterEditImpl();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel
        // initializeModel has the responsibility to setup the model in the presenter correctly
        // In this case, we expect the model to be initialized with the submitter values.
        verify(mockedFlowStore).getSubmitter(any(Long.class), any(PresenterEditImpl.GetSubmitterModelFilteredAsyncCallback.class));
    }

    @Test
    public void saveModel_submitterContentOk_updateSubmitterCalled() {
        setupPresenterEditImpl();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        presenterEditImpl.model = new SubmitterModel();
    }


    @Test
    public void getSubmitterModelFilteredAsyncCallback_successfulCallback_modelUpdated() {

        PresenterEditImplConcrete presenterEditImpl = setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        final String SUBMITTER_NAME = "New Submitter Name";
        SubmitterModel submitterModel = new SubmitterModel();
        submitterModel.setName(SUBMITTER_NAME);
        editView.name = mockedDataBinder;

        presenterEditImpl.getSubmitterModelFilteredAsyncCallback.onSuccess(submitterModel);

        verify(mockedDataBinder).setValue(SUBMITTER_NAME);
    }

    @Test
    public void getSubmitterModelFilteredAsyncCallback_unSuccessfulCallback_errorMessage() {
        PresenterEditImplConcrete presenterEditImpl = setupPresenterEditImplConcrete();
        presenterEditImpl.start(mockedContainerWidget, mockedEventBus);
        ProxyException mockedProxyException = mock(ProxyException.class);
        when(mockedProxyException.getErrorCode()).thenReturn(ProxyError.ENTITY_NOT_FOUND);

        presenterEditImpl.getSubmitterModelFilteredAsyncCallback.onFilteredFailure(mockedProxyException);

        verify(mockedProxyException).getErrorCode();
        verify(mockedProxyErrorTexts).flowStoreProxy_notFoundError();
    }

    private PresenterEditImplConcrete setupPresenterEditImplConcrete() {
        return new PresenterEditImplConcrete(mockedEditPlace, header);
    }

    private void setupPresenterEditImpl() {
        presenterEditImpl = new PresenterEditImpl(mockedEditPlace, header);
        presenterEditImpl.viewInjector = mockedViewGinjector;
        presenterEditImpl.commonInjector = mockedCommonGinjector;
    }
}
