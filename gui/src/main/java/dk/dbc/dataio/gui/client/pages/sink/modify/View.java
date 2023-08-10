package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.popup.PopupValueBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedPasswordTextBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedRadioButtons;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface SinkBinder extends UiBinder<HTMLPanel, View> {
    }

    private static SinkBinder uiBinder = GWT.create(SinkBinder.class);
    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }

    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }

    @UiFactory
    PopupValueBox<TextBox, String> getPopupValueBox() {
        return new PopupValueBox<>(new TextBox(), "", "");
    }

    @UiField
    PromptedList sinkTypeSelection;
    @UiField
    PromptedTextBox name;
    @UiField
    PromptedTextBox queue;
    @UiField
    PromptedTextArea description;
    @UiField
    PromptedTextBox timeout;
    @UiField
    HTMLPanel sequenceAnalysisSection;
    @UiField
    HTMLPanel updateSinkSection;
    @UiField
    HTMLPanel dpfSinkSection;
    @UiField
    HTMLPanel esSinkSection;
    @UiField
    HTMLPanel imsSinkSection;
    @UiField
    HTMLPanel worldCatSinkSection;
    @UiField
    HTMLPanel vipSinkSection;
    @UiField
    PromptedTextBox url;
    @UiField
    PromptedTextBox openupdateuserid;
    @UiField
    PromptedPasswordTextBox openupdatepassword;
    @UiField
    PromptedMultiList queueProviders;
    @UiField
    PromptedMultiList updateServiceIgnoredValidationErrors;
    @UiField
    PromptedTextBox dpfUpdateServiceUserId;
    @UiField
    PromptedPasswordTextBox dpfUpdateServicePassword;
    @UiField
    PromptedMultiList dpfUpdateServiceQueueProviders;
    @UiField
    PromptedTextBox esUserId;
    @UiField
    PromptedTextBox esDatabase;
    @UiField
    PromptedTextBox imsEndpoint;
    @UiField
    PromptedTextBox worldCatUserId;
    @UiField
    PromptedPasswordTextBox worldCatPassword;
    @UiField
    PromptedTextBox worldCatProjectId;
    @UiField
    PromptedTextBox worldCatEndpoint;
    @UiField
    PromptedTextBox vipEndpoint;
    @UiField
    PromptedMultiList worldCatRetryDiagnostics;
    @UiField
    Button deleteButton;
    @UiField
    Label status;
    @UiField
    PopupValueBox queueProvidersPopupTextBox;
    @UiField
    PopupValueBox updateServiceIgnoredValidationErrorsPopupTextBox;
    @UiField
    PopupValueBox worldCatPopupTextBox;
    @UiField
    PromptedRadioButtons sequenceAnalysisSelection;
    @UiField
    PopupBox<Label> confirmation;

    @SuppressWarnings("unused")
    @UiHandler("sinkTypeSelection")
    void sinkTypeSelectionChanged(ValueChangeEvent<String> event) {
        presenter.sinkTypeChanged(SinkContent.SinkType.valueOf(sinkTypeSelection.getSelectedKey()));
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("queue")
    void queueChanged(ValueChangeEvent<String> event) {
        presenter.queueChanged(queue.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @UiHandler("timeout")
    void timeoutChanged(ValueChangeEvent<String> event) {
        if(timeout.getText().matches("\\d+") && Integer.parseInt(timeout.getText()) > 0) presenter.timeoutChanged(timeout.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("url")
    void urlChanged(ValueChangeEvent<String> event) {
        presenter.endpointChanged(url.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("openupdateuserid")
    void useridChanged(ValueChangeEvent<String> event) {
        presenter.openUpdateUserIdChanged(openupdateuserid.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("openupdatepassword")
    void passwordChanged(ValueChangeEvent<String> event) {
        presenter.passwordChanged(openupdatepassword.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("queueProviders")
    void availableQueueProvidersChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.queueProvidersChanged(new ArrayList<>(queueProviders.getValue().values()));
            presenter.keyPressed();
        }
    }

    @SuppressWarnings("unused")
    @UiHandler("updateServiceIgnoredValidationErrors")
    void updateServiceIgnoredValidationErrorsChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.updateServiceIgnoredValidationErrorsChanged(new HashSet<>(updateServiceIgnoredValidationErrors.getValue().values()));
            presenter.keyPressed();
        }
    }

    @UiHandler("updateServiceIgnoredValidationErrors")
    void updateServiceIgnoredValidationErrorsButtonClicked(ClickEvent event) {
        if (updateServiceIgnoredValidationErrors.isAddEvent(event)) {
            presenter.updateServiceIgnoredValidationErrorsAddButtonPressed();
        } else if (updateServiceIgnoredValidationErrors.isRemoveEvent(event)) {
            presenter.updateServiceIgnoredValidationErrorsRemoveButtonPressed(updateServiceIgnoredValidationErrors.getSelectedItem());
        }
    }

    @SuppressWarnings("unused")
    @UiHandler("dpfUpdateServiceUserId")
    void dpfUpdateServiceUserIdChanged(ValueChangeEvent<String> event) {
        presenter.dpfUpdateServiceUserIdChanged(dpfUpdateServiceUserId.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("dpfUpdateServicePassword")
    void dpfUpdateServicePasswordChanged(ValueChangeEvent<String> event) {
        presenter.dpfUpdateServicePasswordChanged(dpfUpdateServicePassword.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("dpfUpdateServiceQueueProviders")
    void dpfUpdateServiceAvailableQueueProvidersChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.dpfUpdateServiceQueueProvidersChanged(
                    new ArrayList<>(dpfUpdateServiceQueueProviders.getValue().values()));
            presenter.keyPressed();
        }
    }

    @SuppressWarnings("unused")
    @UiHandler("esUserId")
    void esUserIdChanged(ValueChangeEvent<String> event) {
        presenter.esUserIdChanged(esUserId.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("esDatabase")
    void esDatabaseChanged(ValueChangeEvent<String> event) {
        presenter.esDatabaseChanged(esDatabase.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("imsEndpoint")
    void imsEndpointChanged(ValueChangeEvent<String> event) {
        presenter.imsEndpointChanged(imsEndpoint.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("worldCatUserId")
    void worldCatUserIdChanged(ValueChangeEvent<String> event) {
        presenter.worldCatUserIdChanged(worldCatUserId.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("worldCatPassword")
    void worldCatPasswordChanged(ValueChangeEvent<String> event) {
        presenter.worldCatPasswordChanged(worldCatPassword.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("worldCatProjectId")
    void worldCatProjectIdChanged(ValueChangeEvent<String> event) {
        presenter.worldCatProjectIdChanged(worldCatProjectId.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("worldCatEndpoint")
    void worldCatEndpointChanged(ValueChangeEvent<String> event) {
        presenter.worldCatEndpointChanged(worldCatEndpoint.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("worldCatRetryDiagnostics")
    void worldCatRetryDiagnosticsChanged(ValueChangeEvent<Map<String, String>> event) {
        if (presenter != null) {
            presenter.worldCatRetryDiagnosticsChanged(new ArrayList<>(worldCatRetryDiagnostics.getValue().values()));
            presenter.keyPressed();
        }
    }

    @SuppressWarnings("unused")
    @UiHandler("vipEndpoint")
    void vipEndpointChanged(ValueChangeEvent<String> event) {
        presenter.vipEndpointChanged(vipEndpoint.getText());
        presenter.keyPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @SuppressWarnings("unused")
    @UiHandler("deleteButton")
    void deleteButtonPressed(ClickEvent event) {
        confirmation.show();
    }

    @UiHandler("queueProviders")
    void availableQueueProvidersButtonClicked(ClickEvent event) {
        if (queueProviders.isAddEvent(event)) {
            presenter.queueProvidersAddButtonPressed();
        }
    }

    @UiHandler("worldCatRetryDiagnostics")
    void worldCatRetryDiagnosticsButtonClicked(ClickEvent event) {
        if (worldCatRetryDiagnostics.isAddEvent(event)) {
            presenter.worldCatRetryDiagnosticsAddButtonPressed();
        } else if (worldCatRetryDiagnostics.isRemoveEvent(event)) {
            presenter.worldCatRetryDiagnosticRemoveButtonPressed(worldCatRetryDiagnostics.getSelectedItem());
        }
    }

    @UiHandler("sequenceAnalysisSelection")
    void sequenceAnalysisSelectionChanged(ValueChangeEvent<String> event) {
        presenter.sequenceAnalysisSelectionChanged(event.getValue());
    }

    @UiHandler("queueProvidersPopupTextBox")
    void popupTextBoxChanged(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            final SinkContent.SinkType sinkType = SinkContent.SinkType.valueOf(sinkTypeSelection.getSelectedKey());
            if (sinkType == SinkContent.SinkType.DPF) {
                Map<String, String> list = dpfUpdateServiceQueueProviders.getValue();
                list.put((String) queueProvidersPopupTextBox.getValue(), (String) queueProvidersPopupTextBox.getValue());
                dpfUpdateServiceQueueProviders.setValue(list, true);
            } else {
                Map<String, String> list = queueProviders.getValue();
                list.put((String) queueProvidersPopupTextBox.getValue(), (String) queueProvidersPopupTextBox.getValue());
                queueProviders.setValue(list, true);
            }
        }
    }

    @UiHandler("updateServiceIgnoredValidationErrorsPopupTextBox")
    void updateServiceIgnoredValidationErrorsPopupTextBoxChanged(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            Map<String, String> list = updateServiceIgnoredValidationErrors.getValue();
            list.put((String) updateServiceIgnoredValidationErrorsPopupTextBox.getValue(),
                    (String) updateServiceIgnoredValidationErrorsPopupTextBox.getValue());
            updateServiceIgnoredValidationErrors.setValue(list, true);
        }
    }

    @UiHandler("worldCatPopupTextBox")
    void worldCatPopupTextBoxChanged(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            Map<String, String> list = worldCatRetryDiagnostics.getValue();
            list.put((String) worldCatPopupTextBox.getValue(), (String) worldCatPopupTextBox.getValue());
            worldCatRetryDiagnostics.setValue(list, true);
        }
    }

    @UiHandler("queueProviders")
    void queueProvidersButtonClicked(ClickEvent event) {
        if (queueProviders.isAddEvent(event)) {
            presenter.queueProvidersAddButtonPressed();
        }
    }

    @UiHandler("dpfUpdateServiceQueueProviders")
    void dpfUpdateServiceQueueProvidersButtonClicked(ClickEvent event) {
        if (dpfUpdateServiceQueueProviders.isAddEvent(event)) {
            presenter.dpfUpdateServiceQueueProvidersAddButtonPressed();
        } else if (dpfUpdateServiceQueueProviders.isRemoveEvent(event)) {
            final Map<String, String> list = dpfUpdateServiceQueueProviders.getValue();
            list.remove(dpfUpdateServiceQueueProviders.getSelectedItem(),
                    dpfUpdateServiceQueueProviders.getSelectedItem());
            dpfUpdateServiceQueueProviders.setValue(list, true);
        }
    }

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }
}
