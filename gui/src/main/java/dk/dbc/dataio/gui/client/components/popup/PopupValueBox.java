package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * <p>Popup value box for displaying a modifiable widget in a popup window - in this context,
 * a modifiable widget is a widget, that embeds a value, that can be changed (ie. implements HasValue)</p>
 * <p>The popup value box extends PopupBox, and therefore is displayed and handled as such.</p>
 *
 * @param <W> The class defining the widget to be embedded (eg. TextBox)
 * @param <T> The class defining the type of the entered value (eg. String)
 */
public class PopupValueBox<W extends HasValue<T> & IsWidget & Focusable, T> extends PopupBox<W> implements HasValue<T> {

    /**
     * Default constructor
     *
     * @param widget       The widget to be embedded in a Popup Box
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    public PopupValueBox(W widget, String dialogTitle, String okButtonText) {
        super(widget, dialogTitle, okButtonText);
    }

    /**
     * Default constructor
     *
     * @param widget The widget to be embedded in a Popup Box
     */
    public PopupValueBox(W widget) {
        super(widget);
    }

    /**
     * Constructor (with component injections - to be used for testing)
     * The Constructor is package scoped - not public
     *
     * @param widget         The widget to be embedded in a Popup Box
     * @param dialogTitle    The title text to display on the Dialog Box (mandatory)
     * @param okButtonText   The text to be displayed in the OK Button (mandatory)
     * @param basePanel      Basepanel to be used to embed the Dialog
     * @param dialogBox      The Dialog Box component
     * @param containerPanel The Container panel to embed the widgets
     * @param buttonPanel    The button container panel to embed the buttons
     * @param okButton       The Ok Button
     * @param cancelButton   The Cancel Button
     * @param extraButton    The Extra Button
     */
    PopupValueBox(W widget,
                  String dialogTitle,
                  String okButtonText,
                  FlowPanel basePanel,
                  DialogBox dialogBox,
                  VerticalPanel containerPanel,
                  FlowPanel buttonPanel,
                  Button okButton,
                  Button cancelButton,
                  Button extraButton) {
        super(widget, dialogTitle, okButtonText, basePanel, dialogBox, containerPanel, buttonPanel, okButton, cancelButton, extraButton);
    }

    /*
     * Public methods
     */

    /**
     * Shows the popup and attach it to the page. It must have a child widget before this method is called.
     */
    public void show() {
        widget.setValue(null);
        super.show();
        widget.setFocus(true);
    }


    /*
     * HasValue overrides
     */

    /**
     * Gets the value of the data entered in the widget
     *
     * @return The value of the widget
     */
    @Override
    public T getValue() {
        return widget.getValue();
    }

    /**
     * Sets the value of the text entered in the text box
     *
     * @param value The new value of the text to set
     */
    @Override
    public void setValue(T value) {
        widget.setValue(value);
    }

    /**
     * Sets the value of the text entered in the text box
     *
     * @param value      The new value of the text to set
     * @param fireEvents Determines whether to fire an event
     */
    @Override
    public void setValue(T value, boolean fireEvents) {
        widget.setValue(value, fireEvents);
    }

    /**
     * Adds an event handler to be fired upon change of the text in the text box
     *
     * @param changeHandler The Value Change Handler to be fired upon changes
     * @return A Handler Registration object
     */
    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<T> changeHandler) {
        return widget.addValueChangeHandler(changeHandler);
    }

}
