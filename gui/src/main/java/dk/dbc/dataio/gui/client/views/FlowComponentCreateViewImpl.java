package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.commons.types.RevisionInfo;
import java.util.List;

public class FlowComponentCreateViewImpl extends VerticalPanel implements FlowComponentCreateView {

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
    public static final String GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX = "flowcomponentcreationnametextbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SVN_PROJECT_PANEL = "flowcomponentcreationsvnprojectpanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SVN_PROJECT_TEXT_BOX = "flowcomponentcreationsvnprojecttextbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_PANEL = "flowcomponentcreationsvnrevisionpanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_LIST_BOX = "flowcomponentcreationsvnrevisionlistbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_PANEL = "flowcomponentcreationscriptnamepanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_LIST_BOX = "flowcomponentcreationscriptnamelistbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LIST_BOX = "flowcomponentcreationinvocationmethodlistbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL = "flowcomponentcreationsaveresultlabel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON = "flowcomponentcreationsavebutton";
    public static final String GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD = "flowcomponentcreationjavascriptfileupload";
    public static final String GUIID_FLOW_COMPONENT_CREATION_NAME_PANEL = "flow-component-name-panel-id";
    public static final String GUIID_FLOW_COMPONENT_CREATION_JAVA_SCRIPT_UPLOAD_PANEL = "flow-component-java-script-upload-panel-id";
    public static final String GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL = "flow-component-invocation-method-panel-id";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_PANEL = "flow-component-save-panel-id";
    public static final String FORM_FIELD_COMPONENT_NAME = "formfieldcomponentname";
    public static final String FORM_FIELD_INVOCATION_METHOD = "formfieldinvocationmethod";
    public static final String FORM_FIELD_JAVASCRIPT_FILE_UPLOAD = "formfieldjavascriptfileupload";
    // private objects
    private FlowComponentCreatePresenter presenter;
    private FlowComponentNamePanel namePanel = new FlowComponentNamePanel();
    private FlowComponentSvnProjectPanel projectPanel = new FlowComponentSvnProjectPanel();
    private FlowComponentSvnRevisionPanel revisionPanel = new FlowComponentSvnRevisionPanel();
    private FlowComponentScriptNamePanel scriptNamePanel = new FlowComponentScriptNamePanel();
    private FlowComponentInvocationMethodPanel invocationMethodPanel = new FlowComponentInvocationMethodPanel();
    private FlowComponentSavePanel savePanel = new FlowComponentSavePanel();
    private Label busyLabel = new Label(FLOW_COMPONENT_CREATION_BUSY_LABEL);

    public FlowComponentCreateViewImpl() {
        super();
        getElement().setId(GUIID_FLOW_COMPONENT_CREATION_WIDGET);
        add(namePanel);
        add(projectPanel);
        add(revisionPanel);
        add(scriptNamePanel);
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
        revisionPanel.setRevisions(availableRevisions);
        revisionPanel.fireChangeEvent();
    }

    @Override
    public void setAvailableScriptNames(List<String> availableScriptNames) {
        setAsBusy(false);
        scriptNamePanel.setScriptNames(availableScriptNames);
        scriptNamePanel.fireChangeEvent();
    }

    @Override
    public void setAvailableInvocationMethods(List<String> availableInvocationMethods) {
        setAsBusy(false);
        invocationMethodPanel.setInvocationMethods(availableInvocationMethods);
        invocationMethodPanel.fireChangeEvent();
    }

    @Override
    public void fetchRevisionFailed(JavaScriptProjectFetcherError errorCode, String detail) {
        revisionPanel.disable();
        scriptNamePanel.disable();
        invocationMethodPanel.disable();
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
        scriptNamePanel.disable();
        invocationMethodPanel.disable();
        onFailure(failText);
    }

    @Override
    public void fetchInvocationMethodsFailed(JavaScriptProjectFetcherError errorCode, String detail) {
        invocationMethodPanel.disable();
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
        presenter.projectNameEntered(projectPanel.getProjectName());
    }

    private void svnRevisionChanged() {
        setAsBusy(true);
        savePanel.setStatusText("");
        presenter.revisionSelected(projectPanel.getProjectName(), revisionPanel.getSelectedRevision());
    }

    private void scriptNameChanged() {
        setAsBusy(true);
        savePanel.setStatusText("");
        presenter.scriptNameSelected(projectPanel.getProjectName(), revisionPanel.getSelectedRevision(), scriptNamePanel.getScriptName());
    }

    private void invocationMethodNameChanged() {
        savePanel.setStatusText("");
    }

    /**
     * Panel: FlowComponentNamePanel
     */
    private class FlowComponentNamePanel extends HorizontalPanel {

        private final Label label = new Label(FLOW_COMPONENT_CREATION_KOMPONENT_NAVN_LABEL);
        private final TextBox textBox = new TextBox();

