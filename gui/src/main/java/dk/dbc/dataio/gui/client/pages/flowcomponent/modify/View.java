package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {

    interface FlowComponentBinder extends UiBinder<HTMLPanel, View> {
    }

    private static FlowComponentBinder uiBinder = GWT.create(FlowComponentBinder.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
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
    PromptedTextBox project;
    @UiField
    PromptedList revision;
    @UiField
    PromptedList next;
    @UiField
    PromptedList script;
    @UiField
    PromptedList method;
    @UiField
    Button deleteButton;
    @UiField
    Label status;
    @UiField
    Label busy;
    @UiField
    PopupBox<Label> confirmation;

    @UiHandler("name")
    void keyPressedInNameField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
    }

    @UiHandler("description")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @UiHandler("project")
    void keyPressedInProjectField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("project")
    void projectChanged(ValueChangeEvent<String> event) {
        presenter.projectChanged(project.getText());
    }

    @UiHandler("revision")
    void revisionChanged(ValueChangeEvent<String> event) {
        presenter.revisionChanged(revision.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("next")
    void nextChanged(ValueChangeEvent<String> event) {
        presenter.nextChanged(next.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("script")
    void scriptNameChanged(ValueChangeEvent<String> event) {
        presenter.scriptNameChanged(script.getSelectedKey());
        presenter.keyPressed();
    }

    @UiHandler("method")
    void invocationMethodChanged(ValueChangeEvent<String> event) {
        presenter.invocationMethodChanged(method.getSelectedKey());
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

    @UiHandler("confirmation")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.deleteButtonPressed();
        }
    }

}
