package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateResponse;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.ValidationMessage;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.ValidationStatus;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.binding.SubField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class ValidationMessageInterpreter {
    static final String DELETE_NONEXISTENT_RECORD_MESSAGE = "Posten kan ikke slettes, da den ikke findes";

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationMessageInterpreter.class);
    private static final String FIELD_PREPEND = "felt ";
    private static final String SUBFIELD_PREPEND = "delfelt ";

    private final Set<String> ignoredValidationErrors;

    ValidationMessageInterpreter(Set<String> ignoredValidationErrors) {
        this.ignoredValidationErrors = ignoredValidationErrors;
    }

    List<Diagnostic> getDiagnostics(UpdateResponse response, MarcBinding marcBinding) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (response == null || response.getStatus() == null || response.getErrors() == null || response.getErrors().isEmpty()) {
            return diagnostics;
        }
        for (ValidationMessage message : response.getErrors()) {
            String text = message.getMessage();
            if (DELETE_NONEXISTENT_RECORD_MESSAGE.equals(text)) {
                LOGGER.debug("Update service result contains non-fatal '{}' message", text);
                // Deleting a record that doesn't exist is treated as a no-op success.
                // Clear any diagnostics accumulated from earlier messages in this response
                // so the whole update is considered OK.
                diagnostics.clear();
                return diagnostics;
            }
            if (!isIgnorable(text)) {
                diagnostics.add(toDiagnostic(message, marcBinding));
            }
        }
        return diagnostics;
    }

    private Diagnostic toDiagnostic(ValidationMessage message, MarcBinding marcBinding) {
        String fieldTag = null;
        String subfieldCode = null;
        try {
            if (marcBinding != null) {
                Integer fieldOrdinal = message.getOrdinalPositionOfField();
                if (fieldOrdinal != null) {
                    DataField dataField = marcBinding.getDataFields(MarcBinding.Sort.BY_OFFSET_AND_TAG).get(fieldOrdinal);
                    fieldTag = dataField.getTag();
                    Integer subfieldOrdinal = message.getOrdinalPositionOfSubfield();
                    if (subfieldOrdinal != null) {
                        SubField subField = dataField.getSubFields().get(subfieldOrdinal);
                        subfieldCode = String.valueOf(subField.getCode());
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Exception caught during extraction of field tag and/or subfield code", e);
        }
        return new Diagnostic(
                diagnosticLevel(message.getType()),
                message.getMessage(),
                null,
                fieldTag != null ? FIELD_PREPEND + fieldTag : null,
                fieldTag != null && subfieldCode != null ? SUBFIELD_PREPEND + subfieldCode : null);
    }

    private Diagnostic.Level diagnosticLevel(ValidationStatus type) {
        if (type == ValidationStatus.WARNING) {
            return Diagnostic.Level.WARNING;
        }
        if (type == ValidationStatus.ERROR) {
            return Diagnostic.Level.ERROR;
        }
        return Diagnostic.Level.FATAL;
    }

    private boolean isIgnorable(String message) {
        if (ignoredValidationErrors == null || message == null) {
            return false;
        }
        return ignoredValidationErrors.stream().anyMatch(message::contains);
    }
}
