package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.DiagnosticModel;

public class DiagnosticModelBuilder {

    private String level = "WARNING";
    private String message = "Diagnostic message";
    private String stacktrace = "Diagnostic stacktrace";

    public DiagnosticModelBuilder setLevel(String level) {
        this.level = level;
        return this;
    }

    public DiagnosticModelBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public DiagnosticModelBuilder setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
        return this;
    }

    public DiagnosticModel build() {
        return new DiagnosticModel(level, message, stacktrace);
    }


}
