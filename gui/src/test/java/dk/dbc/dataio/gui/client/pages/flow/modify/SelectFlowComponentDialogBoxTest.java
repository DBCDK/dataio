/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.dom.client.ClickEvent;
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
    @Mock ClickHandler mockedClickHandler2;
    @Mock ClickEvent mockedClickEvent;
    @Mock Presenter mockedPresenter;

    //------------------------------------------------------------------------------------------------------------------


    @Test
    public void constructorWithEmptyList_instantiate_objectCorrectInitialized() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox(new LinkedHashMap<String, String>(), mockedClickHandler, mockedPresenter);

        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).setGlassEnabled(true);
        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).setAnimationEnabled(true);
    }

    @Test
    public void constructorWithNotEmptyList_instantiate_objectCorrectInitialized() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox(produceTestFlowComponents(), mockedClickHandler, mockedPresenter);

        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_1, FLOW_COMPONENT_NAME_1);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_2, FLOW_COMPONENT_NAME_2);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_3, FLOW_COMPONENT_NAME_3);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).addItem(FLOW_COMPONENT_ID_4, FLOW_COMPONENT_NAME_4);
        verify(selectFlowComponentDialogBoxUnderTest.flowComponentsList).setVisibleItemCount(4);
        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).center();
        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).show();
    }

    @Test
    public void addClickHandler_callAddClickHandler_newClickHandler() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox(new LinkedHashMap<String, String>(), mockedClickHandler, mockedPresenter);

        selectFlowComponentDialogBoxUnderTest.addClickHandler(mockedClickHandler2);

        assertThat(selectFlowComponentDialogBoxUnderTest.selectButtonClickHandler, is(mockedClickHandler2));
    }

    @Test
    public void removeClickHandler_callRemoveInHandlerRegistration_clickHandlerRemoved() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox(new LinkedHashMap<String, String>(), mockedClickHandler, mockedPresenter);
        HandlerRegistration registration = selectFlowComponentDialogBoxUnderTest.addClickHandler(mockedClickHandler2);

        registration.removeHandler();

        assertThat(selectFlowComponentDialogBoxUnderTest.selectButtonClickHandler, is((ClickHandler) null));
    }

    @Test
    public void uiHandlerSelectFlowComponent_callUiHandler_checkCorrectBehavior() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox(new LinkedHashMap<String, String>(), mockedClickHandler, mockedPresenter);
        selectFlowComponentDialogBoxUnderTest.addClickHandler(mockedClickHandler);

        selectFlowComponentDialogBoxUnderTest.selectFlowComponentButtonPressed(mockedClickEvent);

        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).hide();
        verify(mockedClickHandler).onClick(mockedClickEvent);
    }

    @Test
    public void uiHandlerCancel_callUiHandler_checkCorrectBehavior() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox(new LinkedHashMap<String, String>(), mockedClickHandler, mockedPresenter);

        selectFlowComponentDialogBoxUnderTest.cancelButtonPressed(mockedClickEvent);

        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).hide();
    }

    @Test
    public void uiNewFlowComponent_callUiHandler_checkCorrectBehavior() {
        selectFlowComponentDialogBoxUnderTest = new SelectFlowComponentDialogBox(new LinkedHashMap<String, String>(), mockedClickHandler, mockedPresenter);

        selectFlowComponentDialogBoxUnderTest.newFlowComponentButtonPressed(mockedClickEvent);

        verify(selectFlowComponentDialogBoxUnderTest.availableFlowComponentsDialog).hide();
        verify(mockedPresenter).newFlowComponentButtonPressed();
    }

    //------------------------------------------------------------------------------------------------------------------

    private Map<String, String> produceTestFlowComponents() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put(FLOW_COMPONENT_ID_1, FLOW_COMPONENT_NAME_1);
        data.put(FLOW_COMPONENT_ID_2, FLOW_COMPONENT_NAME_2);
        data.put(FLOW_COMPONENT_ID_3, FLOW_COMPONENT_NAME_3);
        data.put(FLOW_COMPONENT_ID_4, FLOW_COMPONENT_NAME_4);
        return data;
    }
}