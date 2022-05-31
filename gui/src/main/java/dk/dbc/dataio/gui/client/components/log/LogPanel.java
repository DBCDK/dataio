package dk.dbc.dataio.gui.client.components.log;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Label;

import java.util.Date;

public class LogPanel extends Label {
    private StringBuilder logMessageBuilder;
    private StringBuilder logHistoryBuilder;
    private static final String GUID_LOG_PANEL = "log-panel";

    public LogPanel() {
        super();
        setup();
        logMessageBuilder = new StringBuilder();
        logHistoryBuilder = new StringBuilder();
    }

    public void showMessage(String message) {
        formatLogMessage(logMessageBuilder, message);
        formatLogMessage(logHistoryBuilder, message);
        setText(logMessageBuilder.toString());
    }

    public void showLog() {
        setText(logMessageBuilder.toString());
    }

    public void showHistory() {
        setText(logHistoryBuilder.toString());
    }

    public void clear() {
        setText("");
        logMessageBuilder = new StringBuilder();
    }

    /* private methods */
    private void setup() {
        Element element = getElement();
        element.setId(GUID_LOG_PANEL);
        element.setPropertyObject(GUID_LOG_PANEL, this);
        setSize("100%", "100%");
        element.getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
        element.getStyle().setMargin(4, Style.Unit.PX);
        element.getStyle().setColor("darkslategray");
        setVisible(true);
    }

    private void formatLogMessage(StringBuilder stringBuilder, String message) {
        stringBuilder.insert(0, new Date() + ": " + message + "\n");
    }

}
