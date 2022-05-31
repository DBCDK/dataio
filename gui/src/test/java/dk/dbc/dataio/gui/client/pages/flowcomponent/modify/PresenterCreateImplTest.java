package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
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
 * PresenterCreateImpl unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterCreateImplTest extends PresenterImplTestBase {

    @Mock
    private Texts mockedTexts;
    @Mock
    dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock
    ViewGinjector mockedViewInjector;

    private View createView;

    private PresenterCreateImpl presenterCreateImpl;

    @Before
    public void setupView() {
        createView = new View();  // GwtMockito automagically populates mocked versions of all UiFields in the view
        when(mockedCommonGinjector.getFlowStoreProxyAsync()).thenReturn(mockedFlowStore);
        when(mockedViewInjector.getView()).thenReturn(createView);
        when(mockedViewInjector.getTexts()).thenReturn(mockedTexts);
        when(mockedCommonGinjector.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowComponentCreation()).thenReturn("Header Text");
    }


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        setupPresenter();
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly
    }


    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {

        // Setup
        setupPresenter();
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getName(), is(""));

        // Subject Under Test
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // Verifications
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getName(), is(""));
        assertThat(presenterCreateImpl.model.getSvnProject(), is(""));
        assertThat(presenterCreateImpl.model.getSvnRevision(), is(""));
        assertThat(presenterCreateImpl.model.getInvocationJavascript(), is(""));
        assertThat(presenterCreateImpl.model.getInvocationMethod(), is(""));
        assertThat(presenterCreateImpl.model.getJavascriptModules(), is(notNullValue()));
        assertThat(presenterCreateImpl.model.getJavascriptModules().isEmpty(), is(true));
    }

    @Test
    public void saveModel_flowComponentOk_createFlowComponentCalled() throws Throwable {

        // Setup
        setupPresenter();
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);
        presenterCreateImpl.model = new FlowComponentModel();

        // Subject Under Test
        presenterCreateImpl.saveModel();

        // Verifications
        verify(mockedCommonGinjector.getFlowStoreProxyAsync()).createFlowComponent(eq(presenterCreateImpl.model), any(PresenterImpl.SaveFlowComponentModelFilteredAsyncCallback.class));
    }

    private void setupPresenter() {
        presenterCreateImpl = new PresenterCreateImpl(header);
        presenterCreateImpl.commonInjector = mockedCommonGinjector;
        presenterCreateImpl.viewInjector = mockedViewInjector;
    }
}
