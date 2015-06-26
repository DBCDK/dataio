package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import java.util.Map;

/**
 * This is the dialog box for selecting a Flow Component
 */
public class SelectFlowComponentDialogBox extends Composite implements HasClickHandlers {
    interface SelectFlowComponentDialogBoxUiBinder extends UiBinder<HTMLPanel, SelectFlowComponentDialogBox> {
    }
    private static SelectFlowComponentDialogBoxUiBinder ourUiBinder = GWT.create(SelectFlowComponentDialogBoxUiBinder.class);

    ClickHandler selectButtonClickHandler = null;  // Is package private due to test

    private final int MAX_NUMBER_OF_SHOWN_ITEMS = 10;


    @UiField DialogBox availableFlowComponentsDialog;
    @UiField ListBox flowComponentsList;


    /**
     * UI Handler for clicks on the Select button in the dialog
     * @param event The Click Event
     */
    @UiHandler("selectFlowComponentButton")
    void selectFlowComponentButtonPressed(ClickEvent event) {
        availableFlowComponentsDialog.hide();
        if (selectButtonClickHandler != null) {
            selectButtonClickHandler.onClick(event);
        }
    }

    /**
     * UI Handler for clicks on the Cancel button in the dialog
     * @param event The Click Event
     */
    @UiHandler("cancelButton")
    void cancelButtonPressed(ClickEvent event) {
        availableFlowComponentsDialog.hide();
    }

    /**
     * Constructor
     * Activates and shows the Dialog Box
     * Supplies available flow components, that will be show in the dialog box
     * @param availableFlowComponents Flow components to be shown in the list
     * @param clickHandler, the click handler
     */
    public SelectFlowComponentDialogBox(Map<String, String> availableFlowComponents, ClickHandler clickHandler) {
        initWidget(ourUiBinder.createAndBindUi(this));
        availableFlowComponentsDialog.setGlassEnabled(true);
        availableFlowComponentsDialog.setAnimationEnabled(true);
        for (Map.Entry<String, String> item : availableFlowComponents.entrySet()) {
            flowComponentsList.addItem(item.getKey(), item.getValue());
        }
        int listBoxSize = availableFlowComponents.size() < MAX_NUMBER_OF_SHOWN_ITEMS ? availableFlowComponents.size() : MAX_NUMBER_OF_SHOWN_ITEMS;  // To assure, that max size is MAX_NUMBER_OF_SHOWN_ITEMS
        listBoxSize = listBoxSize > 1 ? listBoxSize : 2;  // To assure, that min size is 2
        flowComponentsList.setVisibleItemCount(listBoxSize);
        availableFlowComponentsDialog.center();
        availableFlowComponentsDialog.show();
        addClickHandler(clickHandler);
    }

    /**
     * Adds a click handler to the Dialog Box, that will be activated, when pushing one of the buttons in the dialog
     * @param clickHandler Click handler, taking care of click events
     * @return A Handler Registration object, to be used when de-activating the click handler
     */
    @Override
    public HandlerRegistration addClickHandler(ClickHandler clickHandler) {
        selectButtonClickHandler = clickHandler;
        return new HandlerRegistration() {
            @Override
            public void removeHandler() {
                selectButtonClickHandler = null;
            }
        };
    }


}