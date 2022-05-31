package dk.dbc.dataio.gui.client.components.popup;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * <p>Popup select box for displaying a widget in a popup window </p>
 * <p>The popup box consist of a header, the widget itself, 2 radio buttons and 1 confirmation button:</p>
 *  <ul>
 *   <li>radio button left</li>
 *   <li>radio button right</li>
 *   <li>confirmation Button</li>
 *  </ul>
 * <p> All radio buttons have user definable labels</p>
 * <p> Confirmation button have user definable labels</p>
 */
public class PopupSelectBox extends PopupBox {
    public static final String POPUP_SELECT_BOX_GUID = "dio-PopupSelectBox";
    private boolean rightSelected;

    private RadioButton radioButtonLeft;
    private RadioButton radioButtonRight;

    public PopupSelectBox() {
        this(new Label(), "", "", "", "");
    }

    /**
     * Constructor
     *
     * @param widget               The widget to be embedded in a Popup Box
     * @param dialogTitle          The title text to display on the Dialog Box (mandatory)
     * @param okButtonText         The text to be displayed in the OK Button (mandatory)
     * @param radioButtonLeftText  The text to be displayed for the radiobutton to the most left (mandatory)
     * @param radioButtonRightText The text to be displayed for the radiobutton to the most right (mandatory)
     **/
    public PopupSelectBox(IsWidget widget, String dialogTitle, String okButtonText, String radioButtonLeftText, String radioButtonRightText) {
        this(widget,
                dialogTitle,
                okButtonText,
                radioButtonLeftText,
                radioButtonRightText,
                new FlowPanel(),
                new DialogBox(),
                new VerticalPanel(),
                new FlowPanel(),
                new FlowPanel(),
                new RadioButton("radioButtonLeft"),
                new RadioButton("radioButtonRight"));
    }

    /**
     * Constructor (with component injections - to be used for testing)
     * The Constructor is package scoped - not public
     *
     * @param widget               The widget to be embedded in a Popup Box
     * @param dialogTitle          The title text to display on the Dialog Box (mandatory)
     * @param okButtonText         The text to be displayed in the OK Button (mandatory)
     * @param radioButtonLeftText  The text to be displayed for the radiobutton to the most left (mandatory)
     * @param radioButtonRightText The text to be displayed for the radiobutton to the most right (mandatory)
     * @param basePanel            Base panel to be used to embed the Dialog
     * @param dialogBox            The Dialog Box component
     * @param containerPanel       The container panel to embed the widgets
     * @param buttonPanel          The container panel to embed the buttons
     * @param radioButtonsPanel    The container panel to embed the radio buttons
     * @param radioButtonLeft      The left radio button,
     * @param radioButtonRight     The right radio button
     */
    PopupSelectBox(IsWidget widget,
                   String dialogTitle,
                   String okButtonText,
                   String radioButtonLeftText,
                   String radioButtonRightText,
                   FlowPanel basePanel,
                   DialogBox dialogBox,
                   VerticalPanel containerPanel,
                   FlowPanel buttonPanel,
                   FlowPanel radioButtonsPanel,
                   RadioButton radioButtonLeft,
                   RadioButton radioButtonRight) {
        super(widget, dialogTitle, okButtonText, basePanel, dialogBox, containerPanel, buttonPanel, new Button(), new Button(), new Button());

        // Data injection
        this.radioButtonLeft = radioButtonLeft;
        this.radioButtonRight = radioButtonRight;

        // Setup texts
        setLeftRadioButtonText(radioButtonLeftText);
        setRightRadioButtonText(radioButtonRightText);

        // Setup value change event handlers for the radio buttons
        radioButtonLeft.addValueChangeHandler(this::leftRadioButtonChangeEventHandler);
        radioButtonRight.addValueChangeHandler(this::rightRadioButtonChangeEventHandler);

        // Build the widget tree
        radioButtonsPanel.add(radioButtonLeft);
        radioButtonsPanel.add(radioButtonRight);
        containerPanel.add(radioButtonsPanel);
        // Set again because otherwise it ends up on the top of the panel
        containerPanel.add(buttonPanel);

        // Set CSS Stuff
        dialogBox.addStyleName(POPUP_SELECT_BOX_GUID);
    }

    public void setRightSelected(boolean rightSelected) {
        this.rightSelected = rightSelected;
        this.radioButtonRight.setValue(rightSelected, true);
    }

    public Boolean isRightSelected() {
        return rightSelected;
    }

    public void setLeftRadioButtonText(String radioButtonText) {
        configureRadioButtonLeft(radioButtonText);
    }

    public void setRightRadioButtonText(String radioButtonText) {
        configureRadioButtonRight(radioButtonText);
    }

    /*
     * Class protected methods (due to test)
     */
    void leftRadioButtonChangeEventHandler(ValueChangeEvent<Boolean> event) {
        rightSelected = !event.getValue();
        radioButtonLeft.setValue(!rightSelected);
        radioButtonRight.setValue(rightSelected);
    }

    void rightRadioButtonChangeEventHandler(ValueChangeEvent<Boolean> event) {
        rightSelected = event.getValue();
        radioButtonRight.setValue(rightSelected);
        radioButtonLeft.setValue(!rightSelected);
    }

    /*
     * Private methods
     */
    private void configureRadioButtonLeft(String radioButtonText) {
        if (radioButtonText.isEmpty()) {
            radioButtonLeft.setEnabled(false);
            radioButtonLeft.setVisible(false);
        } else {
            radioButtonLeft.setText(radioButtonText);
            radioButtonLeft.setEnabled(true);
            radioButtonLeft.setVisible(true);
            radioButtonLeft.setValue(!rightSelected);
        }
    }

    private void configureRadioButtonRight(String radioButtonText) {
        if (radioButtonText.isEmpty()) {
            radioButtonRight.setEnabled(false);
            radioButtonRight.setVisible(false);
        } else {
            radioButtonRight.setText(radioButtonText);
            radioButtonRight.setEnabled(true);
            radioButtonRight.setVisible(true);
            radioButtonRight.setValue(rightSelected);
        }
    }
}
