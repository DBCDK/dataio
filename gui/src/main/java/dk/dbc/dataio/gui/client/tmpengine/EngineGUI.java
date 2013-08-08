package dk.dbc.dataio.gui.client.tmpengine;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class EngineGUI extends DockLayoutPanel {

    private EngineNamePanel engineNamePanel = new EngineNamePanel();
    
    public EngineGUI() {
        super(Style.Unit.PX);
        
        add(engineNamePanel);
    
    }

    private class EngineNamePanel extends HorizontalPanel {

        private final Label label = new Label("Engine");
        private final TextBox textBox = new TextBox();

        public EngineNamePanel() {
            super();
            add(label);
            // textBox.getElement().setId(GUIID_FLOW_CREATION_NAME_TEXT_BOX);
            textBox.addKeyDownHandler(new InputFieldKeyDownHandler());
            add(textBox);
        }

        public String getText() {
            return textBox.getValue();
        }
    }



    private class InputFieldKeyDownHandler implements KeyDownHandler {

        @Override
        public void onKeyDown(KeyDownEvent keyDownEvent) {
            // flowSavePanel.setStatusText("");
        }
    }
}
