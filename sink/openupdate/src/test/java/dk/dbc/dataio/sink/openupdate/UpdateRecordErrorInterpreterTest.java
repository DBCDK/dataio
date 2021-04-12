/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.oss.ns.catalogingupdate.MessageEntry;
import dk.dbc.oss.ns.catalogingupdate.Messages;
import dk.dbc.oss.ns.catalogingupdate.Type;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void newInstance() {
        interpreter = new UpdateRecordErrorInterpreter();
    }

    @Test
    public void getDiagnostics_producesDiagnosticWithTag() {
        final MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setOrdinalPositionOfField(1);
        messageEntry.setMessage("message text");

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        // Verification
        assertThat("number of diagnostics", diagnostics.size(), is(1));
        assertThat("diagnostic", diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, messageEntry.getMessage(), null,
                "felt 245", null)));
    }

    @Test
    public void getDiagnostics_producesDiagnosticWithoutTagOrAttribute() {
        final MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setMessage("message text");

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        // Verification
        assertThat("number of diagnostics", diagnostics.size(), is(1));
        assertThat("diagnostic", diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, messageEntry.getMessage(), null,
                null, null)));
    }


    @Test
    public void getDiagnostics_producesDiagnosticWithTagAndAttribute() {
        final MessageEntry messageEntry = new MessageEntry();
        messageEntry.setType(Type.ERROR);
        messageEntry.setOrdinalPositionOfField(1);
        messageEntry.setOrdinalPositionOfSubfield(1);
        messageEntry.setMessage("message text");

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        // Verification
        assertThat("number of diagnostics", diagnostics.size(), is(1));
        assertThat("diagnostic", diagnostics.get(0), is(new Diagnostic(
                Diagnostic.Level.ERROR, messageEntry.getMessage(), null,
                "felt 245", "delfelt c")));
    }

    @Test
    public void getDiagnostics_producesMultipleDiagnostics_ok() {
        final MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("message1 text");

        final MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.FATAL);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setOrdinalPositionOfSubfield(1);
        messageEntry2.setMessage("message2 text");

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

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
        final MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("message1 text");

        final MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.ERROR);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setOrdinalPositionOfSubfield(1);
        messageEntry2.setMessage(UpdateRecordErrorInterpreter.NON_FATAL_ERROR_MESSAGE);

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);

        assertThat("Number of diagnostics", diagnostics.size(), is(0));
    }

    @Test
    public void getDiagnostics_allMessagesAreIgnorable() {
        final MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("ignorable 1");

        final MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.ERROR);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setMessage("ignorable 2");

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        final HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("ignorable 1");
        ignoredValidationErrors.add("ignorable 2");
        final UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter(ignoredValidationErrors);

        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);
        assertThat("Number of diagnostics", diagnostics.size(), is(0));
    }

    @Test
    public void getDiagnostics_onlySubsetOfMessagesAreIgnorable() {
        final MessageEntry messageEntry1 = new MessageEntry();
        messageEntry1.setType(Type.ERROR);
        messageEntry1.setOrdinalPositionOfField(0);
        messageEntry1.setMessage("ignorable 1");

        final MessageEntry messageEntry2 = new MessageEntry();
        messageEntry2.setType(Type.ERROR);
        messageEntry2.setOrdinalPositionOfField(1);
        messageEntry2.setMessage("non-ignorable 2");

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.FAILED);
        updateRecordResult.setMessages(new Messages());
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry1);
        updateRecordResult.getMessages().getMessageEntry().add(messageEntry2);

        final HashSet<String> ignoredValidationErrors = new HashSet<>();
        ignoredValidationErrors.add("ignorable 1");
        final UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter(ignoredValidationErrors);

        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, addiRecord);
        assertThat("Number of diagnostics", diagnostics.size(), is(1));
    }
}