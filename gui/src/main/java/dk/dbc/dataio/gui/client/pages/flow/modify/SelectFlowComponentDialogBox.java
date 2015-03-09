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

    private ClickHandler selectButtonClickHandler = null;

    private final int MAX_NUMBER_OF_SHOWN_ITEMS = 10;


    @UiField DialogBox availableFlowComponentsDialog;
    @UiField ListBox flowComponentsList;


    @UiHandler("selectFlowComponentButton")
    void selectFlowComponentButtonPressed(ClickEvent event) {
        availableFlowComponentsDialog.hide();
        if (selectButtonClickHandler != null) {
            selectButtonClickHandler.onClick(event);
        }
    }

    public SelectFlowComponentDialogBox() {
        initWidget(ourUiBinder.createAndBindUi(this));
        availableFlowComponentsDialog.setGlassEnabled(true);
        availableFlowComponentsDialog.setAnimationEnabled(true);
    }


    public void activateDialogBox(Map<String, String> availableFlowComponents) {
        for (Map.Entry<String, String> item : availableFlowComponents.entrySet()) {
            flowComponentsList.addItem(item.getKey(), item.getValue());
        }
        flowComponentsList.setVisibleItemCount(availableFlowComponents.size() < MAX_NUMBER_OF_SHOWN_ITEMS ? availableFlowComponents.size() : MAX_NUMBER_OF_SHOWN_ITEMS);
        availableFlowComponentsDialog.center();
        availableFlowComponentsDialog.show();
    }

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