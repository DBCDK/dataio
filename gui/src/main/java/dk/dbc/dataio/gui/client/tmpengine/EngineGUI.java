package dk.dbc.dataio.gui.client.tmpengine;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.components.TextEntry;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;

import java.util.List;

public class EngineGUI extends DockLayoutPanel {

    public static final String FORM_FIELD_DATA_FILE = "dataFile";
    public static final String FORM_FIELD_FLOW_ID = "formfieldflowid";
    FlowStoreProxyAsync flowStoreProxy = FlowStoreProxy.Factory.getAsyncInstance();
    private EngineFormPanel engineFormPanel = new EngineFormPanel();
    private FileNamePanel fileNamePanel = new FileNamePanel();
    private RunFlowPanel runFlowPanel = new RunFlowPanel();
    // transfiledata input information
    public static final String FORM_FIELD_TRANSFILE_FILENAME = "filenametextentry";
    public static final String FORM_FIELD_TRANSFILE_FORMAT = "formattextentry";
    public static final String FORM_FIELD_TRANSFILE_PACKAGING = "packagingtextentry";
    public static final String FORM_FIELD_TRANSFILE_CHARSET = "charsettextentry";
    public static final String FORM_FIELD_TRANSFILE_DESTINATION = "destinationtextentry";
    public static final String FORM_FIELD_TRANSFILE_VERIFICATION_MAIL = "verificationmailtextentry";
    public static final String FORM_FIELD_TRANSFILE_PROCESSING_MAIL = "processingmailtextentry";
    public static final String FORM_FIELD_TRANSFILE_RESULT_MAIL_INITIALS = "resultmailinitialstextentry";
    private LocalTextEntry filenameTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_FILENAME, "[f] filename");
    private LocalTextEntry formatTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_FORMAT, "[o] indholdsformat");
    private LocalTextEntry packagingTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_PACKAGING, "[t] rammeformat");
    private LocalTextEntry charsetTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_CHARSET, "[c] tegnsæt");
    private LocalTextEntry destinationTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_DESTINATION, "[b] destination");
    private LocalTextEntry verificationMailTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_VERIFICATION_MAIL, "[m] mailaddresse til endt verifikation af data");
    private LocalTextEntry processingMailTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_PROCESSING_MAIL, "[M] mailaddresse til endt processering af data");
    private LocalTextEntry resultMailInitialsTextEntry = new LocalTextEntry(FORM_FIELD_TRANSFILE_RESULT_MAIL_INITIALS, "[i] resultat mail identifikations initialer");

    public EngineGUI() {
        super(Style.Unit.PX);

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.add(fileNamePanel);
        // transfiledata input information: begin
        vpanel.add(new Label("******** Transfile inputfields below are currently unused! ********"));
        vpanel.add(filenameTextEntry);
        vpanel.add(formatTextEntry);
        vpanel.add(packagingTextEntry);
        vpanel.add(charsetTextEntry);
        vpanel.add(destinationTextEntry);
        vpanel.add(verificationMailTextEntry);
        vpanel.add(processingMailTextEntry);
        vpanel.add(resultMailInitialsTextEntry);
        // transfiledata input information: end
        vpanel.add(runFlowPanel);

        engineFormPanel.setWidget(vpanel);

        add(engineFormPanel);
    }

    private static FileUpload getFileUpload(String fieldName) {
        final FileUpload upload = new FileUpload();
        upload.setName(fieldName);
        return upload;
    }

    private class EngineFormPanel extends FormPanel {

        public EngineFormPanel() {
            super();
            // Point to service
            setAction(GWT.getModuleBaseURL() + "EmbeddedEngine");
            // Because we're going to add a FileUpload widget, we'll need to set the
            // form to use the POST method, and multipart MIME encoding.
            setEncoding(FormPanel.ENCODING_MULTIPART);
            setMethod(FormPanel.METHOD_POST);

            addSubmitHandler(new FormPanel.SubmitHandler() {
                public void onSubmit(SubmitEvent event) {
                    // This event is fired just before the form is submitted. We can
                    // take this opportunity to perform validation.
                    runFlowPanel.setStatusText("Processing...");
                }
            });

            addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
                public void onSubmitComplete(SubmitCompleteEvent event) {
                    // When the form submission is successfully completed, this
                    // event is fired.
                    runFlowPanel.setStatusText("Done");
                    runFlowPanel.add(new HTML(event.getResults()));
                }
            });
        }
    }

    private class FileNamePanel extends HorizontalPanel {

        private final Label label = new Label("Filename");
        private final FileUpload upload = getFileUpload(FORM_FIELD_DATA_FILE);

        public FileNamePanel() {
            super();
            add(label);
            add(upload);
        }

        public String getText() {
            return upload.getFilename();
        }
    }

    private class RunFlowPanel extends HorizontalPanel {

        private final Label runFlowResultLabel = new Label("");
        private final Button runFlowButton = new Button("Run flow", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                engineFormPanel.submit();
            }
        });

        public RunFlowPanel() {
            add(runFlowResultLabel);
            add(runFlowButton);
        }

        public void setStatusText(String statusText) {
            runFlowResultLabel.setText(statusText);
        }
    }


    // Trnasfiledata input field
    private class LocalTextEntry extends HorizontalPanel {

        private final Label label;
        private final TextBox textBox;

        public LocalTextEntry(String GUIID, String labelContent) {
            super();
            this.label = new Label(labelContent);

            textBox = new TextBox();
            textBox.setName(GUIID);
            add(label);
            add(textBox);
        }
    }
}
