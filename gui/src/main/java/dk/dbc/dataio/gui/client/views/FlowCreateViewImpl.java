package dk.dbc.dataio.gui.client.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.ContentView.Presenter;

/**
 *
 * @author slf
 */
public class FlowCreateViewImpl extends VerticalPanel implements FlowCreateView {
    // Constants (These are not all private since we use them in the selenium tests)
    public final static String CONTEXT_HEADER = "Flow - opsætning";
    public final static String GUIID_FLOW_CREATION_WIDGET = "flowcreationwidget";
    public final static String GUIID_FLOW_CREATION_NAME_TEXT_BOX = "flowcreationnametextbox";
    public final static String GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA = "flowcreationdescriptiontextarea";
    public final static String GUIID_FLOW_CREATION_SAVE_BUTTON = "flowcreationsavebutton";
    public final static String GUIID_FLOW_CREATION_SAVE_RESULT_LABEL = "flowcreationsaveresultlabel";
    public final static String SAVE_RESULT_LABEL_SUCCES_MESSAGE = "Opsætningen blev gemt";
    public final static String FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR = "Alle felter skal udfyldes.";
    private final static int FLOW_CREATION_DESCRIPTION_MAX_LENGTH = 160;
    
    // Local variables
    private Presenter presenter;
    private final FlowNamePanel flowNamePanel = new FlowNamePanel();
    private final FlowDescriptionPanel flowDescriptionPanel = new FlowDescriptionPanel();
    private final FlowSavePanel flowSavePanel = new FlowSavePanel();
    private FlowStoreProxyAsync flowStoreProxy = FlowStoreProxy.Factory.getAsyncInstance();

    public FlowCreateViewImpl() {
        getElement().setId(GUIID_FLOW_CREATION_WIDGET);
        add(flowNamePanel);
        add(flowDescriptionPanel);
        add(flowSavePanel);
    }

    private class FlowNamePanel extends HorizontalPanel {

        private final Label label = new Label("Flownavn");
        private final TextBox textBox = new TextBox();

        public FlowNamePanel() {
            super();
            add(label);
            textBox.getElement().setId(GUIID_FLOW_CREATION_NAME_TEXT_BOX);
            textBox.addKeyDownHandler(new InputFieldKeyDownHandler());
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }

    private class FlowDescriptionPanel extends HorizontalPanel {

        private final Label flowDescriptionLabel = new Label("Beskrivelse");
        private final TextArea flowDescriptionTextArea = new FlowDescriptionTextArea();

        public FlowDescriptionPanel() {
            add(flowDescriptionLabel);
            add(flowDescriptionTextArea);
        }

        public String getText() {
            return flowDescriptionTextArea.getValue();
        }

        private class FlowDescriptionTextArea extends TextArea {

            public FlowDescriptionTextArea() {
                super();
                setCharacterWidth(40);
                setVisibleLines(4);
                getElement().setAttribute("Maxlength", String.valueOf(FLOW_CREATION_DESCRIPTION_MAX_LENGTH));
                getElement().setId(GUIID_FLOW_CREATION_DESCRIPTION_TEXT_AREA);
                addKeyDownHandler(new InputFieldKeyDownHandler());
            }
        }
    }

    private class FlowSavePanel extends HorizontalPanel {

        private final Button flowSaveButton = new Button("Gem");
        private final Label flowSaveResultLabel = new Label("");

        public FlowSavePanel() {
            flowSaveResultLabel.getElement().setId(GUIID_FLOW_CREATION_SAVE_RESULT_LABEL);
            add(flowSaveResultLabel);
            flowSaveButton.getElement().setId(GUIID_FLOW_CREATION_SAVE_BUTTON);
            flowSaveButton.addClickHandler(new SaveButtonHandler());
            add(flowSaveButton);
        }

        public void setStatusText(String statusText) {
            flowSaveResultLabel.setText(statusText);
        }
    }

    private class SaveButtonHandler implements ClickHandler {

        @Override
        public void onClick(ClickEvent event) {
            String nameValue = flowNamePanel.getText();
            String descriptionValue = flowDescriptionPanel.getText();
            if (!nameValue.isEmpty() && !descriptionValue.isEmpty()) {
                presenter.saveFlow(flowNamePanel.getText(), flowDescriptionPanel.getText());
            } else {
                Window.alert(FLOW_CREATION_INPUT_FIELD_VALIDATION_ERROR);
            }
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {

        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            flowSavePanel.setStatusText("");
        }
    }

    @Override
    public void setPresenter(Presenter presenter) { 
        this.presenter = presenter;
    }

    @Override
    public void setData(String name, String description) {
        // set data
    }

    @Override
    public void displayError(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void displaySuccess(String message) {
        flowSavePanel.setStatusText(message);
    }

    @Override
    public void refresh() {
    }
}
