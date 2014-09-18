package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PresenterCreateImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class PresenterCreateImplTest {
    private ClientFactory mockedClientFactory;
    private FlowStoreProxyAsync mockedFlowStoreProxy;
    private dk.dbc.dataio.gui.client.pages.flow.modify.Texts mockedTexts;
    private AcceptsOneWidget mockedContainerWidget;
    private EventBus mockedEventBus;
    private View mockedCreateView;

    private PresenterCreateImpl presenterCreateImpl;

    private final static String INPUT_FIELD_VALIDATION_ERROR = "InputFieldValidationError";

    //------------------------------------------------------------------------------------------------------------------

    @Before
    public void setupMockedObjects() {
        mockedClientFactory = mock(ClientFactory.class);
        mockedFlowStoreProxy = mock(FlowStoreProxyAsync.class);
        mockedTexts = mock(Texts.class);
        when(mockedClientFactory.getFlowStoreProxyAsync()).thenReturn(mockedFlowStoreProxy);
        mockedContainerWidget = mock(AcceptsOneWidget.class);
        mockedEventBus = mock(EventBus.class);
        mockedCreateView = mock(View.class);
        when(mockedClientFactory.getFlowCreateView()).thenReturn(mockedCreateView);
        when(mockedTexts.error_InputFieldValidationError()).thenReturn(INPUT_FIELD_VALIDATION_ERROR);
    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedTexts);
        // The instanitation of presenterCreateImpl instantiates the "Create version" of the presenter - and the basic test has been done in the test of PresenterImpl
        // Therefore, we only intend to test the Create specific stuff, which basically is to assert, that the view attribute has been initialized correctly

        verify(mockedClientFactory).getFlowCreateView();
    }

    @Test
    public void initializeModel_callPresenterStart_modelIsInitializedCorrectly() {
        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedTexts);
        assertThat(presenterCreateImpl.model, is(nullValue()));
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);  // Calls initializeModel

        assertThat(presenterCreateImpl.model, is(notNullValue()));
        assertThat(presenterCreateImpl.model.getFlowName(), is(""));
        assertThat(presenterCreateImpl.model.getDescription(), is(""));
        assertThat(presenterCreateImpl.model.getFlowComponents().size(), is(0));

        verify(mockedCreateView).setName(presenterCreateImpl.model.getFlowName());
        verify(mockedCreateView).setDescription(presenterCreateImpl.model.getDescription());
        verify(mockedCreateView).setAvailableFlowComponents(anyMap());
        verify(mockedCreateView).setSelectedFlowComponents(anyMap());
    }

    @Test
    public void saveModel_flowContentOk_createFlowCalled() {
        final String FLOW_NAME = "flow name";
        final String DESCRIPTION = "description";
        final long FLOW_COMPONENT_ID = 534;
        final String FLOW_COMPONENT_NAME = "flow component name";

        presenterCreateImpl = new PresenterCreateImpl(mockedClientFactory, mockedTexts);
        presenterCreateImpl.start(mockedContainerWidget, mockedEventBus);

        presenterCreateImpl.nameChanged(FLOW_NAME);
        presenterCreateImpl.descriptionChanged(DESCRIPTION);
        Map<String, String> flowComponents = new HashMap<String, String>();
        flowComponents.put(String.valueOf(FLOW_COMPONENT_ID), FLOW_COMPONENT_NAME);
        presenterCreateImpl.availableFlowComponentModels.add(new FlowComponentModel(FLOW_COMPONENT_ID, 1L, FLOW_COMPONENT_NAME, "", "", "", "", new ArrayList <String>()));
        presenterCreateImpl.flowComponentsChanged(flowComponents);

        presenterCreateImpl.saveModel();

        verify(mockedFlowStoreProxy).createFlow(eq(presenterCreateImpl.model), any(PresenterImpl.SaveFlowModelAsyncCallback.class));
    }

}