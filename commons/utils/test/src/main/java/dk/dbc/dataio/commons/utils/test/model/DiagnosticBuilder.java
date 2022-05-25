package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.Diagnostic;

public class DiagnosticBuilder {
    private Diagnostic.Level level = Diagnostic.Level.FATAL;
    private String message = "diagnostic message";
    private String stackTrace = null;
    private String tag = null;
    private String attribute = null;

    public DiagnosticBuilder setLevel(Diagnostic.Level level) {
        this.level = level;
        return this;
    }

    public DiagnosticBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public DiagnosticBuilder setStacktrace(String stacktrace) {
        this.stackTrace = stacktrace;
        return this;
    }

    public DiagnosticBuilder setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public DiagnosticBuilder setAttribute(String attribute) {
        this.attribute = attribute;
        return this;
    }

    public Diagnostic build() {
        return new Diagnostic(level, message, stackTrace, tag, attribute);
    }
}
