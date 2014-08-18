package dk.dbc.dataio.gui.client.pages.flowcomponentcreateedit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.gui.client.components.ListEntry;
import dk.dbc.dataio.gui.client.components.SaveButton;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;


/**
 *
 * This is the implementation of the Flow Component Creation View
 *
 */
public class FlowComponentCreateEditViewImpl extends ContentPanel<FlowComponentCreateEditPresenter> implements FlowComponentCreateEditView {

    // public Identifiers
    public static final String GUIID_FLOW_COMPONENT_CREATION_EDIT_WIDGET = "flowcomponentcreationeditwidget";
    public static final String GUIID_FLOW_COMPONENT_CREATION_EDIT_NAME_PANEL = "flowcomponentcreationeditnamepanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_EDIT_PROJECT_PANEL = "flowcomponentcreationeditprojectpanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_EDIT_SVN_REVISION_PANEL = "flowcomponentcreationeditsvnrevisionpanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_EDIT_SCRIPT_NAME_PANEL = "flowcomponentcreationeditscriptnamepanel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_EDIT_INVOCATION_METHOD_PANEL = "flow-component-invocation-method-panel-id";
    public static final String GUIID_FLOW_COMPONENT_CREATION_EDIT_SAVE_BUTTON_PANEL = "flow-component-save-panel-id";

    // private objects
    private final static FlowComponentCreateEditConstants constants = GWT.create(FlowComponentCreateEditConstants.class);
    private TextEntry namePanel = new TextEntry(GUIID_FLOW_COMPONENT_CREATION_EDIT_NAME_PANEL, constants.label_ComponentName());
    private TextEntry projectPanel = new TextEntry(GUIID_FLOW_COMPONENT_CREATION_EDIT_PROJECT_PANEL, constants.label_SvnProject());
    private ListEntry revisionPanel = new ListEntry(GUIID_FLOW_COMPONENT_CREATION_EDIT_SVN_REVISION_PANEL, constants.label_SvnRevision());
    private ListEntry scriptNamePanel = new ListEntry(GUIID_FLOW_COMPONENT_CREATION_EDIT_SCRIPT_NAME_PANEL, constants.label_ScriptName());
    private ListEntry invocationMethodPanel = new ListEntry(GUIID_FLOW_COMPONENT_CREATION_EDIT_INVOCATION_METHOD_PANEL, constants.label_InvocationMethod());
    private SaveButton saveButton = new SaveButton(GUIID_FLOW_COMPONENT_CREATION_EDIT_SAVE_BUTTON_PANEL, constants.button_Save(), new SaveButtonEvent());
    private Label busyLabel = new Label(constants.status_Busy());

    /**
     * Constructor
     */
    public FlowComponentCreateEditViewImpl() {
        super("");  // An empty string is supplied, since we don't know yet, if this is a Create or an Edit view, please refer to initializeFields() below.
    }
        /**
         * Initializations of the view
         */
    public void init() {
        getElement().setId(GUIID_FLOW_COMPONENT_CREATION_EDIT_WIDGET);

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

        add(saveButton);
        add(busyLabel);
        setAsBusy(false);
    }

    /*
     * Implementation of interface methods
     */

    /**
     * Clear all fields in this view
     */
    @Override
    public void clearFields() {
        namePanel.clearText();
        projectPanel.clearText();
        revisionPanel.clear();
        scriptNamePanel.clear();
        invocationMethodPanel.clear();
    }

    /**
     * onFailure
     * @param message The fail message to display to the user
     */
    @Override
    public void onFailure(String message) {
        setAsBusy(false);
        saveButton.setStatusText("");
        Window.alert("Error: " + message);
    }

    /**
     * OnSuccess
     * @param message The message to display to the user
     */
    @Override
    public void setStatusText(String message) {
        saveButton.setStatusText(message);
    }

    /**
     * This method is called by the presenter, when signalling a successful save to the user
     */
    @Override
    public void onSaveFlowComponentSuccess() {
        setStatusText(constants.status_FlowComponentSuccessfullySaved());
    }


    /**
     * Initialize all fields in this view
     */
    @Override
    public void initializeFields(String header, FlowComponent flowComponent) {
        setHeader(header);
        clearFields();
        namePanel.setText(flowComponent.getContent().getName());
        projectPanel.setText(flowComponent.getContent().getSvnProjectForInvocationJavascript());
        projectPanel.fireChangeEvent();
    }

    /**
     *
     * @param availableRevisions all SVN revisions available for the flow component
     * @param currentRevision the current SVN revision
     */
    @Override
    public void setAvailableRevisions(List<RevisionInfo> availableRevisions, int currentRevision) {
        setAsBusy(false);
        revisionPanel.clear();
        if (!availableRevisions.isEmpty()) {
            for (RevisionInfo revision: availableRevisions) {
                revisionPanel.setAvailableItem(String.valueOf(revision.getRevision()));
                if (revision.getRevision() == currentRevision){
                    revisionPanel.setSelected(availableRevisions.indexOf(revision));
                }
            }
            revisionPanel.setEnabled(true);
            revisionPanel.fireChangeEvent();
        }
    }

