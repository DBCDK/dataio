package dk.dbc.dataio.gui.client.views;

import dk.dbc.dataio.gui.client.presenters.FlowComponentCreatePresenter;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import static dk.dbc.dataio.gui.client.views.FlowCreateViewImpl.GUIID_FLOW_CREATION_NAME_TEXT_BOX;

public class FlowComponentCreateViewImpl extends VerticalPanel implements FlowComponentCreateView {

    public static final String CONTEXT_HEADER = "Flow Komponent - opsætning";
    public static final String GUIID_FLOW_COMPONENT_CREATION_WIDGET = "flowcomponentcreationwidget";
    private FlowComponentCreatePresenter presenter;
    private FlowComponentNamePanel flowComponentNamePanel = new FlowComponentNamePanel();
    
    public FlowComponentCreateViewImpl() {
        getElement().setId(GUIID_FLOW_COMPONENT_CREATION_WIDGET);
        add(flowComponentNamePanel);
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            textBox.getElement().setId(GUIID_FLOW_CREATION_NAME_TEXT_BOX);
            textBox.addKeyDownHandler(new FlowComponentCreateViewImpl.InputFieldKeyDownHandler());
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }

    private class InputFieldKeyDownHandler implements KeyDownHandler {

        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            //flowSavePanel.setStatusText("");
        }
    }
}
