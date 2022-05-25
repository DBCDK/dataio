package dk.dbc.dataio.gui.client.pages.submitter.modify;

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
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import dk.dbc.dataio.gui.client.components.databinder.DataBinder;
import dk.dbc.dataio.gui.client.components.popup.PopupBox;
import dk.dbc.dataio.gui.client.components.prompted.Prompted;
import dk.dbc.dataio.gui.client.components.prompted.PromptedCheckBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {
    interface SubmitterBinder extends UiBinder<HTMLPanel, View> {
    }

    private static SubmitterBinder uiBinder = GWT.create(SubmitterBinder.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    Texts texts = viewInjector.getTexts();

    public View() {
        super("");
        number = new DataBinder<>(
                new Prompted<>(new TextBox(), texts.label_SubmitterNumber()),
                number -> {
                    presenter.keyPressed();
                    presenter.numberChanged(number);
                }
        );
        name = new DataBinder<>(
                new Prompted<>(new TextBox(), texts.label_SubmitterName()),
                name -> {
                    presenter.keyPressed();
                    presenter.nameChanged(name);
                }
        );
        description = new DataBinder<>(
                new Prompted<>(new TextArea(), texts.label_Description()),
                description -> {
                    presenter.keyPressed();
                    presenter.descriptionChanged(description);
                }
        );
        add(uiBinder.createAndBindUi(this));
    }

    @UiFactory
    PopupBox<Label> getPopupBox() {
        return new PopupBox<>(new Label(viewInjector.getTexts().label_AreYouSureAboutDeleting()), "", "");
    }

    @UiField(provided = true)
    DataBinder<String, Prompted<String, TextBox>> number;
    @UiField(provided = true)
    DataBinder<String, Prompted<String, TextBox>> name;
    @UiField(provided = true)
    DataBinder<String, Prompted<String, TextArea>> description;
    @UiField
    PromptedList priority;
    @UiField
    Button deleteButton;
    @UiField
    Label status;
    @UiField
    PopupBox<Label> confirmation;
    @UiField
    PromptedCheckBox disabledStatus;

    @UiHandler("priority")
    void priorityChanged(ValueChangeEvent<String> event) {
        presenter.priorityChanged(priority.getSelectedKey());
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

    @UiHandler("disabledStatus")
    void disabledChanged(ValueChangeEvent<Boolean> event) {
        presenter.disabledStatusChanged(disabledStatus.getValue());
        presenter.keyPressed();
    }

}