    /**
     * This method is called by the presenter, when pushing Script Names to the view
     * @param availableScriptNames all scripts available for the current flow component
     * @param currentScriptName the name of the current script
     */
    @Override
    public void setAvailableScriptNames(List<String> availableScriptNames, String currentScriptName) {
        setAsBusy(false);
        scriptNamePanel.clear();
        if (!availableScriptNames.isEmpty()) {
            for (String scriptName: availableScriptNames) {
                scriptNamePanel.setAvailableItem(scriptName);
                if(scriptName.equals(currentScriptName)){
                    scriptNamePanel.setSelected(availableScriptNames.indexOf(scriptName));
                }
            }
            scriptNamePanel.setEnabled(true);
            scriptNamePanel.fireChangeEvent();
        }
    }

    /**
     * This method is called by the presenter, when pushing Invocation Method names to the view
     * @param availableInvocationMethods all available invocation methods for the script attached to the flow component
     * @param currentInvocationMethod the current invocation method
     */
    @Override
    public void setAvailableInvocationMethods(List<String> availableInvocationMethods, String currentInvocationMethod) {
        setAsBusy(false);
        invocationMethodPanel.clear();
        if (!availableInvocationMethods.isEmpty()) {
            for (String invocationMethod: availableInvocationMethods) {
                invocationMethodPanel.setAvailableItem(invocationMethod);
                if(invocationMethod.equals(currentInvocationMethod)){
                    invocationMethodPanel.setSelected(availableInvocationMethods.indexOf(invocationMethod));
                }
            }
            invocationMethodPanel.setEnabled(true);
            invocationMethodPanel.fireChangeEvent();
        }
    }

    /**
     * This method is called by the presenter, when signalling an error, when fetching Revisions.
     * @param errorCode Error code for the error
     * @param detail Details about the error
     */
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
                case SCM_RESOURCE_NOT_FOUND: errorMessage = constants.error_ScmProjectNotFoundError();
                    break;
                case SCM_ILLEGAL_PROJECT_NAME: errorMessage = constants.error_ScmIllegalProjectNameError();
                    break;
                default: errorMessage = detail;
                    break;
            }
        }
        onFailure(errorMessage);
    }

    /**
     * This method is called by the presenter, when signalling an error, when fetching Script Names
     * @param failText The error text
     */
    @Override
    public void fetchScriptNamesFailed(String failText) {
        scriptNamePanel.setEnabled(false);
        invocationMethodPanel.setEnabled(false);
        onFailure(failText);
    }

    /**
     * This method is called by the presenter, when signalling an error, when fetching Invocation Method names
     * @param errorCode Error code for the error
     * @param detail Details about the error
     */
    @Override
    public void fetchInvocationMethodsFailed(JavaScriptProjectFetcherError errorCode, String detail) {
        invocationMethodPanel.setEnabled(false);
        final String errorMessage;
        if (errorCode == null) {
            errorMessage = detail;
        } else {
            switch (errorCode) {
                case JAVASCRIPT_REFERENCE_ERROR: errorMessage = constants.error_JavaScriptReferenceError();
                    break;
                default: errorMessage = detail;
                    break;
            }
        }
        onFailure(errorMessage);
    }


    /*
     * Private methods
     */
    private void setAsBusy(Boolean busy) {
        busyLabel.setVisible(busy);
    }

    private void svnProjectChanged() {
        setAsBusy(true);
        saveButton.setStatusText("");
        presenter.projectNameEntered(projectPanel.getText());
    }

    private void svnRevisionChanged() {
        setAsBusy(true);
        saveButton.setStatusText("");
        presenter.revisionSelected(projectPanel.getText(), Long.parseLong(revisionPanel.getSelectedText()));
    }

    private void scriptNameChanged() {
        setAsBusy(true);
        saveButton.setStatusText("");
        presenter.scriptNameSelected(projectPanel.getText(), Long.parseLong(revisionPanel.getSelectedText()), scriptNamePanel.getSelectedText());
    }

    private void invocationMethodNameChanged() {
        saveButton.setStatusText("");
    }


    /**
     * Event Handlers *
     */


    private class SaveButtonEvent implements SaveButton.ButtonEvent {
        @Override
        public void buttonPressed() {
            String name = namePanel.getText();
            String project = projectPanel.getText();
            Long revision;
            try {
                revision = Long.parseLong(revisionPanel.getSelectedText());
            } catch (NumberFormatException e) {
                onFailure(constants.error_InputFieldValidationError());
                return;
            }
            String scriptName = scriptNamePanel.getSelectedText();
            String invocationMethod = invocationMethodPanel.getSelectedText();
            if (name.isEmpty() || project.isEmpty() || revision == 0 || scriptName.isEmpty() || invocationMethod.isEmpty()) {
                onFailure(constants.error_InputFieldValidationError());
            } else {
                saveButton.setStatusText(constants.status_SavingFlowComponent());
                presenter.saveFlowComponent(name, project, revision, scriptName, invocationMethod);
            }
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {
        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            saveButton.setStatusText("");
        }
    }
}

