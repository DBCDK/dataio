package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;

public class PromptedAnchorWithButton extends PromptedAnchor {
    private final String PROMPTED_ANCHOR_WITH_BUTTON_STYLE = "dio-PromptedAnchorWithButton";
    ValueChangeHandler<String> valueChangeHandler = null;

    /**
     * Constructor
     * This is the @UiConstructor, meaning that the two parameters are mandatory inputs, when used by UiBinder
     *
     * @param guiId  The GUI Id
     * @param prompt The prompt label for the widget
     */
    @UiConstructor
    public PromptedAnchorWithButton(String guiId, String prompt) {
        super(guiId, prompt);
    }

    @UiChild(tagname = "button")
    public void addButton(Label text, String value) {
        final Button button = new Button(text.getText());
        button.setStyleName(PROMPTED_ANCHOR_WITH_BUTTON_STYLE);
        addButton(button, value);
    }

    void addButton(Button button, String value) {
        value = value == null ? "" : value;
        final String finalValue = value;
        add(button);
        button.addClickHandler(event -> fireChangeEvent(finalValue));
    }

    private void fireChangeEvent(String value) {
        valueChangeHandler.onValueChange(new ValueChangeEvent<String>(value) {
        });
    }

    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> changeHandler) {
        valueChangeHandler = changeHandler;
        return () -> valueChangeHandler = null;
    }
}
