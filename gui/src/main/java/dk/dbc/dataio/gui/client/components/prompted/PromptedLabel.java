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

import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Label;

/*
 * This class is a Label widget extended with a prompt label
 * The class extends the PromptedData class, that ensures a uniform presence
 */
public class PromptedLabel extends PromptedData {

    @UiField final Label label = new Label();


    /**
     * Constructor
     * This is the @UiConstructor, meaning that the two parameters are mandatory inputs, when used by UiBinder
     * @param guiId  The GUI Id
     * @param prompt The prompt label for the widgt
     */
    @UiConstructor
    public PromptedLabel(String guiId, String prompt) {
        super(guiId, prompt);
        label.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(label);
    }

    /**
     * Clears the label text
     */
    public void clearText() {
        label.setText("");
    }

    /**
     * Sets the text for the label
     * @param value The text value for the label
     */
    public void setText(String value) {
        label.setText(value);
    }

    /**
     * Fetches the text from the label
     * @return The text for the label
     */
    public String getText() {
        return label.getText();
    }

}
