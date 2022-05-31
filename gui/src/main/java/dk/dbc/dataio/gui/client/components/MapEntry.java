package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasValue;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;

import java.util.AbstractMap;
import java.util.Map;

/**
 * <p>This class implements a component, that allows the user to enter map of type Map.Entry&lt;key, value&gt;</p>
 * <p>The orientation on the display can be chosen between vertical and horisontal</p>
 */
public class MapEntry extends FlowPanel implements HasValue<Map.Entry<String, String>>, HasValueChangeHandlers<Map.Entry<String, String>>, Focusable {
    ValueChangeHandler<Map.Entry<String, String>> valueChangeHandler = null;

    @UiField
    final PromptedTextBox keyBox;
    @UiField
    final PromptedTextBox valueBox;

    /**
     * Constructor
     *
     * @param keyPrompt   The prompt text to be used for the key item
     * @param valuePrompt The prompt text to be used for the value item
     */
    @UiConstructor
    public MapEntry(String keyPrompt, String valuePrompt) {
        keyBox = GWT.create(PromptedTextBox.class);
        keyBox.setPrompt(keyPrompt);
        valueBox = GWT.create(PromptedTextBox.class);
        valueBox.setPrompt(valuePrompt);
        add(keyBox);
        add(valueBox);
    }


    /*
     * Public methods
     */

    /**
     * Sets the orientation of the two textboxes and their associated prompt labels
     *
     * @param orientation The string "vertical" or "horizontal" sets the orientation of the radio buttons
     */
    public void setOrientation(String orientation) {
        if (orientation == null) {
            return;
        }
        Boolean vertical = orientation.toLowerCase().equals("vertical");
        String promptStyle = vertical ? "non-stacked" : "stacked";
        keyBox.getElement().getStyle().setProperty("display", vertical ? "block" : "inline");
        keyBox.setPromptStyle(promptStyle);
        valueBox.getElement().getStyle().setProperty("display", vertical ? "block" : "inline");
        valueBox.setPromptStyle(promptStyle);
    }

    /**
     * Sets the Gui Id to be used to identify this component in the DOM
     *
     * @param guiId The Gui Id to identify this component in the DOM
     */
    public void setGuiId(String guiId) {
        getElement().setId(guiId);
    }


    /*
     * HasValue Overrides
     */

    /**
     * Gets the value of the component in the form of a Map.Entry
     *
     * @return The value of the key/value pair
     */
    @Override
    public Map.Entry<String, String> getValue() {
        return new AbstractMap.SimpleEntry<>(keyBox.getValue(), valueBox.getValue());
    }

    /**
     * Sets the value of the component in the form of a Map.Entry
     *
     * @param value The value of the key/value pair
     */
    @Override
    public void setValue(Map.Entry<String, String> value) {
        if (value == null) {
            value = new AbstractMap.SimpleEntry<>("", "");
        }
        setValue(value, false);
    }

    /**
     * Sets the value of the component in the form of a Map.Entry
     *
     * @param value     The value of the key/value pair
     * @param fireEvent If true: Fire a ValueChangeEvent
     */
    @Override
    public void setValue(Map.Entry<String, String> value, boolean fireEvent) {
        keyBox.setValue(value.getKey());
        valueBox.setValue(value.getValue());
        triggerValueChangeEvent(fireEvent);
    }

    /**
     * Adds a ValueChangeHandler to take action upon changes in the MapEntry component
     *
     * @param changeHandler A ValueChangeHandler to take action upon changes
     * @return A HandlerRegistration object, that allows deletion of the ValueChangeHandler
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Map.Entry<String, String>> changeHandler) {
        valueChangeHandler = changeHandler;
        keyBox.addValueChangeHandler(event -> triggerValueChangeEvent(true));
        valueBox.addValueChangeHandler(event -> triggerValueChangeEvent(true));
        return () -> valueChangeHandler = null;
    }


    /*
     * Focusable Overrides
     */


    /**
     * Gets the widget's position in the tab index
     * No implementation needed
     *
     * @return the widget's tab index
     */
    @Override
    public int getTabIndex() {
        return 0;
    }

    /**
     * Sets the widget's 'access key'. This key is used (in conjunction with a browser-specific modifier key) to automatically focus the widget.
     * No implementation needed
     *
     * @param key the widget's access key
     */
    @Override
    public void setAccessKey(char key) {
    }

    /**
     * Explicitly focus/unfocus this widget. Only one widget can have focus at a time, and the widget that does will receive all keyboard events.
     *
     * @param focused whether this widget should take focus or release it
     */
    @Override
    public void setFocus(boolean focused) {
        keyBox.setFocus(focused);
    }

    /**
     * Sets the widget's position in the tab index. If more than one widget has the same tab index, each such widget will receive focus in an arbitrary order. Setting the tab index to -1 will cause this widget to be removed from the tab order.
     * No implementation needed
     *
     * @param index the widget's tab index
     */
    @Override
    public void setTabIndex(int index) {
    }


    /*
     * Private methods
     */

    /**
     * Triggers a Value Change Event - but only if there is an eventhandler registered
     *
     * @param fireEvent Only fire event if fireEvent is true
     */
    private void triggerValueChangeEvent(boolean fireEvent) {
        if (valueChangeHandler != null && fireEvent) {
            valueChangeHandler.onValueChange(new ValueChangeEvent<Map.Entry<String, String>>(getValue()) {
            });
        }
    }

}
