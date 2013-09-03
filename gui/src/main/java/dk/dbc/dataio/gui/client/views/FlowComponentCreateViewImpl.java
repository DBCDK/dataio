package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.commons.types.RevisionInfo;
import java.util.List;

public class FlowComponentCreateViewImpl extends FormPanel implements FlowComponentCreateView {

    // public Identifiers
    public static final String CONTEXT_HEADER = "Flow Komponent - opsætning";
    public static final String GUIID_FLOW_COMPONENT_CREATION_WIDGET = "flowcomponentcreationwidget";
    public static final String GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX = "flowcomponentcreationnametextbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX = "flowcomponentcreationinvocationmethodtextbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL = "flowcomponentcreationsaveresultlabel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON = "flowcomponentcreationsavebutton";
    public static final String GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD = "flowcomponentcreationjavascriptfileupload";
    public static final String GUIID_FLOW_COMPONENT_CREATION_NAME_PANEL = "flow-component-name-panel-id";
    public static final String GUIID_FLOW_COMPONENT_CREATION_JAVA_SCRIPT_UPLOAD_PANEL = "flow-component-java-script-upload-panel-id";
    public static final String GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL = "flow-component-invocation-method-panel-id";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_PANEL = "flow-component-save-panel-id";
    public static final String FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    public static final String FORM_FIELD_COMPONENT_NAME = "formfieldcomponentname";
    public static final String FORM_FIELD_INVOCATION_METHOD = "formfieldinvocationmethod";
    public static final String FORM_FIELD_JAVASCRIPT_FILE_UPLOAD = "formfieldjavascriptfileupload";
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public static final String SAVE_RESULT_LABEL_PROCESSING_MESSAGE = "Opsætningen gemmes...";
    // private objects
    private FlowComponentCreatePresenter presenter;
    private VerticalPanel localPanel = new VerticalPanel();
    private FlowComponentNamePanel namePanel = new FlowComponentNamePanel();
    private FlowComponentSvnProjectPanel projectPanel = new FlowComponentSvnProjectPanel();
    private FlowComponentSvnRevisionPanel revisionPanel = new FlowComponentSvnRevisionPanel();
    private FlowComponentScriptNamePanel scriptNamePanel = new FlowComponentScriptNamePanel();
    private FlowComponentInvocationMethodPanel invocationMethodPanel = new FlowComponentInvocationMethodPanel();
    private FlowComponentSavePanel savePanel = new FlowComponentSavePanel();
    private Label busyLabel = new Label("Busy...");

    class ItemChangedEvent extends ChangeEvent {}

