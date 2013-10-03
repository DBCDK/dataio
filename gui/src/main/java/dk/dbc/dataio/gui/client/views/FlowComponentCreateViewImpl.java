package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.components.ListEntry;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import java.util.List;


public class FlowComponentCreateViewImpl extends FlowPanel implements FlowComponentCreateView {

    // public Identifiers
    public static final String CONTEXT_HEADER = "Flow Komponent - opsætning";
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public static final String SAVE_RESULT_LABEL_PROCESSING_MESSAGE = "Opsætningen gemmes...";
    public static final String FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    public static final String FLOW_COMPONENT_CREATION_SCM_PROJECT_NOT_FOUND_ERROR = "Det angivne projekt findes ikke i SVN.";
    public static final String FLOW_COMPONENT_CREATION_SCM_ILLEGAL_PROJECT_NAME_ERROR = "Det angivne projekt må ikke indeholde sti elementer.";
    public static final String FLOW_COMPONENT_CREATION_JAVASCRIPT_REFERENCE_ERROR = "Der skete en fejl i forbindelse med kald til SVN. Prøv at vælge en anden revision eller et andet javascript.";
    public static final String FLOW_COMPONENT_CREATION_BUSY_LABEL = "Busy...";
    public static final String FLOW_COMPONENT_CREATION_KOMPONENT_NAVN_LABEL = "Komponentnavn";
    public static final String FLOW_COMPONENT_CREATION_SVN_PROJEKT_LABEL = "SVN Projekt";
    public static final String FLOW_COMPONENT_CREATION_SVN_REVISION_LABEL = "SVN Revision";
    public static final String FLOW_COMPONENT_CREATION_SCRIPT_NAME_LABEL = "Script navn";
    public static final String FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LABEL = "Invocation Method";
    public static final String FLOW_COMPONENT_CREATION_SAVE_BUTTON_LABEL = "Gem";

    public static final String GUIID_FLOW_COMPONENT_CREATION_WIDGET = "flowcomponentcreationwidget";
    public static final String GUIID_FLOW_COMPONENT_CREATION_NAME_PANEL = "flowcomponentcreationnamepanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_PROJECT_PANEL = "flowcomponentcreationprojectpanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_PANEL = "flowcomponentcreationsvnrevisionpanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_PANEL = "flowcomponentcreationscriptnamepanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL = "flow-component-invocation-method-panel-id";
    
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL = "flowcomponentcreationsaveresultlabel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON = "flowcomponentcreationsavebutton";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_PANEL = "flow-component-save-panel-id";
    public static final String FORM_FIELD_COMPONENT_NAME = "formfieldcomponentname";
    public static final String FORM_FIELD_INVOCATION_METHOD = "formfieldinvocationmethod";
    public static final String FORM_FIELD_JAVASCRIPT_FILE_UPLOAD = "formfieldjavascriptfileupload";
    // private objects
    private FlowComponentCreatePresenter presenter;

