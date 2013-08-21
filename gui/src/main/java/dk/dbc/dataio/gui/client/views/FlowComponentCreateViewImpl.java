package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class FlowComponentCreateViewImpl extends FormPanel implements FlowComponentCreateView {

    // public Identifiers
    public static final String CONTEXT_HEADER = "Flow Komponent - opsætning";
    public static final String GUIID_FLOW_COMPONENT_CREATION_WIDGET = "flowcomponentcreationwidget";
    public static final String GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX = "flowcomponentcreationnametextbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX = "flowcomponentcreationinvocationmethodtextbox";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL = "flowcomponentcreationsaveresultlabel";
    public static final String GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON = "flowcomponentcreationsavebutton";
    public static final String GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD = "flowcomponentcreationjavascriptfileupload";
    public static final String FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    public static final String FORM_FIELD_COMPONENT_NAME = "formfieldcomponentname";
    public static final String FORM_FIELD_INVOCATION_METHOD = "formfieldinvocationmethod";
    public static final String FORM_FIELD_JAVASCRIPT_FILE_UPLOAD = "formfieldjavascriptfileupload";
    public static final String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public static final String SAVE_RESULT_LABEL_PROCESSING_MESSAGE = "Opsætningen gemmes...";
    // private objects
    private FlowComponentCreatePresenter presenter;
    private VerticalPanel localPanel = new VerticalPanel();
    private FlowComponentNamePanel flowComponentNamePanel = new FlowComponentNamePanel();
    private FlowComponentJavaScriptUploadPanel flowComponentJavaScriptUploadPanel = new FlowComponentJavaScriptUploadPanel();
    private FlowComponentInvocationMethodPanel flowComponentInvocationMethodPanel = new FlowComponentInvocationMethodPanel();
    private FlowComponentSavePanel flowComponentSavePanel = new FlowComponentSavePanel();

    public FlowComponentCreateViewImpl() {
        super();

        getElement().setId(GUIID_FLOW_COMPONENT_CREATION_WIDGET);
        setWidget(localPanel);
        localPanel.add(flowComponentNamePanel);
        localPanel.add(flowComponentJavaScriptUploadPanel);
        localPanel.add(flowComponentInvocationMethodPanel);
        localPanel.add(flowComponentSavePanel);

        setAction(GWT.getModuleBaseURL() + "JavascriptUpload");
        // Because we're going to add a FileUpload widget, we'll need to set the
        // form to use the POST method, and multipart MIME encoding.
        setEncoding(FormPanel.ENCODING_MULTIPART);
        setMethod(FormPanel.METHOD_POST);

        addSubmitHandler(new FormPanel.SubmitHandler() {
            public void onSubmit(SubmitEvent event) {
                // This event is fired just before the form is submitted. We can
                // take this opportunity to perform validation.
                // flowComponentSavePanel.setStatusText(SAVE_RESULT_LABEL_PROCESSING_MESSAGE);
            }
        });

        addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            public void onSubmitComplete(SubmitCompleteEvent event) {
                // When the form submission is successfully completed, this
                // event is fired.
                flowComponentSavePanel.setStatusText(SAVE_RESULT_LABEL_SUCCES_MESSAGE);
            }
        });
    }

    @Override
    public void setPresenter(FlowComponentCreatePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void displayError(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void displaySuccess(String message) {
        flowComponentSavePanel.setStatusText(message);
    }

    @Override
    public void refresh() {
    }

    private class FlowComponentNamePanel extends HorizontalPanel {

        private final Label label = new Label("Komponentnavn");
        private final TextBox textBox = new TextBox();

        public FlowComponentNamePanel() {
            super();
            add(label);
            addStyleName("flow-component-name-panel");
            textBox.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_NAME_TEXT_BOX);
            textBox.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            textBox.setName(FORM_FIELD_COMPONENT_NAME);
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }

    private class FlowComponentJavaScriptUploadPanel extends HorizontalPanel {

        private final Label label = new Label("Javascript");
        private final FileUpload fileUpload = new FileUpload();

        public FlowComponentJavaScriptUploadPanel() {
            super();
            add(label);
            addStyleName("flow-component-java-script-upload-panel");
            fileUpload.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_JAVASCRIPT_FILE_UPLOAD);
            fileUpload.setName(FORM_FIELD_JAVASCRIPT_FILE_UPLOAD);
            add(fileUpload);
            fileUpload.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    flowComponentSavePanel.setStatusText("");
                }
            });
        }

        public String getFilename() {
            return fileUpload.getFilename();
        }
    }

    private class FlowComponentInvocationMethodPanel extends HorizontalPanel {

        private final Label label = new Label("Invocation Method");
        private final TextBox textBox = new TextBox();

        public FlowComponentInvocationMethodPanel() {
            super();
            add(label);
            addStyleName("flow-component-invocation-method-panel");
            textBox.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_INVOCATION_METHOD_TEXT_BOX);
            textBox.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            textBox.setName(FORM_FIELD_INVOCATION_METHOD);
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }

    private class FlowComponentSavePanel extends HorizontalPanel {

        private final Button flowComponentSaveButton = new Button("Gem");
        private final Label flowComponentSaveResultLabel = new Label("");

        public FlowComponentSavePanel() {
            flowComponentSaveResultLabel.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SAVE_RESULT_LABEL);
            add(flowComponentSaveResultLabel);
            addStyleName("flow-component-save-panel");
            flowComponentSaveButton.getElement().setId(GUIID_FLOW_COMPONENT_CREATION_SAVE_BUTTON);
            flowComponentSaveButton.addClickHandler(new SaveButtonHandler());
            add(flowComponentSaveButton);
        }

        public void setStatusText(String statusText) {
            flowComponentSaveResultLabel.setText(statusText);
        }
    }

    private class SaveButtonHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            String nameValue = flowComponentNamePanel.getText();
            String invocationMethodValue = flowComponentInvocationMethodPanel.getText();
            String javascriptFileUploadValue = flowComponentJavaScriptUploadPanel.getFilename();
            if (nameValue.isEmpty() || invocationMethodValue.isEmpty() || javascriptFileUploadValue.isEmpty()) {
                Window.alert(FLOW_COMPONENT_CREATION_INPUT_FIELD_VALIDATION_ERROR);
            } else {
                submit();
            }
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {

        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            flowComponentSavePanel.setStatusText("");
        }
    }
}