    public FlowComponentCreateViewImpl() {
        super();

        getElement().setId(GUIID_FLOW_COMPONENT_CREATION_WIDGET);
        setWidget(localPanel);
        localPanel.add(namePanel);
        localPanel.add(projectPanel);
        localPanel.add(revisionPanel);
        localPanel.add(scriptNamePanel);
        localPanel.add(invocationMethodPanel);
        localPanel.add(savePanel);
        localPanel.add(busyLabel);
        setAsBusy(false);
        addSubmitHandler(new FormPanel.SubmitHandler() {
            public void onSubmit(SubmitEvent event) {
                // This event is fired just before the form is submitted. We can
                // take this opportunity to perform validation.
                // flowComponentSavePanel.setStatusText(SAVE_RESULT_LABEL_PROCESSING_MESSAGE);
                setAsBusy(true);
            }
        });
        addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            public void onSubmitComplete(SubmitCompleteEvent event) {
                // When the form submission is successfully completed, this
                // event is fired.
                setAsBusy(false);
                savePanel.setStatusText(SAVE_RESULT_LABEL_SUCCES_MESSAGE);
            }
        });
    }

    @Override
    public void setPresenter(FlowComponentCreatePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void displayError(String message) {
        setAsBusy(false);
        Window.alert("Error: " + message);
    }

    @Override
    public void displaySuccess(String message) {
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
        revisionPanel.setRevisions(availableRevisions);
    }

    @Override
    public void setAvailableScriptNames(List<String> availableScriptNames) {
        scriptNamePanel.setScriptNames(availableScriptNames);
    }

    @Override
    public void setAvailableInvocationMethods(List<String> availableInvocationMethods) {
        invocationMethodPanel.setInvocationMethods(availableInvocationMethods);
    }

    @Override
    public void disableRevisionEntry() {
        revisionPanel.disable();
    }

    @Override
    public void disableScriptNameEntry() {
        scriptNamePanel.disable();
    }

    @Override
    public void disableInvocationMethodEntry() {
        invocationMethodPanel.disable();
    }

    /**
     * Panel: FlowComponentNamePanel
     */
    private class FlowComponentNamePanel extends HorizontalPanel {

        private final Label label = new Label("Komponentnavn");
        private final TextBox textBox = new TextBox();

        public FlowComponentNamePanel() {
            super();
            add(label);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_NAME_PANEL);
            textBox.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX);
            textBox.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            textBox.setName(FORM_FIELD_COMPONENT_NAME);
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

        private final Label label = new Label("SVN Projekt");
        private final TextBox svnProject = new TextBox();

        public FlowComponentSvnProjectPanel() {
            super();
            add(label);
            add(svnProject);
            svnProject.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    setAsBusy(true);
                    presenter.projectNameEntered(getProjectName());
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

        private final Label label = new Label("SVN Revision");
        private final ListBox svnRevision = new ListBox();

        public FlowComponentSvnRevisionPanel() {
            super();
            add(label);
            add(svnRevision);
            svnRevision.setEnabled(false);
            svnRevision.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    setAsBusy(true);
                    presenter.revisionSelected(getSelectedRevision());
                }
            });
        }
        
        public void disable() {
            svnRevision.clear();
            svnRevision.setEnabled(false);
        }

        public void setRevisions(List<RevisionInfo> revisions) {
            svnRevision.clear();
            if (revisions.size() != 0) {
                for (RevisionInfo revision: revisions) {
                    svnRevision.addItem(String.valueOf(revision.getRevision()), HasDirection.Direction.RTL, String.valueOf(revision.hashCode()));
                }
                svnRevision.setSelectedIndex(0);  // Set the first item to be selected
            }
            svnRevision.setEnabled(true);
            setAsBusy(false);
            svnRevision.fireEvent(new ItemChangedEvent());
        }

        public long getSelectedRevision() {
            return Long.parseLong(svnRevision.getItemText(svnRevision.getSelectedIndex()));
        }
    }

    /**
     * Panel: FlowComponentScriptNamePanel
     */
    private class FlowComponentScriptNamePanel extends HorizontalPanel {

        private final Label label = new Label("Script navn");
        private final ListBox scriptName = new ListBox();

        public FlowComponentScriptNamePanel() {
            super();
            add(label);
            add(scriptName);
            scriptName.setEnabled(false);
            scriptName.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    setAsBusy(true);
                    presenter.scriptNameSelected(getScriptName());
                }
            });
        }
 
        public void disable() {
            scriptName.clear();
            scriptName.setEnabled(false);
        }

        public void setScriptNames(List<String> fetchScriptNames) {
            scriptName.clear();
            for (String name: fetchScriptNames) {
                scriptName.addItem(name);
            }
            scriptName.setEnabled(true);
            setAsBusy(false);
            scriptName.fireEvent(new ItemChangedEvent());
        }
        
        public String getScriptName() {
            return scriptName.getItemText(scriptName.getSelectedIndex());
        }
    }
    
    /**
     * Panel: FlowComponentInvocationMethodPanel
     */
    private class FlowComponentInvocationMethodPanel extends HorizontalPanel {

        private final Label label = new Label("Invocation Method");
        private final ListBox invocationMethodName = new ListBox();

        public FlowComponentInvocationMethodPanel() {
            super();
            add(label);
            getElement().setId(GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_PANEL);
            invocationMethodName.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX);
            invocationMethodName.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            invocationMethodName.setName(FORM_FIELD_INVOCATION_METHOD);
            add(invocationMethodName);
            invocationMethodName.setEnabled(false);
        }

        public void disable() {
            invocationMethodName.clear();
            invocationMethodName.setEnabled(false);
        }

        public void setInvocationMethods(List<String> fetchInvocationMethods) {
            invocationMethodName.clear();
            for (String method: fetchInvocationMethods) {
                invocationMethodName.addItem(method);
            }
            invocationMethodName.setEnabled(true);
            setAsBusy(false);
            invocationMethodName.fireEvent(new ItemChangedEvent());
        }

        public String getInvocationMethod() {
            return invocationMethodName.getItemText(invocationMethodName.getSelectedIndex());
        }
    }

    /**
     * Panel: FlowComponentSavePanel
     */
    private class FlowComponentSavePanel extends HorizontalPanel {

        private final Button flowComponentSaveButton = new Button("Gem");
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

    
    
    /** Event Handlers **/
    
    private class SaveButtonHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            String name = namePanel.getFlowComponentName();
            String project = projectPanel.getProjectName();
            long revision = revisionPanel.getSelectedRevision();
            String scriptName = scriptNamePanel.getScriptName();
            String invocationMethod = invocationMethodPanel.getInvocationMethod();
            Window.alert("Component name: " + name + ", Project name: " + project + ", Revision: " + revision + ", Script naem: " + scriptName + ", Invocation Method: " + invocationMethod);
            if (name.isEmpty() || 
                project.isEmpty() ||
                (revision == 0) ||
                scriptName.isEmpty() ||
                invocationMethod.isEmpty() ) {
                Window.alert(FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR);
            } else {
                submit();
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
