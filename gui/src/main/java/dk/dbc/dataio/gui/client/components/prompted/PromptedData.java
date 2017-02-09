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

package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * A Super class for constructing prompted data entry
 */
public class PromptedData extends FlowPanel {
    public static final String PROMPTED_DATA_CLASS = "dio-PromptedData";
    public static final String PROMPTED_DATA_PROMPT_CLASS = "dio-PromptedData-PromptClass";
    public static final String PROMPTED_DATA_DATA_CLASS = "dio-PromptedData-DataClass";

    private final Label promptLabel = new Label();


    /**
     * Constructor
     * @param prompt The Prompt text for the component
     */
    public PromptedData(String prompt) {
        super();
        setPrompt(prompt);
        setStylePrimaryName(PROMPTED_DATA_CLASS);
        promptLabel.setStylePrimaryName(PROMPTED_DATA_PROMPT_CLASS);
        add(promptLabel);
    }

    /**
     * Constructor
     * @param guiId The Gui Id to identify this component in the DOM
     * @param prompt The Prompt text for the component
     */
    public PromptedData(String guiId, String prompt) {
        this(prompt);
        setGuiId(guiId);
    }

    /**
     * Sets the Gui Id to be used to identify this component in the DOM
     * @param guiId The Gui Id to identify this component in the DOM
     */
    public void setGuiId(String guiId) {
        getElement().setId(guiId);
    }

    /**
     * Gets the Gui Id to be used to identify this component in the DOM
     * @return The Gui Id to identify this component in the DOM
     */
    public String getGuiId() {
        return getElement().getId();
    }

    /**
     * Sets the Prompt text
     * @param prompt The Prompt text
     */
    public void setPrompt(String prompt) {
        promptLabel.setText(prompt);
    }

    /**
     * Gets the Prompt text
     * @return The Prompt text
     */
    public String getPrompt() {
        return promptLabel.getText();
    }

    /**
     * <p>Sets the Prompt Style to be used for this component</p>
     * <ul>
     *     <li>non-stacked (default): The prompt text is displayed to the left of the entry field</li>
     *     <li>stacked: The prompt text is display vertically on top of the entry field</li>
     * </ul>
     * @param promptStyle stacked or non-stacked
     */
    public void setPromptStyle(String promptStyle) {
        if (promptStyle.toLowerCase().equals("stacked")) {
            promptLabel.getElement().getStyle().setProperty("display", "block");
            getElement().getStyle().setProperty("float", "left");
        } else {
            promptLabel.getElement().getStyle().setProperty("display", "");
            getElement().getStyle().setProperty("float", "");
        }
    }

}
