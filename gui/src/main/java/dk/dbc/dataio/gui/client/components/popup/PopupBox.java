package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.events.DialogHandler;
import dk.dbc.dataio.gui.client.events.HasDialogHandlers;

/**
 * <p>Popup box for displaying a widget in a popup window </p>
 * <p>The popup box consist of a header, the widget itself, and up to three buttons:</p>
 *  <ul>
 *   <li>OK button</li>
 *   <li>Cancel button</li>
 *   <li>Extra button</li>
 *  </ul>
 * <p> All buttons have user definable labels</p>
 *
 * @param <W> The class defining the widget to be embedded (eg. TextBox)
 */
public class PopupBox<W extends IsWidget> extends Composite implements HasDialogHandlers {
    public static final String POPUP_BOX_GUID = "dio-PopupBox";

    DialogHandler dialogHandler = null;  // This is package private because of test - should be private

    FlowPanel basePanel;
    DialogBox dialogBox;
    VerticalPanel containerPanel;
    FlowPanel buttonPanel;
    Button okButton;
    Button cancelButton;
    Button extraButton;
    protected W widget;


    /**
     * Constructor
     *
     * @param widget       The widget to be embedded in a Popup Box
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    public PopupBox(W widget, String dialogTitle, String okButtonText) {
        this(widget, dialogTitle, okButtonText, new FlowPanel(), new DialogBox(), new VerticalPanel(), new FlowPanel(), new Button(), new Button(), new Button());
    }

    /**
     * Constructor
     *
     * @param widget The widget to be embedded in a Popup Box
     */
    public PopupBox(W widget) {
        this(widget, "", "", new FlowPanel(), new DialogBox(), new VerticalPanel(), new FlowPanel(), new Button(), new Button(), new Button());
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
    PopupBox(W widget,
             String dialogTitle,
             String okButtonText,
             FlowPanel basePanel,
             DialogBox dialogBox,
             VerticalPanel containerPanel,
             FlowPanel buttonPanel,
             Button okButton,
             Button cancelButton,
             Button extraButton) {
        // Data injection
        this.widget = widget;
        this.basePanel = basePanel;
        this.dialogBox = dialogBox;
        this.containerPanel = containerPanel;
        this.buttonPanel = buttonPanel;
        this.okButton = okButton;
        this.cancelButton = cancelButton;
        this.extraButton = extraButton;

        // Setup texts
        dialogBox.setText(dialogTitle);
        setButton(okButton, okButtonText);
        setButton(cancelButton, "");  // The Cancel Button is optional, so no text is added yet - meaning until that is done, it is not set
        setButton(extraButton, "");  // The Extra Button is optional, so no text is added yet - meaning until that is done, it is not set

        // Setup click event handlers for the buttons
        okButton.addClickHandler(this::okClickHandler);
        cancelButton.addClickHandler(this::cancelClickHandler);
        extraButton.addClickHandler(this::extraClickHandler);

        // Build the widget tree
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(extraButton);
        containerPanel.add(widget);
        containerPanel.add(buttonPanel);
        dialogBox.add(containerPanel);
        basePanel.add(dialogBox);
        initWidget(basePanel);

        // Set CSS Stuff
        dialogBox.addStyleName(POPUP_BOX_GUID);

        // Assure, that the dialogbox is included in the DOM
        dialogBox.setAutoHideEnabled(true);
        dialogBox.setModal(true);
        boolean animationEnabled = dialogBox.isAnimationEnabled();
        dialogBox.setAnimationEnabled(false);  // Assure, that the dialogbox will not blink upon startup when executing the next two statements...
        show();  // First show the DialogBox in order to add it to the DOM
        hide();  // ... but we don't want it shown upon startup - so hide it again
        dialogBox.setAnimationEnabled(animationEnabled);
    }

    /**
     * Sets the Title of the Dialog
     *
     * @param title The Dialog Title
     */
    public void setDialogTitle(String title) {
        dialogBox.setText(title);
    }

    /**
     * Sets the OK Button Text
     *
     * @param value The text to display on the button
     */
    public void setOkButtonText(String value) {
        setButton(okButton, value);
    }

    /**
     * Sets the Cancel Button Text
     * Optional setting in the UI Binder activation
     *
     * @param value The text to display on the button
     */
    public void setCancelButtonText(String value) {
        setButton(cancelButton, value);
    }

    /**
     * Sets the Extra Button Text
     * Optional setting in the UI Binder activation
     *
     * @param value The text to display on the button
     */
    public void setExtraButtonText(String value) {
        setButton(extraButton, value);
    }

    /**
     * Sets the Auto Hide feature. If Auto Hide is set true, the Popup box is removed, if the user
     * clicks outside the Popup box<br>
     * Default value is true.
     *
     * @param autoHide Auto hide true if a click outside the Popup box hides the Popup box
     */
    public void setAutoHide(Boolean autoHide) {
        dialogBox.setAutoHideEnabled(autoHide);
    }

    /**
     * Sets the Guid for this element
     * Optional setting in the UI Binder activation
     *
     * @param guid The Guid for this element
     */
    public void setGuid(String guid) {
        if (!guid.isEmpty()) {
            dialogBox.getElement().setId(guid);
        }
    }

    /**
     * Gets the Content Widget
     *
     * @return The Content Widget
     */
    public W getContentWidget() {
        return widget;
    }


    /*
     * Click Handlers
     */

    /**
     * Ok button Click Handler
     *
     * @param clickEvent The click event for the OK Button
     */
    public void okClickHandler(ClickEvent clickEvent) {
        triggerDialogEvent(DialogEvent.DialogButton.OK_BUTTON);
        hide();
    }

    /**
     * Cancel button Click Handler
     *
     * @param clickEvent The click event for the Cancel Button
     */
    public void cancelClickHandler(ClickEvent clickEvent) {
        triggerDialogEvent(DialogEvent.DialogButton.CANCEL_BUTTON);
        hide();
    }

    /**
     * Extra button Click Handler
     *
     * @param clickEvent The click event for the Cancel Button
     */
    public void extraClickHandler(ClickEvent clickEvent) {
        triggerDialogEvent(DialogEvent.DialogButton.EXTRA_BUTTON);
        hide();
    }


    /*
     * Public methods
     */

    /**
     * Shows the popup and attach it to the page. It must have a child widget before this method is called.
     */
    public void show() {
        dialogBox.center();
        dialogBox.show();
    }

    /**
     * Hides the popup and detaches it from the page. This has no effect if it is not currently showing.
     */
    public void hide() {
        dialogBox.hide();
    }


    /*
     * HasDialogHandlers overrides
     */

    /**
     * Adds a Dialog handler to be fired upon click on one of the three buttons
     *
     * @param handler The new DialogHandler to service
     * @return A Handler Registration object
     */
    @Override
    public HandlerRegistration addDialogHandler(DialogHandler handler) {
        dialogHandler = handler;
        return () -> dialogHandler = null;
    }

    /*
     * Private methods
     */

    /**
     * Prepares a button on the dialog box
     *
     * @param button     The button to prepare
     * @param buttonText The text to display on the button
     */
    private void setButton(Button button, String buttonText) {
        if (buttonText.isEmpty()) {
            button.setEnabled(false);
            button.setVisible(false);
        } else {
            button.setText(buttonText);
            button.setEnabled(true);
            button.setVisible(true);
        }
    }

    /**
     * Triggers a ClickEvent
     *
     * @param button The button, that is being triggered
     */
    protected void triggerDialogEvent(DialogEvent.DialogButton button) {
        if (dialogHandler != null) {
            dialogHandler.onDialogButtonClick(new DialogEvent() {
                @Override
                public DialogButton getDialogButton() {
                    return button;
                }
            });
        }
    }

}
