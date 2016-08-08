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

package dk.dbc.dataio.gui.client.components;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PopupValueBox unit tests
 * <p/>
 * The test methods of this class uses the following naming convention:
 * <p/>
 * unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PopupValueBoxTest {
    @Mock TextBox mockedWidget;
    @Mock FlowPanel mockedBasePanel;
    @Mock DialogBox mockedDialogBox;
    @Mock VerticalPanel mockedContainerPanel;
    @Mock FlowPanel mockedButtonPanel;
    @Mock Button mockedOkButton;
    @Mock Button mockedCancelButton;
    @Mock Button mockedExtraButton;
    @Mock ValueChangeHandler mockedValueChangeHandler;

    /**
     * Subject Under Test
     */
    PopupValueBox<TextBox, String> popupValueBox;


    /**
     * Tests starts here...
     */

    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        setupTest();

        // Test verification
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }


    @Test
    public void show_call_show() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.show();

        // Test Verification
        verify(mockedWidget, times(2)).setValue(null);
        verify(mockedWidget, times(2)).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void getValue_call_getTheValue() {
        // Test Preparation
        setupTest();
        when(mockedWidget.getValue()).thenReturn("correct value");

        // Activate Subject Under Test
        String value = (String) popupValueBox.getValue();

        // Test Verification
        assertThat(value, is("correct value"));
        verify(mockedWidget).getValue();
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void setValue_call_newValueSet() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.setValue("new value");

        // Test Verification
        verify(mockedWidget).setValue("new value");
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void setValue_callWithFireEvent_newValueSetAndFireEvent() {
        // Test Preparation
        setupTest();
        popupValueBox.addValueChangeHandler(mockedValueChangeHandler);

        // Activate Subject Under Test
        popupValueBox.setValue("new value", true);

        // Test Verification
        verify(mockedWidget).setValue("new value", true);
        verify(mockedWidget).setValue(null);
        verify(mockedWidget).setFocus(true);
        verify(mockedWidget).getValue();
        verify(mockedValueChangeHandler).onValueChange(any(ValueChangeEvent.class));
        verifyNoMoreInteractions(mockedWidget);
    }

    @Test
    public void addValueChangeHandler_removeHandler_handlerRemoved() {
        // Test Preparation
        setupTest();
        HandlerRegistration registration = popupValueBox.addValueChangeHandler(mockedValueChangeHandler);
        assertThat(popupValueBox.valueChangeHandler, is(notNullValue()));

        // Activate Subject Under Test
        registration.removeHandler();

        // Test Verification
        assertThat(popupValueBox.valueChangeHandler, is(nullValue()));
    }


    /**
     * Private methods
     */

    private void setupTest() {
        popupValueBox = new PopupValueBox<TextBox, String>(
                mockedWidget,
                "Dialog Title",
                "Ok Button Text",
                mockedBasePanel,
                mockedDialogBox,
                mockedContainerPanel,
                mockedButtonPanel,
                mockedOkButton,
                mockedCancelButton,
                mockedExtraButton);
    }

}
