package dk.dbc.dataio.gui.client.components;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Popup box for entering a text string in a popup window
 */
public class PopupTextBox extends Composite implements HasValue<String> {
    interface PopupTextBoxUiBinder extends UiBinder<HTMLPanel, PopupTextBox> {}
    private static PopupTextBoxUiBinder ourUiBinder = GWT.create(PopupTextBoxUiBinder.class);
    ValueChangeHandler<String> valueChangeHandler = null;  // This is package private because of test - should be private


    /**
     * Ui Fields
     */
    @UiField DialogBox dialogBox;
    @UiField TextBox text;
    @UiField Button okButton;
    @UiField Button cancelButton;


    /**
     * Constructor
     *
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param cancelButtonText The text to be displayed in the Cancel Button (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    @UiConstructor
    public PopupTextBox(String dialogTitle, String okButtonText, String cancelButtonText) {
        initWidget(ourUiBinder.createAndBindUi(this));
        dialogBox.setText(dialogTitle);
        setButton(okButton, okButtonText);
        setButton(cancelButton, cancelButtonText);
        boolean animationEnabled = dialogBox.isAnimationEnabled();
        dialogBox.setAnimationEnabled(false);  // Assure, that the dialogbox will not blink upon startup when executing the next two statements...
        dialogBox.setAutoHideEnabled(true);
        dialogBox.setModal(true);
        show();  // First show the DialogBox in order to add it to the DOM
        hide();  // ... but we don't want it shown upon startup - so hide it again
        if (animationEnabled) {
            dialogBox.setAnimationEnabled(true);
        }
    }

    /**
     * Ui Handler Methods
     */

    /**
     * Ok button Click Handler
     *
     * @param clickEvent The click event for the OK Button
     */
    @UiHandler("okButton")
    public void okClickHandler(ClickEvent clickEvent) {
        hide();
        triggerValueChangeEvent();
    }

    /**
     * Cancel button Click Handler
     *
     * @param clickEvent The click event for the Cancel Button
     */
    @UiHandler("cancelButton")
    public void cancelClickHandler(ClickEvent clickEvent) {
        hide();
    }


    /*
     * Public methods
     */

    /**
     * Shows the popup and attach it to the page. It must have a child widget before this method is called.
     */

    public void show() {
        text.setValue("");
        dialogBox.center();
        dialogBox.show();
        text.setFocus(true);
    }

    /**
     * Hides the popup and detaches it from the page. This has no effect if it is not currently showing.
     */
    public void hide() {
        dialogBox.hide();
    }


    /*
     * HasValue overrides
     */

    /**
     * Gets the value of the text entered in the text box
     *
     * @return The value of the text
     */
    @Override
    public String getValue() {
        return text.getValue();
    }

    /**
     * Sets the value of the text entered in the text box
     *
     * @param value The new value of the text to set
     */
    @Override
    public void setValue(String value) {
        text.setValue(value);
    }

    /**
     * Sets the value of the text entered in the text box
     *
     * @param value      The new value of the text to set
     * @param fireEvents Determines whether to fire an event
     */
    @Override
    public void setValue(String value, boolean fireEvents) {
        text.setValue(value);
        if (fireEvents) {
            triggerValueChangeEvent();
        }
    }

    /**
     * Adds an event handler to be fired upon change of the text in the text box
     *
     * @param changeHandler The Value Change Handler to be fired upon changes
     * @return A Handler Registration object
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> changeHandler) {
        valueChangeHandler = changeHandler;
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                valueChangeHandler = null;
            }
        };
    }


    /*
     * Private methods
     */

    /**
     * Prepares a button on the dialog box
     * @param button The button to prepare
     * @param buttonText The text to display on the button
     */
    private void setButton(Button button, String buttonText) {
        if (buttonText.isEmpty()) {
            button.setVisible(false);
        } else {
            button.setText(buttonText);
            button.setEnabled(true);
            button.setVisible(true);
        }
    }

    private void triggerValueChangeEvent() {
        if (valueChangeHandler != null) {
            valueChangeHandler.onValueChange(new ValueChangeEvent<String>(getValue()) {});
        }
    }


}