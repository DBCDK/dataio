package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {

    interface SinkBinder extends UiBinder<HTMLPanel, View> {}
    private static SinkBinder uiBinder = GWT.create(SinkBinder.class);

    public View(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    @UiField PromptedTextBox name;
    @UiField PromptedTextBox resource;
    @UiField PromptedTextArea description;
    @UiField Button deleteButton;
    @UiField Label status;

    @UiHandler("name")
    @SuppressWarnings("unused")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
        presenter.keyPressed();
    }

    @UiHandler("resource")
    @SuppressWarnings("unused")
    void resourceChanged(ValueChangeEvent<String> event) {
        presenter.resourceChanged(resource.getText());
        presenter.keyPressed();
    }

    @UiHandler("description")
    @SuppressWarnings("unused")
    void descriptionChanged(ValueChangeEvent<String> event) {
        presenter.descriptionChanged(description.getText());
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    @SuppressWarnings("unused")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    @SuppressWarnings("unused")
    void deleteButtonPressed(ClickEvent event) {
        presenter.deleteButtonPressed();
    }
}
