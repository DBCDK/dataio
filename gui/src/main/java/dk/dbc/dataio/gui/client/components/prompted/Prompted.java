package dk.dbc.dataio.gui.client.components.prompted;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.UIObject;
import dk.dbc.dataio.gui.client.components.Tooltip;

/**
 * A Super class for constructing prompted data entry
 */
public class Prompted<T, W extends UIObject & IsWidget & HasValue<T> & HasEnabled & Focusable> extends FlowPanel implements HasValue<T>, IsWidget, HasEnabled, Focusable {
    public static final String PROMPTED_CLASS = "dio-Prompted";
    public static final String PROMPTED_PROMPT_CLASS = "dio-Prompted-Prompt";
    public static final String PROMPTED_DATA_CLASS = "dio-Prompted-Data";

    private final Label promptLabel = new Label();
    private W widget;

    /**
     * Constructor
     *
     * @param widget The contained widget
     */
    public Prompted(W widget) {
        super();
        this.widget = widget;
        setStylePrimaryName(PROMPTED_CLASS);
        widget.setStylePrimaryName(PROMPTED_DATA_CLASS);
        promptLabel.setStylePrimaryName(PROMPTED_PROMPT_CLASS);
        add(promptLabel);
        add(widget);
    }

    /**
     * Constructor
     *
     * @param widget The contained widget
     * @param prompt The prompt string
     */
    public Prompted(W widget, String prompt) {
        this(widget);
        setPrompt(prompt);
    }

    /**
     * Sets the Gui Id to be used to identify this component in the DOM
     *
     * @param guiId The Gui Id to identify this component in the DOM
     */
    public void setGuiId(String guiId) {
        getElement().setId(guiId);
    }

    /**
     * Gets the Gui Id to be used to identify this component in the DOM
     *
     * @return The Gui Id to identify this component in the DOM
     */
    public String getGuiId() {
        return getElement().getId();
    }

    /**
     * Sets the Prompt text
     *
     * @param prompt The Prompt text
     */
    public void setPrompt(String prompt) {
        promptLabel.setText(prompt);
    }

    /**
     * Gets the Prompt text
     *
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
     *
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

    /**
     * Sets the Max Length of the text box in number of characters that can be entered
     *
     * @param maxLength The Max Length of the text box
     */
    public void setMaxLength(String maxLength) {
        widget.getElement().setAttribute("Maxlength", String.valueOf(maxLength));
    }

    /**
     * Sets the Tool Tip to be used. Tool Tips will be shown, whenever the user hovers over the component
     *
     * @param toolTip The Tool Tip to be shown to the user
     */
    public void setToolTip(String toolTip) {
        if (!toolTip.isEmpty() && widget instanceof FocusWidget) {
            new Tooltip((FocusWidget) widget, toolTip);
        }
    }


    // Implementation of methods from the interface Focusable

    @Override
    public int getTabIndex() {
        return widget.getTabIndex();
    }

    @Override
    public void setAccessKey(char key) {
        widget.setAccessKey(key);
    }

    @Override
    public void setFocus(boolean focused) {
        widget.setFocus(focused);
    }

    @Override
    public void setTabIndex(int index) {
        widget.setTabIndex(index);
    }


    // Implementation of methods from the interface HasEnabled

    @Override
    public boolean isEnabled() {
        return widget.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        widget.setEnabled(enabled);
    }


    // Implementation of methods from the interface HasValue

    @Override
    public T getValue() {
        return widget.getValue();
    }

    @Override
    public void setValue(T value) {
        widget.setValue(value);
    }

    @Override
    public void setValue(T value, boolean fireEvents) {
        widget.setValue(value, fireEvents);
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler handler) {
        return widget.addValueChangeHandler(handler);
    }
}
