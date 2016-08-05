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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
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
    @Mock KeyPressEvent mockedKeyPressEvent;
    @Mock TextBox mockedWidget;
    @Mock FlowPanel mockedBasePanel;
    @Mock DialogBox mockedDialogBox;
    @Mock VerticalPanel mockedContainerPanel;
    @Mock FlowPanel mockedButtonPanel;
    @Mock Button mockedOkButton;
    @Mock Button mockedCancelButton;
    @Mock Button mockedExtraButton;
    @Mock ClickEvent mockedClickEvent;
    @Mock ValueChangeHandler mockedValueChangeHandler;
    @SuppressWarnings("deprecation")
    @Mock com.google.gwt.user.client.Element mockedElement;

    /**
     * Subject Under Test
     */
    PopupValueBox popupValueBox;


    /**
     * Tests starts here...
     */

    @Test
    public void constructor_instantiate_instantiatedCorrectly() {
        // Activate Subject Under Test
        setupTest();

        // Test verification
        constructorVerification();
        noMoreMockInvocations();
    }

    @Test
    public void setDialogTitle_nullValue_exception() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setDialogTitle(null);

        // Test Verification
        verify(popupValueBox.dialogBox).setText(null);
        noMoreMockInvocations();
    }

    @Test
    public void setDialogTitle_emptyValue_buttonIsDisabled() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setDialogTitle("");

        // Test Verification
        verify(popupValueBox.dialogBox).setText("");
        noMoreMockInvocations();
    }

    @Test
    public void setDialogTitle_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setDialogTitle("-Dialog-Title-");

        // Test Verification
        verify(popupValueBox.dialogBox).setText("-Dialog-Title-");
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setOkButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.setOkButtonText(null);
    }

    @Test
    public void setOkButtonText_emptyValue_buttonIsDisabled() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setOkButtonText("");

        // Test Verification
        verify(mockedOkButton).setEnabled(false);
        verify(mockedOkButton).setVisible(false);
        noMoreMockInvocations();
    }

    @Test
    public void setOkButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setOkButtonText("-Ok-Button-Text-");

        // Test Verification
        verify(mockedOkButton).setText("-Ok-Button-Text-");
        verify(mockedOkButton, times(2)).setEnabled(true);
        verify(mockedOkButton, times(2)).setVisible(true);
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setCancelButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.setCancelButtonText(null);
    }

    @Test
    public void setCancelButtonText_emptyValue_buttonIsDisabled() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setCancelButtonText("");

        // Test Verification
        verify(mockedCancelButton, times(2)).setEnabled(false);
        verify(mockedCancelButton, times(2)).setVisible(false);
        noMoreMockInvocations();
    }

    @Test
    public void setCancelButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setCancelButtonText("-Cancel-Button-Text-");

        // Test Verification
        verify(mockedCancelButton).setText("-Cancel-Button-Text-");
        verify(mockedCancelButton).setEnabled(true);
        verify(mockedCancelButton).setVisible(true);
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setExtraButtonText_nullValue_exception() {
        // Test Preparation
        setupTest();

        // Activate Subject Under Test
        popupValueBox.setExtraButtonText(null);
    }

    @Test
    public void setExtraButtonText_emptyValue_buttonIsDisabled() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setExtraButtonText("");

        // Test Verification
        verify(mockedExtraButton, times(2)).setEnabled(false);
        verify(mockedExtraButton, times(2)).setVisible(false);
        noMoreMockInvocations();
    }

    @Test
    public void setExtraButtonText_validValue_textIsSetCorrectly() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setExtraButtonText("-Extra-Button-Text-");

        // Test Verification
        verify(mockedExtraButton).setText("-Extra-Button-Text-");
        verify(mockedExtraButton).setEnabled(true);
        verify(mockedExtraButton).setVisible(true);
        noMoreMockInvocations();
    }

    @Test(expected = NullPointerException.class)
    public void setGuid_nullValue_exception() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setGuid(null);
    }

    @Test
    public void setGuid_emptyValue_noAction() {
        // Test Preparation
        setupTest();
        constructorVerification();
        when(mockedDialogBox.getElement()).thenReturn(mockedElement);

        // Activate Subject Under Test
        popupValueBox.setGuid("");

        // Test Verification
        noMoreMockInvocations();
    }

    @Test
    public void setGuid_validValue_setGuid() {
        // Test Preparation
        setupTest();
        constructorVerification();
        when(mockedDialogBox.getElement()).thenReturn(mockedElement);

        // Activate Subject Under Test
        popupValueBox.setGuid("gui ID");

        // Test Verification
        verify(mockedDialogBox).getElement();
        verify(mockedElement).setId("gui ID");
        noMoreMockInvocations();
    }

    @Test
    public void okClickHandler_noValueChangeHandler_onlyHide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.okClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void okClickHandler_validValueChangeHandler_bothHideAndTriggerEvent() {
        // Test Preparation
        setupTest();
        constructorVerification();
        popupValueBox.addValueChangeHandler(mockedValueChangeHandler);

        // Activate Subject Under Test
        popupValueBox.okClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        verify(mockedWidget).getValue();
        verify(mockedValueChangeHandler).onValueChange(any(ValueChangeEvent.class));
        noMoreMockInvocations();
    }

    @Test
    public void cancelClickHandler_call_hide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.cancelClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void extraClickHandler_call_hide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.extraClickHandler(mockedClickEvent);

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void show_call_show() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.show();

        // Test Verification
        verify(mockedWidget, times(2)).setValue(null);
        verify(mockedDialogBox, times(2)).center();
        verify(mockedDialogBox, times(2)).show();
        verify(mockedWidget, times(2)).setFocus(true);
        noMoreMockInvocations();
    }

    @Test
    public void hide_call_hide() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.hide();

        // Test Verification
        verify(mockedDialogBox, times(2)).hide();
        noMoreMockInvocations();
    }

    @Test
    public void getValue_call_getTheValue() {
        // Test Preparation
        setupTest();
        constructorVerification();
        when(mockedWidget.getValue()).thenReturn("correct value");

        // Activate Subject Under Test
        String value = (String) popupValueBox.getValue();

        // Test Verification
        assertThat(value, is("correct value"));
        verify(mockedWidget).getValue();
        noMoreMockInvocations();
    }

    @Test
    public void setValue_call_newValueSet() {
        // Test Preparation
        setupTest();
        constructorVerification();

        // Activate Subject Under Test
        popupValueBox.setValue("new value");

        // Test Verification
        verify(mockedWidget).setValue("new value");
        noMoreMockInvocations();
    }

    @Test
    public void setValue_callWithFireEvent_newValueSetAndFireEvent() {
        // Test Preparation
        setupTest();
        constructorVerification();
        popupValueBox.addValueChangeHandler(mockedValueChangeHandler);

        // Activate Subject Under Test
        popupValueBox.setValue("new value", true);

        // Test Verification
        verify(mockedWidget).setValue("new value", true);
        verify(mockedDialogBox).hide();
        verify(mockedWidget).getValue();
        verify(mockedValueChangeHandler).onValueChange(any(ValueChangeEvent.class));
        noMoreMockInvocations();
    }

    @Test
    public void addValueChangeHandler_removeHandler_handlerRemoved() {
        // Test Preparation
        setupTest();
        constructorVerification();
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
        popupValueBox = new PopupValueBox(
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

    private void constructorVerification() {
        verify(mockedDialogBox).setText("Dialog Title");
        verify(mockedOkButton).setText("Ok Button Text");
        verify(mockedOkButton).setEnabled(true);
        verify(mockedOkButton).setVisible(true);
        verify(mockedCancelButton).setEnabled(false);
        verify(mockedCancelButton).setVisible(false);
        verify(mockedExtraButton).setEnabled(false);
        verify(mockedExtraButton).setVisible(false);
        verify(mockedOkButton).addClickHandler(any(ClickHandler.class));
        verify(mockedCancelButton).addClickHandler(any(ClickHandler.class));
        verify(mockedExtraButton).addClickHandler(any(ClickHandler.class));
        verify(mockedButtonPanel).add(mockedOkButton);
        verify(mockedButtonPanel).add(mockedCancelButton);
        verify(mockedButtonPanel).add(mockedExtraButton);
        verify(mockedContainerPanel).add(mockedWidget);
        verify(mockedContainerPanel).add(mockedButtonPanel);
        verify(mockedDialogBox).add(mockedContainerPanel);
        verify(mockedBasePanel).add(mockedDialogBox);
        verify(mockedDialogBox).addStyleName("dio-PopupValueBox");
        verify(mockedDialogBox).setAutoHideEnabled(true);
        verify(mockedDialogBox).setModal(true);
        verify(mockedDialogBox).isAnimationEnabled();
        verify(mockedDialogBox, times(2)).setAnimationEnabled(false);
        verify(mockedWidget).setValue(null);
        verify(mockedDialogBox).center();
        verify(mockedDialogBox).show();
        verify(mockedWidget).setFocus(true);
        verify(mockedDialogBox).hide();
    }

    private void noMoreMockInvocations() {
        verifyNoMoreInteractions(mockedDialogBox);
        verifyNoMoreInteractions(mockedOkButton);
        verifyNoMoreInteractions(mockedCancelButton);
        verifyNoMoreInteractions(mockedExtraButton);
        verifyNoMoreInteractions(mockedButtonPanel);
        verifyNoMoreInteractions(mockedContainerPanel);
        verifyNoMoreInteractions(mockedBasePanel);
        verifyNoMoreInteractions(mockedWidget);
        verifyNoMoreInteractions(mockedKeyPressEvent);
        verifyNoMoreInteractions(mockedElement);
    }

}
