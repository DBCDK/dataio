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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 *
 * @author slf
 */
public class SaveButton extends FlowPanel {
    public static final String SAVE_BUTTON_CLASS = "dio-SaveButton";
    public static final String SAVE_BUTTON_RESULT_LABEL_CLASS = "dio-SaveButton-ResultLabel";
    public static final String SAVE_BUTTON_BUTTON_CLASS = "dio-SaveButton-Button";

    /**
     * Event interface for signalling button pressed event
     */
    public interface ButtonEvent {
        void buttonPressed();
    }
    
    /**
     * Implementation of button handler for key press
     */
    private class SaveButtonHandler implements ClickHandler {
        @Override
        public void onClick(ClickEvent event) {
            saveButtonEvent.buttonPressed();
        }
    }
    
    // Instance variables
    private final Button saveButton = new Button("");
    private final Label resultLabel = new Label("");
    private ButtonEvent saveButtonEvent = null;

   /**
     * Constructor
     *
     * @param guiId the id of the gui element
     * @param buttonText buttonText The text to be displayed on the save button
     * @param saveButtonEvent The callback event interface, that gets called upon press on the save button
     */
    public SaveButton(String guiId, String buttonText, ButtonEvent saveButtonEvent) {
        this.saveButtonEvent = saveButtonEvent;
        getElement().setId(guiId);
        resultLabel.setStylePrimaryName(SAVE_BUTTON_RESULT_LABEL_CLASS);
        add(resultLabel);
        saveButton.setText(buttonText);
        saveButton.addStyleName(SAVE_BUTTON_BUTTON_CLASS);
        saveButton.addClickHandler(new SaveButtonHandler());
        add(saveButton);
    }

    /**
     * Sets the text, displaying the status info (busy, completed etc.)
     * 
     * @param statusText the status text to set
     */
    public void setStatusText(String statusText) {
        resultLabel.setText(statusText);
    }

    
}
    
