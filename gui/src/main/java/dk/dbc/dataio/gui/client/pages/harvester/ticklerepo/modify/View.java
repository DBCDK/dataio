package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedDateTimeBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface HarvesterBinder extends UiBinder<HTMLPanel, View> {
    }

    private static HarvesterBinder uiBinder = GWT.create(HarvesterBinder.class);
    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }


    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }


    @UiField
    PromptedTextBox id;
    @UiField
    PromptedTextBox name;
    @UiField
    PromptedTextArea description;
    @UiField
    PromptedTextBox destination;
    @UiField
    PromptedTextBox format;
    @UiField
    PromptedList type;
    @UiField
    PromptedCheckBox enabled;
    @UiField
    PromptedCheckBox notificationsEnabled;
    @UiField
    PromptedDateTimeBox deleteOutdatedRecordsFromDate;
    @UiField
    Button taskRecordHarvestButton;
    @UiField
    Button deleteOutdatedRecordsButton;
    @UiField
    Button saveButton;
    @UiField
    Button deleteButton;
    @UiField
    Label status;
    @UiField
    PopupBox<Label> confirmation;
    @UiField
    DialogBox recordHarvestConfirmationDialog;
    @UiField
    DialogBox deleteOutdatedRecordsDialog;
    @UiField
    Label recordHarvestCount;
    @UiField
    Label recordHarvestConfirmation;
    @UiField
    Button recordHarvestOkButton;


    @SuppressWarnings("unused")
    @UiHandler("id")
    void idChanged(ValueChangeEvent<String> event) {
        presenter.idChanged(id.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("destination")
    void destinationChanged(ValueChangeEvent<String> event) {
        presenter.destinationChanged(destination.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("format")
    void formatChanged(ValueChangeEvent<String> event) {
        presenter.formatChanged(format.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("type")
    void typeChanged(ValueChangeEvent<String> event) {
        presenter.typeChanged(type.getSelectedKey());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("enabled")
    void enabledChanged(ValueChangeEvent<Boolean> event) {
        presenter.enabledChanged(enabled.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("notificationsEnabled")
    void notificationsEnabledChanged(ValueChangeEvent<Boolean> event) {
        presenter.notificationsEnabledChanged(notificationsEnabled.getValue());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("taskRecordHarvestButton")
    void taskRecordHarvestButtonPressed(ClickEvent event) {
        presenter.setRecordHarvestCount();
    }

    @SuppressWarnings("unused")
    @UiHandler("deleteOutdatedRecordsButton")
    void setDeleteOutdatedRecordsButtonPressed(ClickEvent event) {
        presenter.deleteOutdatedRecordsButtonPressed();
    }

    @UiHandler("recordHarvestOkButton")
    @SuppressWarnings("unused")
    void onRerunOkButtonClick(ClickEvent event) {
        presenter.taskRecordHarvestButtonPressed();
        recordHarvestConfirmationDialog.hide();
    }

    @UiHandler("recordHarvestCancelButton")
    @SuppressWarnings("unused")
    void onRerunCancelButtonClick(ClickEvent event) {
        recordHarvestConfirmationDialog.hide();  // Just hide - do nothing else...
    }

    @UiHandler("deleteOutdatedRecordsOkButton")
    @SuppressWarnings("unused")
    void onDeleteOutdatedRecordsOkButtonClick(ClickEvent event) {
        presenter.deleteOutdatedRecords();
        deleteOutdatedRecordsDialog.hide();
    }

    @UiHandler("deleteOutdatedRecordsCancelButton")
    @SuppressWarnings("unused")
    void onDeleteOutdatedRecordsCancelButtonClick(ClickEvent event) {
        deleteOutdatedRecordsDialog.hide(); // Just hide - do nothing else...
    }

    @SuppressWarnings("unused")
    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }

}
