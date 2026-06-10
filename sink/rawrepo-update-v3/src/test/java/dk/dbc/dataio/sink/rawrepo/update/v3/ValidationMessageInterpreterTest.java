package dk.dbc.dataio.sink.rawrepo.update.v3;

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateResponse;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.UpdateResponseStatus;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.ValidationMessage;
import dk.dbc.dataio.sink.rawrepo.update.v3.connector.ValidationStatus;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcBinding;
import dk.dbc.marc.binding.SubField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ValidationMessageInterpreterTest {

    private final ValidationMessageInterpreter interpreter = new ValidationMessageInterpreter(null);

    @Test
    void getDiagnostics_responseIsNull_returnsEmpty() {
        assertThat(interpreter.getDiagnostics(null, null).isEmpty(), is(true));
    }

    @Test
    void getDiagnostics_statusOk_returnsEmpty() {
        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.OK);
        assertThat(interpreter.getDiagnostics(response, null).isEmpty(), is(true));
    }

    @Test
    void getDiagnostics_errorsListEmpty_returnsEmpty() {
        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        response.setErrors(List.of());
        assertThat(interpreter.getDiagnostics(response, null).isEmpty(), is(true));
    }

    @Test
    void getDiagnostics_nonFatalDeleteMessage_returnsEmpty() {
        ValidationMessage message = new ValidationMessage();
        message.setType(ValidationStatus.ERROR);
        message.setMessage(ValidationMessageInterpreter.DELETE_NONEXISTENT_RECORD_MESSAGE);

        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        response.setErrors(List.of(message));

        assertThat(interpreter.getDiagnostics(response, null).isEmpty(), is(true));
    }

    @Test
    void getDiagnostics_ignoredMessage_returnsEmpty() {
        ValidationMessageInterpreter interpreterWithIgnored =
                new ValidationMessageInterpreter(Set.of("ignored phrase"));

        ValidationMessage message = new ValidationMessage();
        message.setType(ValidationStatus.ERROR);
        message.setMessage("this contains ignored phrase here");

        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        response.setErrors(List.of(message));

        assertThat(interpreterWithIgnored.getDiagnostics(response, null).isEmpty(), is(true));
    }

    @Test
    void getDiagnostics_errorType_returnsDiagnosticWithErrorLevel() {
        ValidationMessage message = new ValidationMessage();
        message.setType(ValidationStatus.ERROR);
        message.setMessage("some error");

        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        response.setErrors(List.of(message));

        List<Diagnostic> diagnostics = interpreter.getDiagnostics(response, null);

        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.ERROR));
        assertThat(diagnostics.get(0).getMessage(), is("some error"));
    }

    @Test
    void getDiagnostics_fieldOrdinalResolvesTagFromMarcBinding() {
        MarcBinding marcBinding = new MarcBinding()
                .addField(new DataField("001", "00").addSubField(new SubField('a', "12345")))
                .addField(new DataField("245", "00")
                        .addSubField(new SubField('a', "Titel"))
                        .addSubField(new SubField('b', "undertitel")));

        ValidationMessage message = new ValidationMessage();
        message.setType(ValidationStatus.ERROR);
        message.setOrdinalPositionOfField(1);
        message.setMessage("some error");

        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        response.setErrors(List.of(message));

        List<Diagnostic> diagnostics = interpreter.getDiagnostics(response, marcBinding);

        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, "some error", null, "felt 245", null)));
    }

    @Test
    void getDiagnostics_subfieldOrdinalResolvesCodeFromMarcBinding() {
        MarcBinding marcBinding = new MarcBinding()
                .addField(new DataField("001", "00").addSubField(new SubField('a', "12345")))
                .addField(new DataField("245", "00")
                        .addSubField(new SubField('a', "Titel"))
                        .addSubField(new SubField('b', "undertitel")));

        ValidationMessage message = new ValidationMessage();
        message.setType(ValidationStatus.ERROR);
        message.setOrdinalPositionOfField(1);
        message.setOrdinalPositionOfSubfield(1);
        message.setMessage("some error");

        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        response.setErrors(List.of(message));

        List<Diagnostic> diagnostics = interpreter.getDiagnostics(response, marcBinding);

        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, "some error", null, "felt 245", "delfelt b")));
    }

    @Test
    void getDiagnostics_warningType_returnsDiagnosticWithWarningLevel() {
        ValidationMessage message = new ValidationMessage();
        message.setType(ValidationStatus.WARNING);
        message.setMessage("some warning");

        UpdateResponse response = new UpdateResponse();
        response.setStatus(UpdateResponseStatus.ERROR);
        response.setErrors(List.of(message));

        List<Diagnostic> diagnostics = interpreter.getDiagnostics(response, null);

        assertThat(diagnostics.size(), is(1));
        assertThat(diagnostics.get(0).getLevel(), is(Diagnostic.Level.WARNING));
    }
}
