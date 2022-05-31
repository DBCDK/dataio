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
 * <p>
 * +-----------+----------+-------------------------+
 * | -first-   | -second- | -remainder-             |
 * +-----------+----------+-------------------------+
 * <p>
 * It is possible to set a value for the complete bar - the Max value
 * Then, by setting firstValue and secondValue, the length of these bars
 * are calculated correspondingly.
 * The color of the first bar is Green, the second one is Blue, and the
 * remainder is gray.
 * <p>
 * A text is overlayed on the bars, if set.
 */
public class MultiProgressBar extends Composite {
    interface MultiProgressBarUiBinder extends UiBinder<HTMLPanel, MultiProgressBar> {
    }

    private static MultiProgressBarUiBinder ourUiBinder = GWT.create(MultiProgressBarUiBinder.class);

    @UiField
    public Label textProgress;      // Public to allow unit test
    @UiField
    public Element firstProgress;   // Public to allow unit test
    @UiField
    public Element secondProgress;  // Public to allow unit test

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
     *
     * @param text Text to be displayen on the Progress Bar
     */
    public void setText(String text) {
        textProgress.setText(text);
    }

    /**
     * Sets the first value for the Progress Bar
     *
     * @param value The first value of the Progress Bar
     */
    public void setFirstValue(String value) {
        firstProgress.setAttribute("value", value);
    }

    /**
     * Sets the second value for the Progress Bar
     *
     * @param value The second value of the Progress Bar
     */
    public void setSecondValue(String value) {
        secondProgress.setAttribute("value", value);
    }

    /**
     * Sets the max value for the Progress Bar.
     *
     * @param value The max value for the Progress Bar
     */
    public void setMaxValue(String value) {
        firstProgress.setAttribute("max", value);
        secondProgress.setAttribute("max", value);
    }

}
