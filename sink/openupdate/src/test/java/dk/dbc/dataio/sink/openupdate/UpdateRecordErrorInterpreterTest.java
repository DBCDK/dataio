package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Messages;
import dk.dbc.oss.ns.catalogingupdate.Type;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UpdateRecordErrorInterpreterTest extends AbstractOpenUpdateSinkTestBase {
    private final byte[] marcRecord =
            ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                    "        <marcx:leader>00000dmpe 22000001 4500</marcx:leader>" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">" +
                    "            <marcx:subfield code=\"a\">x5104995</marcx:subfield>" +
                    "            <marcx:subfield code=\"f\">a</marcx:subfield>" +
                    "            <marcx:subfield code=\"o\">d</marcx:subfield>" +
                    "        </marcx:datafield>" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                    "            <marcx:subfield code=\"a\">Dansk råstof</marcx:subfield>" +
                    "            <marcx:subfield code=\"c\">socialt projektmagasin</marcx:subfield>" +
                    "            <marcx:subfield code=\"m\">video</marcx:subfield>" +
                    "        </marcx:datafield>" +
                    "    </marcx:record>").getBytes(StandardCharsets.UTF_8);

    private final AddiRecord addiRecord = new AddiRecord("meta".getBytes(StandardCharsets.UTF_8), marcRecord);

    private UpdateRecordErrorInterpreter interpreter;

    @BeforeEach
    public void newInstance() {
        interpreter = new UpdateRecordErrorInterpreter();
    }

    @Test
    public void getDiagnostics_producesDiagnosticWithTag() {
        MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setOrdinalPositionOfField(1);
        messageEntry.setMessage("message text");

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry);

        // Subject under test
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        // Verification
        assertThat("number of diagnostics", diagnostics.size(), is(1));
        assertThat("diagnostic", diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, messageEntry.getMessage(), null,
                "felt 245", null)));
    }

    @Test
    public void getDiagnostics_producesDiagnosticWithoutTagOrAttribute() {
        MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setMessage("message text");

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry);

        // Subject under test
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        // Verification
        assertThat("number of diagnostics", diagnostics.size(), is(1));
        assertThat("diagnostic", diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, messageEntry.getMessage(), null,
                null, null)));
    }


    @Test
    public void getDiagnostics_producesDiagnosticWithTagAndAttribute() {
        MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setOrdinalPositionOfField(1);
        messageEntry.setOrdinalPositionOfSubfield(1);
        messageEntry.setMessage("message text");

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry);

        // Subject under test
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        // Verification
        assertThat("number of diagnostics", diagnostics.size(), is(1));
        assertThat("diagnostic", diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, messageEntry.getMessage(), null,
                "felt 245", "delfelt c")));
    }

    @Test
    public void getDiagnostics_producesMultipleDiagnostics_ok() {
        MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("message1 text");

        MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.FATAL);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setOrdinalPositionOfSubfield(1);
        messageEntry2.setMessage("message2 text");

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        // Subject under test
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        // Verification
        assertThat("number of diagnostics", diagnostics.size(), is(2));
        assertThat("1st diagnostic", diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, messageEntry1.getMessage(), null,
                "felt 001", null)));
        assertThat("2nd diagnostic", diagnostics.get(1), is(new Diagnostic(
                Diagnostic.Level.FATAL, messageEntry2.getMessage(), null,
                "felt 245", "delfelt c")));
    }

    @Test
    public void getDiagnostics_messageEntryContainsNonFatalMessage_returnsEmptyList() {
        MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("message1 text");

        MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.ERROR);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setOrdinalPositionOfSubfield(1);
        messageEntry2.setMessage(UpdateRecordErrorInterpreter.NON_FATAL_ERROR_MESSAGE);

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        // Subject under test
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        assertThat("Number of diagnostics", diagnostics.size(), is(0));
    }

    @Test
    public void getDiagnostics_allMessagesAreIgnorable() {
        MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("ignorable 1");

        MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.ERROR);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setMessage("ignorable 2");

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("ignorable 1");
        ignoredValidationErrors.add("ignorable 2");
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter(ignoredValidationErrors);

        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);
        assertThat("Number of diagnostics", diagnostics.size(), is(0));
    }

    @Test
    public void getDiagnostics_onlySubsetOfMessagesAreIgnorable() {
        MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("ignorable 1");

        MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.ERROR);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setMessage("non-ignorable 2");

        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("ignorable 1");
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter(ignoredValidationErrors);

        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);
        assertThat("Number of diagnostics", diagnostics.size(), is(1));
    }
}
