package dk.dbc.dataio.gui.client.pages.flow.modify;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.MultiListEntry;
import dk.dbc.dataio.gui.client.components.TextAreaEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Map;

public class View extends ContentPanel<Presenter> {
    interface FlowUiBinder extends UiBinder<HTMLPanel, View> {}
    private static FlowUiBinder uiBinder = GWT.create(FlowUiBinder.class);

    public View(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    @Override
    public void init() {
    }

    @UiField TextEntry name;
    @UiField TextAreaEntry description;
    @UiField MultiListEntry flowComponents;
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
        }
        presenter.keyPressed();
    }

    @UiHandler(value={"name", "description"})
    void keyPressed(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.saveButtonPressed();
    }

}