    private TextEntry namePanel = new TextEntry(GUIID_FLOW_COMPONENT_CREATION_NAME_PANEL, FLOW_COMPONENT_CREATION_KOMPONENT_NAVN_LABEL);
    private TextEntry projectPanel = new TextEntry(GUIID_FLOW_COMPONENT_CREATION_PROJECT_PANEL, FLOW_COMPONENT_CREATION_SVN_PROJEKT_LABEL);
    private ListEntry revisionPanel = new ListEntry(GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_PANEL, FLOW_COMPONENT_CREATION_SVN_REVISION_LABEL);
    private ListEntry scriptNamePanel = new ListEntry(GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_PANEL, FLOW_COMPONENT_CREATION_SCRIPT_NAME_LABEL);
    private ListEntry invocationMethodPanel = new ListEntry(GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL, FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LABEL);
    private FlowComponentSavePanel savePanel = new FlowComponentSavePanel();
    private Label busyLabel = new Label(FLOW_COMPONENT_CREATION_BUSY_LABEL);

    
    public FlowComponentCreateViewImpl() {
        super();
        getElement().setId(GUIID_FLOW_COMPONENT_CREATION_WIDGET);
        
        namePanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        add(namePanel);
        
        projectPanel.addKeyDownHandler(new InputFieldKeyDownHandler());
        projectPanel.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                svnProjectChanged();
            }
        });
        add(projectPanel);
        
        revisionPanel.setEnabled(false);
        revisionPanel.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                svnRevisionChanged();
            }
        });
        add(revisionPanel);
        
        scriptNamePanel.setEnabled(false);
        scriptNamePanel.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                scriptNameChanged();
            }
        });
        add(scriptNamePanel);
        
        invocationMethodPanel.setEnabled(false);
        invocationMethodPanel.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                invocationMethodNameChanged();
            }
        });
        add(invocationMethodPanel);
        
        add(savePanel);
        add(busyLabel);
        setAsBusy(false);
    }

    @Override
    public void setPresenter(FlowComponentCreatePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onFailure(String message) {
        setAsBusy(false);
        savePanel.setStatusText("");
        Window.alert("Error: " + message);
    }

    @Override
    public void onSuccess(String message) {
        savePanel.setStatusText(message);
    }

    @Override
    public void refresh() {
    }

    public void setAsBusy(Boolean busy) {
        busyLabel.setVisible(busy);
    }

    @Override
    public void setAvailableRevisions(List<RevisionInfo> availableRevisions) {
        setAsBusy(false);
        revisionPanel.clear();
        if (!availableRevisions.isEmpty()) {
            for (RevisionInfo revision: availableRevisions) {
                revisionPanel.setAvailableItem(String.valueOf(revision.getRevision()));
            }
            revisionPanel.setEnabled(true);
            revisionPanel.fireChangeEvent();
        }
    }

    @Override
    public void setAvailableScriptNames(List<String> availableScriptNames) {
        setAsBusy(false);
        scriptNamePanel.clear();
        if (!availableScriptNames.isEmpty()) {
            for (String scriptName: availableScriptNames) {
                scriptNamePanel.setAvailableItem(scriptName);
            }
            scriptNamePanel.setEnabled(true);
            scriptNamePanel.fireChangeEvent();
        }
    }

    @Override
    public void setAvailableInvocationMethods(List<String> availableInvocationMethods) {
        setAsBusy(false);
        invocationMethodPanel.clear();
        if (!availableInvocationMethods.isEmpty()) {
            for (String invocationMethod: availableInvocationMethods) {
                invocationMethodPanel.setAvailableItem(invocationMethod);
            }
            invocationMethodPanel.setEnabled(true);
            invocationMethodPanel.fireChangeEvent();
        }
    }

    @Override
    public void fetchRevisionFailed(JavaScriptProjectFetcherError errorCode, String detail) {
        revisionPanel.setEnabled(false);
        scriptNamePanel.setEnabled(false);
        invocationMethodPanel.setEnabled(false);
        final String errorMessage;
        if (errorCode == null) {
            errorMessage = detail;
        } else {
            switch (errorCode) {
                case SCM_RESOURCE_NOT_FOUND: errorMessage = FLOW_COMPONENT_CREATION_SCM_PROJECT_NOT_FOUND_ERROR;
                    break;
                case SCM_ILLEGAL_PROJECT_NAME: errorMessage = FLOW_COMPONENT_CREATION_SCM_ILLEGAL_PROJECT_NAME_ERROR;
                    break;
                default: errorMessage = detail;
                    break;
            }
        }
        onFailure(errorMessage);
    }

    @Override
    public void fetchScriptNamesFailed(String failText) {
        scriptNamePanel.setEnabled(false);
        invocationMethodPanel.setEnabled(false);
        onFailure(failText);
    }

    @Override
    public void fetchInvocationMethodsFailed(JavaScriptProjectFetcherError errorCode, String detail) {
        invocationMethodPanel.setEnabled(false);
        final String errorMessage;
        if (errorCode == null) {
            errorMessage = detail;
        } else {
            switch (errorCode) {
                case JAVASCRIPT_REFERENCE_ERROR: errorMessage = FLOW_COMPONENT_CREATION_JAVASCRIPT_REFERENCE_ERROR;
                    break;
                default: errorMessage = detail;
                    break;
            }
        }
        onFailure(errorMessage);
    }

    private void svnProjectChanged() {
        setAsBusy(true);
        savePanel.setStatusText("");
        presenter.projectNameEntered(projectPanel.getText());
    }

    private void svnRevisionChanged() {
        setAsBusy(true);
        savePanel.setStatusText("");
        presenter.revisionSelected(projectPanel.getText(), Long.parseLong(revisionPanel.getSelectedText()));
    }

    private void scriptNameChanged() {
        setAsBusy(true);
        savePanel.setStatusText("");
        presenter.scriptNameSelected(projectPanel.getText(), Long.parseLong(revisionPanel.getSelectedText()), scriptNamePanel.getSelectedText());
    }

    private void invocationMethodNameChanged() {
        savePanel.setStatusText("");
    }


    /**
     * Panel: FlowComponentSavePanel
     */
    private class FlowComponentSavePanel extends HorizontalPanel {

        private final Button flowComponentSaveButton = new Button(FLOW_COMPONENT_CREATION_SAVE_BUTTON_LABEL);
        private final Label flowComponentSaveResultLabel = new Label("");

        public FlowComponentSavePanel() {
            flowComponentSaveResultLabel.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL);
            add(flowComponentSaveResultLabel);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SAVE_PANEL);
            flowComponentSaveButton.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON);
            flowComponentSaveButton.addClickHandler(new SaveButtonHandler());
            add(flowComponentSaveButton);
        }

        public void setStatusText(String statusText) {
            flowComponentSaveResultLabel.setText(statusText);
        }
    }

    /**
     * Event Handlers *
     */
    private class SaveButtonHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            String name = namePanel.getText();
            String project = projectPanel.getText();
            long revision = Long.parseLong(revisionPanel.getSelectedText());
            String scriptName = scriptNamePanel.getSelectedText();
            String invocationMethod = invocationMethodPanel.getSelectedText();
            if (name.isEmpty() || project.isEmpty() || (revision == 0) || scriptName.isEmpty() || invocationMethod.isEmpty()) {
                onFailure(FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR);
            } else {
                savePanel.setStatusText(SAVE_RESULT_LABEL_PROCESSING_MESSAGE);
                presenter.saveFlowComponent(name, project, revision, scriptName, invocationMethod);
            }
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {

        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            savePanel.setStatusText("");
        }
    }
}
