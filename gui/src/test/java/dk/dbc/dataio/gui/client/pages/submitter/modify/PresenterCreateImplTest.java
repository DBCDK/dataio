package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.modelBuilders.SubmitterModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterCreateImplTest extends PresenterImplTestBase {
    @Mock
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock
    private Texts mockedTexts;
    @Mock
    ViewGinjector mockedViewKGinjector;
    private View createView;

    private PresenterCreateImpl presenterCreateImpl;

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupView() {
        createView = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedViewKGinjector.getView()).thenReturn(createView);
        when(mockedViewKGinjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_SubmitterCreation()).thenReturn("Header Text");
    }


    //------------------------------------------------------------------------------------------------------------------

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenterCreateImpl();
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly
//        verify(mockedClientFactory).getSubmitterCreateView();
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {

        // Setup
        setupPresenterCreateImpl();

        // Subject Under Test
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getName(), is(""));
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // Verifications
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getNumber(), is(""));
        assertThat(presenterCreateImpl.model.getName(), is(""));
        assertThat(presenterCreateImpl.model.getDescription(), is(""));
        assertThat(presenterCreateImpl.model.isEnabled(), is(true));
    }

    @Test
    public void saveModel_submitterContentOk_createSubmitterCalled() {

        // Setup
        setupPresenterCreateImpl();
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);
        presenterCreateImpl.model = new SubmitterModelBuilder().build();

        // Subject Under Test
        presenterCreateImpl.saveModel();

        // Verifications
        verify(mockedFlowStoreProxy).createSubmitter(eq(presenterCreateImpl.model), any(PresenterImpl.SaveSubmitterModelFilteredAsyncCallback.class));
    }

    private void setupPresenterCreateImpl() {
        presenterCreateImpl = new PresenterCreateImpl(header);
        presenterCreateImpl.commonInjector = mockedCommonGinjector;
        presenterCreateImpl.viewInjector = mockedViewKGinjector;
    }
}
