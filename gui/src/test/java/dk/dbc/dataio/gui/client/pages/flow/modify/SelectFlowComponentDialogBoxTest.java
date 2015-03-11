package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * SelectFlowComponentDialogBox unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class SelectFlowComponentDialogBoxTest {
    private final static String   FLOW_COMPONENT_ID_1 = "111";
    private final static String FLOW_COMPONENT_NAME_1 = "FlowComponentName1";
    private final static String   FLOW_COMPONENT_ID_2 = "222";
    private final static String FLOW_COMPONENT_NAME_2 = "FlowComponentName2";
    private final static String   FLOW_COMPONENT_ID_3 = "333";
    private final static String FLOW_COMPONENT_NAME_3 = "FlowomponentName3";
    private final static String   FLOW_COMPONENT_ID_4 = "444";
    private final static String FLOW_COMPONENT_NAME_4 = "FlowComponentName4";

    private SelectFlowComponentDialogBox selectFlowComponentDialogBoxUnderTest;

    @Mock ClickHandler mockedClickHandler;


    //------------------------------------------------------------------------------------------------------------------

//    @Before
//    public void setupMockedObjects() {
//
//    }

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructor_instantiate_objectCorrectInitialized() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox();

        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).setGlassEnabled(true);
        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).setAnimationEnabled(true);
    }

    @Test
    public void activateDialogBox_callActivateDialogBox_dialogBoxIsShown() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox();
        Map<String, String> availableTestFlowComponents = produceTestFlowComponents();

        selectFlowComponentDialogBoxUnderTest.activateDialogBox(availableTestFlowComponents);

        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_1, FLOW_COMPONENT_NAME_1);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_2, FLOW_COMPONENT_NAME_2);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_3, FLOW_COMPONENT_NAME_3);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_4, FLOW_COMPONENT_NAME_4);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).setVisibleItemCount(4);
        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).center();
        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).show();
    }

    @Test
    public void addClickHandler_callAddClickHandler_clickHandlerAdded() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox();
        assertThat(selectFlowComponentDialogBoxUnderTest.selectButtonClickHandler, is((ClickHandler) null));

        selectFlowComponentDialogBoxUnderTest.addClickHandler(mockedClickHandler);

        assertThat(selectFlowComponentDialogBoxUnderTest.selectButtonClickHandler, is(mockedClickHandler));
    }

    @Test
    public void removeClickHandler_callRemoveInHandlerRegistration_clickHandlerRemoved() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox();
        HandlerRegistration registration = selectFlowComponentDialogBoxUnderTest.addClickHandler(mockedClickHandler);

        registration.removeHandler();

        assertThat(selectFlowComponentDialogBoxUnderTest.selectButtonClickHandler, is((ClickHandler) null));
    }

    //------------------------------------------------------------------------------------------------------------------

    private Map<String, String> produceTestFlowComponents() {
        Map<String, String> data = new LinkedHashMap<String, String>();
        data.put(FLOW_COMPONENT_ID_1, FLOW_COMPONENT_NAME_1);
        data.put(FLOW_COMPONENT_ID_2, FLOW_COMPONENT_NAME_2);
        data.put(FLOW_COMPONENT_ID_3, FLOW_COMPONENT_NAME_3);
        data.put(FLOW_COMPONENT_ID_4, FLOW_COMPONENT_NAME_4);
        return data;
    }

}