package dk.dbc.dataio.gui.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DiagnosticModel implements IsSerializable {
    private String level;
    private String message;
    private String stacktrace;

    public DiagnosticModel(String level, String message, String stacktrace) {
        this.level = level;
        this.message = message;
        this.stacktrace = stacktrace;
    }

    public DiagnosticModel() {
        this("", "", "");
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }
}
