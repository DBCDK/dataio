package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This class converts any error messages contained in an UpdateRecordResult into a list of diagnostics
 */
class UpdateRecordErrorInterpreter {
    static final String NON_FATAL_ERROR_MESSAGE = "Posten kan ikke slettes, da den ikke findes";

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecordErrorInterpreter.class);
    private static final String NO_STACK_TRACE = null;
    private static final String FIELD_PREPEND = "felt ";
    private static final String SUBFIELD_PREPEND = "delfelt ";

    private Set<String> ignoredValidationErrors;

    public UpdateRecordErrorInterpreter() {
    }

    public UpdateRecordErrorInterpreter(Set<String> ignoredValidationErrors) {
        this.ignoredValidationErrors = ignoredValidationErrors;
    }

    /**
     * Interprets an UpdateRecordResult and returns a list of diagnostics
     *
     * @param updateRecordResult web service result to interpret
     * @param addiRecord         Addi record containing the corresponding MARC record used to access the field and subfield
     * @return list of Diagnostics
     */
    List<Diagnostic> getDiagnostics(UpdateRecordResult updateRecordResult, AddiRecord addiRecord) {
        final List<Diagnostic> diagnostics = new ArrayList<>();
        if (updateRecordResult != null && updateRecordResult.getMessages() != null) {
            final List<MessageEntry> messages = updateRecordResult.getMessages().getMessageEntry();
            if (messages != null && !messages.isEmpty()) {
                final MarcRecord marcRecord = toMarcRecord(addiRecord);

                for (MessageEntry message : messages) {
                    final Diagnostic diagnostic = toDiagnostic(message, marcRecord);
                    if (NON_FATAL_ERROR_MESSAGE.equals(diagnostic.getMessage())) {
                        LOGGER.debug("Update service result contains non-fatal '{}' message", diagnostic.getMessage());
                        // Empty diagnostics will cause ChunkItemProcessor to
                        // return ChunkItem with status SUCCESS
                        diagnostics.clear();
                        break;
                    }
                    if (!isIgnorable(diagnostic.getMessage())) {
                        diagnostics.add(diagnostic);
                    }
                }
            }
        }
        return diagnostics;
    }

    /**
     * Gets a MarcRecord from the given Addi record
     *
     * @param addiRecord Addi record containing MARC record
     * @return MarcRecord object or null of no MARC record could be extracted
     */
    private MarcRecord toMarcRecord(AddiRecord addiRecord) {
        try {
            final BufferedInputStream inputStream = new BufferedInputStream(
                    new ByteArrayInputStream(addiRecord.getContentData()));
            return new MarcXchangeV1Reader(inputStream, StandardCharsets.UTF_8).read();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts a message entry into its corresponding diagnostic
     *
     * @param message    message entry
     * @param marcRecord MARC record used to access the field and subfield
     * @return Diagnostic object
     */
    private Diagnostic toDiagnostic(MessageEntry message, MarcRecord marcRecord) {
        String datafieldTag = null;
        String subfieldCode = null;
        try {
            if (marcRecord != null) {
                final Optional<DataField> datafield = getDatafield(message, marcRecord);
                if (datafield.isPresent()) {
                    datafieldTag = datafield.get().getTag();
                    final Optional<SubField> subfield = getSubfield(message, datafield.get());
                    if (subfield.isPresent()) {
                        subfieldCode = String.valueOf(subfield.get().getCode());
                    }
                }
            }
        } catch (RuntimeException e) {
            final String errorMsg = "Exception caught during extraction of datafield tag and/or subfield code";
            LOGGER.error(errorMsg, e);
            return new Diagnostic(Diagnostic.Level.FATAL, errorMsg, e);
        }
        String messageText = message.getMessage();
        if (messageText == null || messageText.isEmpty()) {
            messageText = "No message from update service";
        }
        return new Diagnostic(getDiagnosticLevel(message), messageText, NO_STACK_TRACE,
                datafieldTag != null ? FIELD_PREPEND + datafieldTag : null,
                subfieldCode != null ? SUBFIELD_PREPEND + subfieldCode : null);
    }

    /**
     * Extracts datafield if identified by given message entry from given MARC record
     *
     * @param message    message entry
     * @param marcRecord MARC record containing identified datafield
     * @return DataField or empty
     */
    private Optional<DataField> getDatafield(MessageEntry message, MarcRecord marcRecord) {
        final Integer fieldno = message.getOrdinalPositionOfField();
        if (fieldno != null) {
            try {
                return Optional.of((DataField) marcRecord.getFields().get(fieldno));
            } catch (RuntimeException e) {
                LOGGER.error("Caught exception while extracting datafield no %d from MARC record", fieldno, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts subfield if identified by given message entry from given datafield
     *
     * @param message   message entry
     * @param datafield datafield containing identified subfield
     * @return SubField or empty
     */
    private Optional<SubField> getSubfield(MessageEntry message, DataField datafield) {
        final Integer subfieldno = message.getOrdinalPositionOfSubfield();
        if (subfieldno != null) {
            try {
                return Optional.of(datafield.getSubfields().get(subfieldno));
            } catch (RuntimeException e) {
                LOGGER.error("Caught exception while extracting subfield no %d from datafield %s",
                        subfieldno, datafield, e);
            }
        }
        return Optional.empty();
    }

    /**
     * Converts message entry type to its corresponding diagnostic level
     *
     * @param message message entry
     * @return Diagnostic.Level
     */
    private Diagnostic.Level getDiagnosticLevel(MessageEntry message) {
        switch (message.getType()) {
            case WARNING:
                return Diagnostic.Level.WARNING;
            case ERROR:
                return Diagnostic.Level.ERROR;
            default:
                return Diagnostic.Level.FATAL;
        }
    }

    private boolean isIgnorable(String validationError) {
        if (ignoredValidationErrors == null || validationError == null) {
            return false;
        }
        return ignoredValidationErrors.stream().anyMatch(validationError::contains);
    }
}
