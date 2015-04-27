package dk.dbc.dataio.gui.client.pages.flow.modify;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    @Mock ClientFactory mockedClientFactory;
    @Mock FlowStoreProxyAsync mockedFlowStoreProxy;
    @Mock Texts mockedTexts;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;

    private View view;

    private PresenterCreateImpl presenterCreateImpl;

    private final static String INPUT_FIELD_VALIDATION_ERROR = "InputFieldValidationError";

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        when(mockedClientFactory.getFlowCreateView()).thenReturn(view);
        when(mockedTexts.error_InputFieldValidationError()).thenReturn(INPUT_FIELD_VALIDATION_ERROR);
    }

    @Before
    public void setupView() {
        view = new View("Header Text");  // GwtMockito automagically populates mocked versions of all UiFields in the view
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory);
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly

        verify(mockedClientFactory).getFlowCreateView();
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        // Verify, that the model is cleared and updated accordingly
        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getFlowName(), is(""));
        assertThat(presenterCreateImpl.model.getDescription(), is(""));
        assertThat(presenterCreateImpl.model.getFlowComponents().size(), is(0));

        // Verify, that the view is updated accordingly
        verify(view.name).setText("");
        verify(view.name).setEnabled(true);
        verify(view.description).setText("");
        verify(view.description).setEnabled(true);
        verify(view.flowComponents, times(2)).clear();
        verify(view.flowComponents).setEnabled(false);
        verifyNoMoreInteractions(view.flowComponents);  // To make sure, that there are no addValue() calls
    }

    @Test
    public void saveModel_flowContentOk_createFlowCalled() {
        final String FLOW_NAME = "flow name";
        final String DESCRIPTION = "description";
        final long FLOW_COMPONENT_ID = 534;
        final String FLOW_COMPONENT_NAME = "flow component name";

        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);

        presenterCreateImpl.nameChanged(FLOW_NAME);
        presenterCreateImpl.descriptionChanged(DESCRIPTION);
        Map<String, String> flowComponents = new HashMap<String, String>();
        flowComponents.put(String.valueOf(FLOW_COMPONENT_ID), FLOW_COMPONENT_NAME);
        presenterCreateImpl.availableFlowComponentModels = Arrays.asList(new FlowComponentModel(FLOW_COMPONENT_ID, 1L, FLOW_COMPONENT_NAME, "", "", "", "", new ArrayList<String>()));
        presenterCreateImpl.flowComponentsChanged(flowComponents);

        presenterCreateImpl.saveModel();

        verify(mockedFlowStoreProxy).createFlow(eq(presenterCreateImpl.model), any(PresenterImpl.SaveFlowModelAsyncCallback.class));
    }

}