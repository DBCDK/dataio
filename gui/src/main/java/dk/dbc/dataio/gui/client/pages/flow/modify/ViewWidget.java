package dk.dbc.dataio.gui.client.pages.flow.modify;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class ViewWidget extends ContentPanel<Presenter> {
    interface FlowUiBinder extends UiBinder<HTMLPanel, ViewWidget> {
    }

    private static FlowUiBinder uiBinder = GWT.create(FlowUiBinder.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    protected FlowModel model;
    protected boolean showAvailableFlowComponents;

    public ViewWidget() {
        super("");
        add(uiBinder.createAndBindUi(this));
        this.model = new FlowModel();
        this.showAvailableFlowComponents = false;
    }

    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }

    @UiField
    PromptedTextBox name;
    @UiField
    PromptedTextArea description;
    @UiField
    PromptedMultiList flowComponents;
    @UiField
    Button deleteButton;
    @UiField
    Label status;
    @UiField
    PopupListBox popupListBox;
    @UiField
    PopupBox<Label> confirmation;


    @UiHandler("name")
    void nameChanged(BlurEvent event) {
        presenter.nameChanged(name.getText());
    }

    @UiHandler("description")
    void descriptionChanged(BlurEvent event) {
        presenter.descriptionChanged(description.getText());
    }

    @UiHandler("flowComponents")
    void flowComponentsChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.flowComponentsChanged(flowComponents.getValue());
            presenter.keyPressed();
        }
    }

    @UiHandler(value = {"name", "description"})
    void keyPressed(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
    }

    @UiHandler("flowComponents")
    void flowComponentButtonsClicked(ClickEvent event) {
        if (flowComponents.isAddEvent(event)) {
            presenter.addButtonPressed();
        } else if (flowComponents.isRemoveEvent(event)) {
            presenter.removeButtonPressed();
        }
    }

    @UiHandler("popupListBox")
    void popupListBoxClicked(DialogEvent event) {
        switch (event.getDialogButton()) {
            case OK_BUTTON:
                presenter.selectFlowComponentButtonPressed(popupListBox.getValue());
                break;
            case EXTRA_BUTTON:
                presenter.newFlowComponentButtonPressed();
                break;
            case CANCEL_BUTTON:
            default:
                // Do nothing
                break;
        }
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }

}
