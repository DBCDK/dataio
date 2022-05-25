package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;


public class PromptedRadioButtons extends PromptedData implements HasValue<String>, HasValueChangeHandlers<String> {
    ValueChangeHandler<String> valueChangeHandler = null;  // This is package private because of test - should be private
    private String radioButtonName = "Name";
    private boolean vertical = true;

    @UiField
    final FlowPanel containerPanel;

    /**
     * UI Constructor - carries the parameters to be used by UI Binder
     *
     * @param guiId  The GUI Id to be used for this particular DOM element
     * @param prompt The Prompt text to be used for the List Box
     */
    public @UiConstructor PromptedRadioButtons(String guiId, String prompt) {
        super(guiId, prompt);
        radioButtonName = guiId + radioButtonName;
        containerPanel = GWT.create(FlowPanel.class);
        containerPanel.addStyleName(PromptedData.PROMPTED_DATA_DATA_CLASS);
        add(containerPanel);
    }

    /**
     * UI Child - allows child elements under the "button" element. <br>
     * Furthermore, an attribute named "value" is allowed in the "button" element <br>
     * In UI Binder, use the following format for the PromptedRadioButtons: <br>
     * <pre>
     * {@code
     *   <dio:PromptedRadioButtons guiId="sinktypeselection" prompt="Sink type">
     *      <dio:item value="ES_SINK_TYPE"><g:Label>ES sink</g:Label></dio:item>
     *      <dio:item value="UPDATE_SINK_TYPE"><g:Label>Update sink</g:Label></dio:item>
     *      <dio:item value="DUMMY_SINK_TYPE"><g:Label>Dummy sink</g:Label></dio:item>
     *   </dio:PromptedRadioButtons>
     * }
     * </pre>
     *
     * @param text  The containing text in the "button" element
     * @param value The value of the "value" attribute
     */
    @UiChild(tagname = "button")
    public void addButton(Label text, String value) {
        addButton(new RadioButton(radioButtonName, text.getText()), value);
    }

    /**
     * A testable version of the above method with injection of the radio button
     *
     * @param radioButton The instance of the RadioButton to add
     * @param value       The value of the "value" attribute
     */
    void addButton(RadioButton radioButton, String value) {
        value = value == null ? "" : value;
        setOrientationOnWidget(radioButton);
        setValueAttributeOfRadioButton(radioButton, value);
        containerPanel.add(radioButton);
        radioButton.addValueChangeHandler(event -> fireChangeEvent(getValueAttributeOfRadioButton((RadioButton) event.getSource())));
    }

    /**
     * Sets the orientation of the radio buttons.
     * Must be used upon initialization - after initialization, the method has no effect.
     *
     * @param orientation The string "vertical" or "horizontal" sets the orientation of the radio buttons
     */
    public void setOrientation(String orientation) {
        if (orientation == null) {
            return;
        }
        vertical = orientation.toLowerCase().equals("vertical");
        if (containerPanel != null) {
            for (int i = 0; i < containerPanel.getWidgetCount(); i++) {
                setOrientationOnWidget(containerPanel.getWidget(i));
            }
        }
    }

    /**
     * Clears the selection in the radio boxes - hereafter no selection is active
     */
    @Override
    public void clear() {
        if (containerPanel != null) {
            for (int i = 0; i < containerPanel.getWidgetCount(); i++) {
                ((RadioButton) containerPanel.getWidget(i)).setValue(false);
            }
        }
    }

    /**
     * Fetches the selected item from the list box.
     * The string returned is the value attribute, associated with the RadioButton
     * If there is no selection, an empty string is returned
     *
     * @return The value of the selected RadioButton
     */
    public String getValue() {
        if (containerPanel != null) {
            for (int i = 0; i < containerPanel.getWidgetCount(); i++) {
                RadioButton radioButton = getRadioButton(i);
                if (radioButton != null && radioButton.getValue()) {
                    return getValueAttributeOfRadioButton(radioButton);
                }
            }
        }
        return "";
    }

    /**
     * Sets the selection of the listbox. Use the radio button value to point out the selection.<br>
     * Do not fire a ChangeEvent
     *
     * @param value The expected value of the RadioButton to select. If not found, no selection is done
     */
    public void setValue(String value) {
        if (containerPanel != null) {
            for (int i = 0; i < containerPanel.getWidgetCount(); i++) {
                RadioButton radioButton = getRadioButton(i);
                if (radioButton != null) {
                    radioButton.setValue(getValueAttributeOfRadioButton(radioButton).equals(value));
                }
            }
        }
    }

    /**
     * Sets the selection of the listbox. Use the radio button value to point out the selection.<br>
     * If the supplied boolean is true, do also fire a ChangeEvent
     *
     * @param value      The displayed text of the item to be selected
     * @param fireEvents If true, do fire a ChangeEvent
     */
    @Override
    public void setValue(String value, boolean fireEvents) {
        setValue(value);
        if (fireEvents) {
            ValueChangeEvent.fire(this, value);
        }
    }

    /**
     * Adds a ValueChangeHandler to the PromptedRadioButtons component
     *
     * @param changeHandler The ValueChangeHandler to add
     * @return A HandlerRegistration object to be used to remove the change handler
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> changeHandler) {
        valueChangeHandler = changeHandler;
        return () -> valueChangeHandler = null;
    }


    /*
     * Private methods
     */

    private void setOrientationOnWidget(Widget radioButton) {
        radioButton.getElement().getStyle().setProperty("display", vertical ? "block" : "inline");
    }

    private RadioButton getRadioButton(int i) {
        return (RadioButton) containerPanel.getWidget(i);
    }

    private void fireChangeEvent(String value) {
        valueChangeHandler.onValueChange(new ValueChangeEvent<String>(value) {
        });
    }

    private void setValueAttributeOfRadioButton(RadioButton radioButton, String value) {
        if (radioButton != null && radioButton.getElement() != null && radioButton.getElement().getFirstChildElement() != null) {
            radioButton.getElement().getFirstChildElement().setAttribute("value", value);
        }
    }

    private String getValueAttributeOfRadioButton(RadioButton radioButton) {
        if (radioButton != null && radioButton.getElement() != null && radioButton.getElement().getFirstChildElement() != null) {
            return radioButton.getElement().getFirstChildElement().getAttribute("value");
        }
        return "";
    }

}
