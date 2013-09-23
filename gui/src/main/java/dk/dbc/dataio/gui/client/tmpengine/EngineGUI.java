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
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;

import java.util.List;

public class EngineGUI extends DockLayoutPanel {

    FlowStoreProxyAsync flowStoreProxy = FlowStoreProxy.Factory.getAsyncInstance();
    public static final String FORM_FIELD_DATA_FILE = "dataFile";
    public static final String FORM_FIELD_FLOW_ID = "formfieldflowid";
    private EngineFormPanel engineFormPanel = new EngineFormPanel();
    private FileNamePanel fileNamePanel = new FileNamePanel();
    private FlowPanel flowPanel = new FlowPanel();
    private RunFlowPanel runFlowPanel = new RunFlowPanel();

    public EngineGUI() {
        super(Style.Unit.PX);

        getFlows();

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.add(fileNamePanel);
        vpanel.add(flowPanel);
        vpanel.add(runFlowPanel);


        engineFormPanel.setWidget(vpanel);

        add(engineFormPanel);
    }

    private static FileUpload getFileUpload(String fieldName) {
        final FileUpload upload = new FileUpload();
        upload.setName(fieldName);
        return upload;
    }

    public void getFlows() {
        flowStoreProxy.findAllFlows(new AsyncCallback<List<Flow>>() {
            @Override
            public void onFailure(Throwable caught) {
                runFlowPanel.setStatusText("Error!!!!");
                Window.alert(caught.getMessage());
            }

            @Override
            public void onSuccess(List<Flow> flows) {
                // runFlowPanel.setStatusText("Size: " + flows.size());
                flowPanel.setFlows(flows);
            }
        });
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

    private class FlowPanel extends HorizontalPanel {

        private final Label label = new Label("Flow");
        private final ListBox listBox = new ListBox();

        public FlowPanel() {
            super();
            add(label);
            add(listBox);
            listBox.setName(FORM_FIELD_FLOW_ID);
        }

        public void setFlows(List<Flow> flows) {
            listBox.clear();
            for (Flow flow : flows) {
                String flowId = Long.toString(flow.getId()) + "-" + Long.toString(flow.getVersion());
                listBox.addItem(flow.getContent().getName(), flowId);
            }
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
}