        public FlowComponentNamePanel() {
            super();
            add(label);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_NAME_PANEL);
            textBox.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX);
            textBox.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            add(textBox);
        }

        public String getFlowComponentName() {
            return textBox.getValue();
        }
    }

    /**
     * Panel: FlowComponentSvnProjectPanel
     */
    private class FlowComponentSvnProjectPanel extends HorizontalPanel {

        private final Label label = new Label(FLOW_COMPONENT_CREATION_SVN_PROJEKT_LABEL);
        private final TextBox svnProject = new TextBox();

        public FlowComponentSvnProjectPanel() {
            super();
            add(label);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SVN_PROJECT_PANEL);
            svnProject.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SVN_PROJECT_TEXT_BOX);
            add(svnProject);
            svnProject.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            svnProject.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    svnProjectChanged();
                }
            });
        }

        public String getProjectName() {
            return svnProject.getValue();
        }
    }

    /**
     * Panel: FlowComponentSvnRevisionPanel
     */
    private class FlowComponentSvnRevisionPanel extends HorizontalPanel {

        private final Label label = new Label(FLOW_COMPONENT_CREATION_SVN_REVISION_LABEL);
        private final ListBox svnRevision = new ListBox();

        public FlowComponentSvnRevisionPanel() {
            super();
            add(label);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_PANEL);
            svnRevision.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SVN_REVISION_LIST_BOX);
            add(svnRevision);
            svnRevision.setEnabled(false);
            svnRevision.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    svnRevisionChanged();
                }
            });
        }

        public void disable() {
            svnRevision.clear();
            svnRevision.setEnabled(false);
        }

        public void setRevisions(List<RevisionInfo> revisions) {
            svnRevision.clear();
            for (RevisionInfo revision : revisions) {
                svnRevision.addItem(String.valueOf(revision.getRevision()), HasDirection.Direction.RTL, String.valueOf(revision.hashCode()));
            }
            if (svnRevision.getItemCount() > 0) {
                svnRevision.setSelectedIndex(0);  // Set the first item to be selected
            }
            svnRevision.setEnabled(true);
        }

        public long getSelectedRevision() {
            int selectedRevisionIndex = svnRevision.getSelectedIndex();
            if (selectedRevisionIndex < 0) {
                return 0;
            }
            return Long.parseLong(svnRevision.getItemText(selectedRevisionIndex));
        }

        public void fireChangeEvent() {
            class RevisionChangedEvent extends ChangeEvent {
            }
            svnRevision.fireEvent(new RevisionChangedEvent());
        }
    }

    /**
     * Panel: FlowComponentScriptNamePanel
     */
    private class FlowComponentScriptNamePanel extends HorizontalPanel {

        private final Label label = new Label(FLOW_COMPONENT_CREATION_SCRIPT_NAME_LABEL);
        private final ListBox scriptName = new ListBox();

        public FlowComponentScriptNamePanel() {
            super();
            add(label);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_PANEL);
            scriptName.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SCRIPT_NAME_LIST_BOX);
            add(scriptName);
            scriptName.setEnabled(false);
            scriptName.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            scriptName.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    scriptNameChanged();
                }
            });
        }

        public void disable() {
            scriptName.clear();
            scriptName.setEnabled(false);
        }

        public void setScriptNames(List<String> fetchScriptNames) {
            scriptName.clear();
            for (String name : fetchScriptNames) {
                scriptName.addItem(name);
            }
            if (scriptName.getItemCount() > 0) {
                scriptName.setSelectedIndex(0);  // Set the first item to be selected
            }
            scriptName.setEnabled(true);
        }

        public String getScriptName() {
            int selectedScriptNameIndex = scriptName.getSelectedIndex();
            if (selectedScriptNameIndex < 0) {
                return "";
            }
            return scriptName.getItemText(selectedScriptNameIndex);
        }

        public void fireChangeEvent() {
            class ScriptNameChangedEvent extends ChangeEvent {
            }
            scriptName.fireEvent(new ScriptNameChangedEvent());
        }
    }

    /**
     * Panel: FlowComponentInvocationMethodPanel
     */
    private class FlowComponentInvocationMethodPanel extends HorizontalPanel {

        private final Label label = new Label(FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LABEL);
        private final ListBox invocationMethodName = new ListBox();

        public FlowComponentInvocationMethodPanel() {
            super();
            add(label);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL);
            invocationMethodName.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_LIST_BOX);
            add(invocationMethodName);
            invocationMethodName.setEnabled(false);
            invocationMethodName.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    invocationMethodNameChanged();
                }
            });
        }

        public void disable() {
            invocationMethodName.clear();
            invocationMethodName.setEnabled(false);
        }

        public void setInvocationMethods(List<String> fetchInvocationMethods) {
            invocationMethodName.clear();
            for (String method : fetchInvocationMethods) {
                invocationMethodName.addItem(method);
            }
            if (invocationMethodName.getItemCount() > 0) {
                invocationMethodName.setSelectedIndex(0);  // Set the first item to be selected
            }
            invocationMethodName.setEnabled(true);
        }

        public String getInvocationMethod() {
            int selectedInvocationMethodIndex = invocationMethodName.getSelectedIndex();
            if (selectedInvocationMethodIndex < 0) {
                return "";
            }
            return invocationMethodName.getItemText(selectedInvocationMethodIndex);
        }

        public void fireChangeEvent() {
            class InvocationMethodNameChangedEvent extends ChangeEvent {
            }
            invocationMethodName.fireEvent(new InvocationMethodNameChangedEvent());
        }
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
            String name = namePanel.getFlowComponentName();
            String project = projectPanel.getProjectName();
            long revision = revisionPanel.getSelectedRevision();
            String scriptName = scriptNamePanel.getScriptName();
            String invocationMethod = invocationMethodPanel.getInvocationMethod();
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
