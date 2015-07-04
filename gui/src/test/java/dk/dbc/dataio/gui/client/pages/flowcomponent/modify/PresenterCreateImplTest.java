package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterCreateImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterCreateImplTest {

    @Mock private ClientFactory mockedClientFactory;
    @Mock private FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock private Texts mockedTexts;
    @Mock private AcceptsOneWidget mockedContainerWidget;
    @Mock private EventBus mockedEventBus;
    @Mock dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;

    private CreateView createView;

    private PresenterCreateImpl presenterCreateImpl;

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getFlowComponentCreateView()).thenReturn(createView);
        when(mockedClientFactory.getFlowComponentModifyTexts()).thenReturn(mockedTexts);
    }

    @Before
    public void setupView() {
        when(mockedClientFactory.getMenuTexts()).thenReturn(mockedMenuTexts);
        when(mockedMenuTexts.menu_FlowComponentCreation()).thenReturn("Header Text");
        createView = new CreateView(mockedClientFactory);  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory);
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly

        verify(mockedClientFactory).getFlowComponentCreateView();
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory);
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getName(), is(""));
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

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
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);
        presenterCreateImpl.model = new FlowComponentModel();
        presenterCreateImpl.saveModel();

        verify(mockedFlowStoreProxy).createFlowComponent(eq(presenterCreateImpl.model), any(PresenterImpl.SaveFlowComponentModelFilteredAsyncCallback.class));
    }
}
