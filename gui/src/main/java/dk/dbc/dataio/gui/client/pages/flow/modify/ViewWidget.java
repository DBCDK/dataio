package dk.dbc.dataio.gui.client.pages.flow.modify;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.PromptedMultiList;
import dk.dbc.dataio.gui.client.components.PromptedTextArea;
import dk.dbc.dataio.gui.client.components.PromptedTextBox;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class ViewWidget extends ContentPanel<Presenter> {
    interface FlowUiBinder extends UiBinder<HTMLPanel, ViewWidget> {}
    private static FlowUiBinder uiBinder = GWT.create(FlowUiBinder.class);

    public ViewWidget(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    @UiField PromptedTextBox name;
    @UiField PromptedTextArea description;
    @UiField PromptedMultiList flowComponents;
    @UiField Button deleteButton;
    @UiField Label status;


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

    @UiHandler(value={"name", "description"})
    void keyPressed(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

    @UiHandler("deleteButton")
    @SuppressWarnings("unused")
    void deleteButtonPressed(ClickEvent event) {
        presenter.deleteButtonPressed();
    }

    @UiHandler("flowComponents")
    void flowComponentButtonsClicked(ClickEvent event) {
        if (flowComponents.isAddEvent(event)) {
            presenter.addButtonPressed();
        } else if (flowComponents.isRemoveEvent(event)) {
            presenter.removeButtonPressed();
        }
    }

}
