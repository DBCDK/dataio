package dk.dbc.dataio.commons.utils.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HowRU {
    private static final JSONBContext jsonbContext = new JSONBContext();
    private boolean ok = true;
    private String errorText;
    private Error error;

    public boolean isOk() {
        return ok;
    }

    public HowRU withOk(boolean ok) {
        this.ok = ok;
        return this;
    }

    public String getErrorText() {
        return errorText;
    }

    public HowRU withErrorText(String errorText) {
        this.errorText = errorText;
        return this;
    }

    public Error getError() {
        return error;
    }

    public HowRU setError(Error error) {
        this.error = error;
        return this;
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public HowRU withException(Exception e) {
        ok = false;
        errorText = e.getMessage();
        try {
            error = new Error()
                    .withMessage(e.getMessage())
                    .withStacktrace(StringUtil.getStackTraceString(e));
        } catch (RuntimeException runtimeException) {
            // Unable to serialize stacktrace
        }
        return this;
    }

    public String toJson() {
        try {
            return jsonbContext.marshall(this);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private String message;
        private String stacktrace;

        public String getMessage() {
            return message;
        }

        public Error withMessage(String message) {
            this.message = message;
            return this;
        }

        public String getStacktrace() {
            return stacktrace;
        }

        public Error withStacktrace(String stacktrace) {
            this.stacktrace = stacktrace;
            return this;
        }
    }
}
