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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasValue;

public class PromptedCheckBox extends PromptedData implements HasValue<Boolean> {

    @UiField final CheckBox checkBox = new CheckBox();


    /**
     * UiBinder Constructor
     * @param guiId The GUI Id to be set in the DOM
     * @param prompt The prompt text
     */
    public @UiConstructor
    PromptedCheckBox(String guiId, String prompt) {
        super(guiId, prompt);
        checkBox.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(checkBox);
    }

    /**
     * Gets the current boolean value for the CheckBox
     * @return The current value: True if selected, False if not
     */
    @Override
    public Boolean getValue() {
        return checkBox.getValue();
    }

    /**
     * Sets the value of the CheckBox
     * @param value The value to set for the CheckBox: True if selected, False if not
     */
    @Override
    public void setValue(Boolean value) {
        checkBox.setValue(value, false);
    }

    /**
     * Sets the value of the CheckBox, and fires an event, if the fireEvents parameter is set
     * @param value The value to set for the CheckBox: True if selected, False if not
     * @param fireEvents Fires an event, if set
     */
    @Override
    public void setValue(Boolean value, boolean fireEvents) {
        checkBox.setValue(value, fireEvents);
    }

    /**
     * Adds a {@link com.google.gwt.event.logical.shared.ValueChangeHandler} handler.
     *
     * @param handler the handler
     * @return the registration for the event
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
        return checkBox.addValueChangeHandler(handler);
    }

    /**
     * Enables or disables the CheckBox
     * @param enabled If true, the CheckBox is enabled, if false, the CheckBox is disabled
     */
    public void setEnabled(boolean enabled) {
        checkBox.setEnabled(enabled);
    }

    /**
     * Fires a change event
     */
    public void fireChangeEvent() {
        class TextBoxChangedEvent extends ChangeEvent {}
        checkBox.fireEvent(new TextBoxChangedEvent());
    }

}
