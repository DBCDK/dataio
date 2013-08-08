package dk.dbc.dataio.gui.client.tmpengine;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.proxies.EmbeddedEngine;
import dk.dbc.dataio.gui.client.proxies.EmbeddedEngineAsync;

public class EngineGUI extends DockLayoutPanel {

    private FileNamePanel fileNamePanel = new FileNamePanel();
    private FlowPanel flowPanel = new FlowPanel();
    private RunFlowPanel runFlowPanel = new RunFlowPanel();

    private final EmbeddedEngineAsync embeddedEngine;
    
    public EngineGUI() {
        super(Style.Unit.PX);
 
        VerticalPanel vpanel = new VerticalPanel();
        vpanel.add(fileNamePanel);
        vpanel.add(flowPanel);
        vpanel.add(runFlowPanel);
        add(vpanel);
    
        embeddedEngine = EmbeddedEngine.Factory.getAsyncInstance();
    }

    private class FileNamePanel extends HorizontalPanel {

        private final Label label = new Label("Filename");
        private final TextBox textBox = new TextBox();

        public FileNamePanel() {
            super();
            add(label);
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }

    private class FlowPanel extends HorizontalPanel {

        private final Label label = new Label("Flow");
        private final TextBox textBox = new TextBox();

        public FlowPanel() {
            super();
            add(label);
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }
    
    private class RunFlowPanel extends HorizontalPanel {

        private final Button runFlowButton = new Button("Run flow");
        private final Label runFlowResultLabel = new Label("");

        public RunFlowPanel() {
            add(runFlowResultLabel);
            runFlowButton.addClickHandler(new SaveButtonHandler());
            add(runFlowButton);
        }

        public void setStatusText(String statusText) {
            runFlowResultLabel.setText(statusText);
        }
    }

    private class SaveButtonHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            runFlowPanel.setStatusText("Processing");
            
            String nameValue = fileNamePanel.getText();
            String descriptionValue = flowPanel.getText();

            try {
                embeddedEngine.executeJob(nameValue, descriptionValue, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        runFlowPanel.setStatusText("Error: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        runFlowPanel.setStatusText("Done");
                    }
                });
            } catch (Exception ex) {
                runFlowPanel.setStatusText("Exception: " + ex.getMessage());
            }
        }
    }

}
