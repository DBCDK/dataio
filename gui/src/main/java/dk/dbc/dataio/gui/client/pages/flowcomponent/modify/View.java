package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.components.ListEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {

    interface FlowComponentBinder extends UiBinder<HTMLPanel, View> {}
    private static FlowComponentBinder uiBinder = GWT.create(FlowComponentBinder.class);

    public View(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    @Override
    public void init() {}

    @UiField TextEntry name;
    @UiField TextEntry project;
    @UiField ListEntry revision;
    @UiField ListEntry script;
    @UiField ListEntry method;
    @UiField Label status;
    @UiField Label busy;

    @UiHandler("name")
    void keyPressedInNameField(KeyDownEvent event) {
        presenter.keyPressed();
    }

    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        presenter.nameChanged(name.getText());
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

}
