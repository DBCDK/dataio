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

import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.TextBox;

public class PromptedTextBox extends PromptedData implements HasValue<String> {

    @UiField final TextBox textBox = new TextBox();

    
    public PromptedTextBox(String guiId, String prompt, int maxLength) {
        this(guiId, prompt);
        textBox.getElement().setAttribute("Maxlength", String.valueOf(maxLength));
    }
    
    public PromptedTextBox(String guiId, String prompt) {
        super(guiId, prompt);
        textBox.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(textBox);
    }

    public @UiConstructor
    PromptedTextBox(String guiId, String prompt, String maxLength) {
        this(guiId, prompt);
        if (!maxLength.isEmpty()) {
            textBox.getElement().setAttribute("Maxlength", maxLength);
        }
    }

    public void setToolTip(String toolTip) {
        if (!toolTip.isEmpty()) {
            new Tooltip(textBox, toolTip);
        }

    }

    @Override
    public String getValue() {
        return textBox.getValue();
    }

    @Override
    public void setValue(String value) {
        textBox.setValue(value, false);
    }

    @Override
    public void setValue(String value, boolean fireEvents) {
        textBox.setValue(value, fireEvents);
    }

    /**
     * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeHandler} handler.
     *
     * @param handler the handler
     * @return the registration for the event
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler) {
        return textBox.addValueChangeHandler(handler);
    }

    public void clearText() {
        textBox.setText("");
    }
    
    public void setText(String value) {
        textBox.setText(value);
    }
    
    public String getText() {
        return textBox.getText();
    }

    public void setEnabled(boolean enabled) {
        textBox.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return textBox.isEnabled();
    }

    public void setFocus(boolean focused) {
        textBox.setFocus(focused);
    }

    public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
        return textBox.addKeyDownHandler(handler);
    }

    public void fireChangeEvent() {
        class TextBoxChangedEvent extends ChangeEvent {}
        textBox.fireEvent(new TextBoxChangedEvent());
    }
    
    public HandlerRegistration addChangeHandler(ChangeHandler handler) {
        return textBox.addChangeHandler(handler);
    }

    public HandlerRegistration addBlurHandler(BlurHandler handler){
        return textBox.addBlurHandler(handler);
    }
}
