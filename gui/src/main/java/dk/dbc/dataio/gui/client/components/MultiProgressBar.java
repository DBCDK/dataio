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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * This class implements a Multi Progress Bar component
 * The component includes two progress values in the same bar.
 * Like this:
 *
 *   +-----------+----------+-------------------------+
 *   | -first-   | -second- | -remainder-             |
 *   +-----------+----------+-------------------------+
 *
 *   It is possible to set a value for the complete bar - the Max value
 *   Then, by setting firstValue and secondValue, the length of these bars
 *   are calculated correspondingly.
 *   The color of the first bar is Green, the second one is Blue, and the
 *   remainder is gray.
 *
 *   A text is overlayed on the bars, if set.
 */
public class MultiProgressBar extends Composite {
    interface MultiProgressBarUiBinder extends UiBinder<HTMLPanel, MultiProgressBar> {
    }

    private static MultiProgressBarUiBinder ourUiBinder = GWT.create(MultiProgressBarUiBinder.class);

    @UiField public Label textProgress;      // Public to allow unit test
    @UiField public Element firstProgress;   // Public to allow unit test
    @UiField public Element secondProgress;  // Public to allow unit test

    public MultiProgressBar() {
        this("", "0", "0", "100");
    }

    @UiConstructor
    public MultiProgressBar(String text, String firstValue, String secondValue, String max) {
        initWidget(ourUiBinder.createAndBindUi(this));
        setText(text);
        setFirstValue(firstValue);
        setSecondValue(secondValue);
        setMaxValue(max);
    }

    /**
     * Sets the text to be displayed on top of, and in the center of the Progress Bar
     * @param text Text to be displayen on the Progress Bar
     */
    public void setText(String text) {
        textProgress.setText(text);
    }

    /**
     * Sets the first value for the Progress Bar
     * @param value The first value of the Progress Bar
     */
    public void setFirstValue(String value) {
        firstProgress.setAttribute("value", value);
    }

    /**
     * Sets the second value for the Progress Bar
     * @param value The second value of the Progress Bar
     */
    public void setSecondValue(String value) {
        secondProgress.setAttribute("value", value);
    }

    /**
     * Sets the max value for the Progress Bar.
     * @param value The max value for the Progress Bar
     */
    public void setMaxValue(String value) {
        firstProgress.setAttribute("max", value);
        secondProgress.setAttribute("max", value);
    }

}