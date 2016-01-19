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

import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.Field;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.UpdateStatusEnum;
import dk.dbc.oss.ns.catalogingupdate.ValidateEntry;
import dk.dbc.oss.ns.catalogingupdate.ValidateInstance;
import dk.dbc.oss.ns.catalogingupdate.ValidateWarningOrErrorEnum;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class UpdateRecordErrorInterpreterTest {

    final byte[] emptyMarcExchangeRecord = ("").getBytes();
    final byte[] rubbishMarcExchangeRecord = ("rubbish input").getBytes();
    final byte[] simpleValidMarcExchangeRecord =
            ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                    "        <marcx:leader>00000cape 22000003 4500</marcx:leader>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
                    "            <marcx:subfield code=\"a\">x7845232</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">19900326</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"o\">d</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "    </marcx:record>").getBytes(StandardCharsets.UTF_8);


    @Test(expected = NullPointerException.class)
    public void getDataField_nullFieldIndex_MarcReaderException() throws MarcReaderException {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getDataField(null, emptyMarcExchangeRecord);
    }

    @Test(expected = NullPointerException.class)
    public void getDataField_nullMarcExchangeRecord_NullPointerException() throws MarcReaderException {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getDataField(createValidateEntry(33, null), null);
    }

    @Test(expected = MarcReaderException.class)
    public void getDataField_emptyMarcExchangeRecord_MarcReaderException() throws MarcReaderException {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getDataField(createValidateEntry(33, null), emptyMarcExchangeRecord);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void getDataField_simpleOneElementMarcExchangeRecordFetchBeforeFirstRecord_notOk() throws MarcReaderException {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getDataField(createValidateEntry(0, null), simpleValidMarcExchangeRecord);
    }

    @Test
    public void getDataField_simpleOneElementMarcExchangeRecordFetchFirstRecord_ok() throws MarcReaderException {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        Optional<DataField> result = interpreter.getDataField(createValidateEntry(1, null), simpleValidMarcExchangeRecord);

        // Verify Test
        assertThat(result, is(not(nullValue())));
        assertThat(result.isPresent(), is(true));
        DataField dataField = result.get();
        assertThat(dataField.getTag(), is("001"));
        assertThat(dataField.getInd1(), is('0'));
        assertThat(dataField.getInd2(), is('0'));
        assertThat(dataField.getInd3(), is(nullValue()));
        List<SubField> subFields = dataField.getSubfields();
        assertThat(subFields.size(), is(4));
        assertThat(subFields.get(0).getCode(), is('a'));
        assertThat(subFields.get(0).getData(), is("x7845232"));
        assertThat(subFields.get(1).getCode(), is('d'));
        assertThat(subFields.get(1).getData(), is("19900326"));
        assertThat(subFields.get(2).getCode(), is('f'));
        assertThat(subFields.get(2).getData(), is("a"));
        assertThat(subFields.get(3).getCode(), is('o'));
        assertThat(subFields.get(3).getData(), is("d"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getDataField_simpleOneElementMarcExchangeRecordFetchSecondRecord_notOk() throws MarcReaderException {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getDataField(createValidateEntry(2, null), simpleValidMarcExchangeRecord);
    }

    @Test
    public void getDataField_noOrdinalPositionOfField_returnsOptionalEmpty() throws MarcReaderException {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        Optional<DataField> result = interpreter.getDataField(new ValidateEntry(), simpleValidMarcExchangeRecord);

        // Verify test
        assertThat(result, is(Optional.empty()));
    }

    @Test(expected = NullPointerException.class)
    public void getLevel_nullInput_nullPointerException() {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getLevel(null);
    }

    @Test
    public void getLevel_errorInput_fatalOutput() {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        Diagnostic.Level result = interpreter.getLevel(ValidateWarningOrErrorEnum.ERROR);

        // Verify test
        assertThat(result, is(Diagnostic.Level.FATAL));
    }

    @Test
    public void getLevel_warningInput_warningOutput() {
        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        Diagnostic.Level result = interpreter.getLevel(ValidateWarningOrErrorEnum.WARNING);

        // Verify test
        assertThat(result, is(Diagnostic.Level.WARNING));
    }

    @Test
    public void getAttribute_noOrdinalPositionOfField_returnsNull() {
        // Prepare test
        DataField dataField = createSimpleDataField();

        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        String attribute = interpreter.getAttribute(createValidateEntry(null, null), dataField);

        // Verify test
        assertThat(attribute, is(nullValue()));
    }

    @Test
    public void getAttribute_validInput_validOutput() {
        // Prepare test
        DataField dataField = createSimpleDataField();

        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        String attribute = interpreter.getAttribute(createValidateEntry(null, 1), dataField);

        // Verify test
        assertThat(attribute, is("s"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getAttribute_warningInput_warningOutput() {
        // Prepare test
        DataField dataField = createSimpleDataField();

        // Subject under test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getAttribute(createValidateEntry(null, 2), dataField);
    }

    @Test(expected = NullPointerException.class)
    public void getMarcRecord_nullInput_exception() throws MarcReaderException {
        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getMarcRecord(null);
    }

    @Test(expected = MarcReaderException.class)
    public void getMarcRecord_emptyInput_exception() throws MarcReaderException {
        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getMarcRecord(emptyMarcExchangeRecord);
    }

    @Test(expected = MarcReaderException.class)
    public void getMarcRecord_rubbishInput_exception() throws MarcReaderException {
        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        interpreter.getMarcRecord(rubbishMarcExchangeRecord);
    }

    @Test
    public void getMarcRecord_simpleValidMarcExchangeRecord_validOutput() throws MarcReaderException {
        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        MarcRecord marcRecord = interpreter.getMarcRecord(simpleValidMarcExchangeRecord);

        // Test Verification
        assertThat(marcRecord.getLeader().getData(), is("00000cape 22000003 4500"));
        List<Field> fields = marcRecord.getFields();
        assertThat(fields.size(), is(1));
        assertThat(fields.get(0).getTag(), is("001"));
    }

    final byte[] marcExchangeRecord1 =
            ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                    "        <marcx:leader>00000cape 22000003 4500</marcx:leader>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
                    "            <marcx:subfield code=\"a\">x7845232</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">19900326</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"o\">d</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"004\">\n" +
                    "            <marcx:subfield code=\"r\">c</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">e</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"008\">\n" +
                    "            <marcx:subfield code=\"t\">p</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"b\">dk</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"v\">3</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"l\">dan</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"&amp;\">3</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"009\">\n" +
                    "            <marcx:subfield code=\"a\">a</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">xx</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"022\">\n" +
                    "            <marcx:subfield code=\"a\">0903-0077</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"088\">\n" +
                    "            <marcx:subfield code=\"a\">352(489)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"096\">\n" +
                    "            <marcx:subfield code=\"b\">s Kom</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">p103277027</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"u\">Udlånes ikke</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
                    "            <marcx:subfield code=\"a\">Kommunen</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"ø\">ny sondring</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"ø\">Ulovligt dubleret(BDM)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"260\">\n" +
                    "            <marcx:subfield code=\"a\">København</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">20070501</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">ret911210</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">kat910222</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">sam</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"f\">sg</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">NB</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"980\">\n" +
                    "            <marcx:subfield code=\"b\">15-</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">1972-</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">1</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"980\">\n" +
                    "            <marcx:subfield code=\"m\">Mangler: nr.1-5,7,17(1972);nr.10(1973);nr.12(1987); nr.15,16(1988); nr. 9(2014); nr.8+16-17+19-21(2015)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "    </marcx:record>").getBytes(StandardCharsets.UTF_8);


    @Test
    public void getMarcRecord_marcExchangeRecord1_ok() throws MarcReaderException {
        // Prepare test
        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        ValidateInstance validateInstance = new ValidateInstance();
        List<ValidateEntry> validateInstances = validateInstance.getValidateEntry();
        ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry.setOrdinalPositionOfField(BigInteger.valueOf(8));
        validateEntry.setMessage("Delfelt 'ø' er gentaget '2' gange i feltet");
        validateInstances.add(validateEntry);
        updateRecordResult.setValidateInstance(validateInstance);

        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, marcExchangeRecord1);

        // Test Verification
        assertThat(diagnostics.size(), is(1));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is("Delfelt 'ø' er gentaget '2' gange i feltet"));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("245"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
    }

    final byte[] marcExchangeRecord2 =
            ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                    "        <marcx:leader>00000cape 22000003 4500</marcx:leader>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
                    "            <marcx:subfield code=\"a\">0348484x</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">19880615</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"o\">d</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"004\">\n" +
                    "            <marcx:subfield code=\"r\">c</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">e</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"008\">\n" +
                    "            <marcx:subfield code=\"t\">p</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"v\">3</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"l\">swe</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"&amp;\">3</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"009\">\n" +
                    "            <marcx:subfield code=\"a\">a</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">xx</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"022\">\n" +
                    "            <marcx:subfield code=\"a\">0348-484x</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"088\">\n" +
                    "            <marcx:subfield code=\"a\">800</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">(485)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"096\">\n" +
                    "            <marcx:subfield code=\"b\">h Pra</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">p103840775</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
                    "            <marcx:subfield code=\"a\">Praktisk lingvistik/ Institutionen för lingvistik, Lunds universitet. - Lund</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"260\">\n" +
                    "            <marcx:subfield code=\"a\">Sted</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"c\">år2015</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"B\">åbenrå</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"b\">Aabenraa</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"K\">mangler lille distribution(BDM)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"565\">\n" +
                    "            <marcx:subfield code=\"a\">nogle nr. kan søges som bøger</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">20070501</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">kat</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">ret911114</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">hum</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">afsl</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">HN</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">ops000906</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"710\">\n" +
                    "            <marcx:subfield code=\"a\">Lunds universitet. Institutionen för lingvistik</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"710\">\n" +
                    "            <marcx:subfield code=\"a\">Institutionen för lingvistik</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"980\">\n" +
                    "            <marcx:subfield code=\"b\">3-</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"c\">16</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">1979-</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"e\">1998</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"s\">opsagt</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "    </marcx:record>").getBytes();

    @Test
    public void getMarcRecord_marcExchangeRecord2_ok() throws MarcReaderException {
        // Prepare test
        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        ValidateInstance validateInstance = new ValidateInstance();
        List<ValidateEntry> validateInstances = validateInstance.getValidateEntry();
        ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry.setMessage("Systemfejl ved validering: ReferenceError: \"typename\" is not defined");
        validateInstances.add(validateEntry);
        updateRecordResult.setValidateInstance(validateInstance);

        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, marcExchangeRecord2);

        // Test Verification
        assertThat(diagnostics.size(), is(1));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is("Systemfejl ved validering: ReferenceError: \"typename\" is not defined"));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is(nullValue()));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
    }

    final byte[] marcExchangeRecord3 =
            ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                    "        <marcx:leader>00000dmpe 22000001 4500</marcx:leader>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
                    "            <marcx:subfield code=\"a\">x5104995</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"o\">d</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"004\">\n" +
                    "            <marcx:subfield code=\"r\">d</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">e</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"008\">\n" +
                    "            <marcx:subfield code=\"t\">p</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"v\">1</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"l\">dan</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"&amp;\">9</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"009\">\n" +
                    "            <marcx:subfield code=\"a\">m</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">nh</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"088\">\n" +
                    "            <marcx:subfield code=\"a\">361(489)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"096\">\n" +
                    "            <marcx:subfield code=\"b\">s Dan</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">p105104995</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"u\">Udlånes ikke</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
                    "            <marcx:subfield code=\"a\">Dansk råstof</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"c\">socialt projektmagasin</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"m\">video</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"260\">\n" +
                    "            <marcx:subfield code=\"a\">København</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">sle151204</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">peri</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">kasseret</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">20070501</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">kat920304</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">av</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">PS</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">afsl</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"980\">\n" +
                    "            <marcx:subfield code=\"b\">1-</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"c\">3</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">1991-</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"e\">1992</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "    </marcx:record>").getBytes();


    @Test
    public void getMarcRecord_marcExchangeRecord3_ok() throws MarcReaderException {
        // Prepare test
        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        ValidateInstance validateInstance = new ValidateInstance();
        List<ValidateEntry> validateInstances = validateInstance.getValidateEntry();
        ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry.setMessage("Værdien 'p' er ikke end del af de valide værdier: 'm', 's'");
        validateEntry.setOrdinalPositionOfField(BigInteger.valueOf(3));
        validateEntry.setOrdinalPositionOfSubField(BigInteger.valueOf(1));
        validateInstances.add(validateEntry);
        updateRecordResult.setValidateInstance(validateInstance);

        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, marcExchangeRecord3);

        // Test Verification
        assertThat(diagnostics.size(), is(1));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is("Værdien 'p' er ikke end del af de valide værdier: 'm', 's'"));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("008"));
        assertThat(diagnostic.getAttribute(), is("t"));
    }

    final byte[] marcExchangeRecord4 =
            ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">\n" +
                    "        <marcx:leader>00000came 22000000 4500</marcx:leader>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
                    "            <marcx:subfield code=\"a\">9781843922735</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"b\">820040</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">20151120</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"004\">\n" +
                    "            <marcx:subfield code=\"r\">c</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">e</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"008\">\n" +
                    "            <marcx:subfield code=\"t\">m</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">2013</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"b\">gb</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"d\">y</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"l\">eng</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"v\">0</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"&amp;\">2</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"009\">\n" +
                    "            <marcx:subfield code=\"a\">a</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"g\">xx</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"021\">\n" +
                    "            <marcx:subfield code=\"e\">9781843922735</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"e\">9780415625951</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"082\">\n" +
                    "            <marcx:subfield code=\"a\">364.68</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"088\">\n" +
                    "            <marcx:subfield code=\"a\">343.8</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">301.173</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"096\">\n" +
                    "            <marcx:subfield code=\"b\">343.8Ele</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"c\">b</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">\n" +
                    "            <marcx:subfield code=\"a\">Electronically monitored punishment</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"c\">international and critical perspectives</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"e\">ed. by Mike Nellis, Kristel Beynes and Dan Kaminski</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"260\">\n" +
                    "            <marcx:subfield code=\"a\">Routlegde</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"b\">London</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"c\">2013</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"300\">\n" +
                    "            <marcx:subfield code=\"a\">xi, 279 s.</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"504\">\n" +
                    "            <marcx:subfield code=\"a\">Summary: Electronic monitoring (EM) is a way of supervising offenders in the community whilst they are on bail, serving a community sentence or after release from prison. Various technologies can be used, including voice verification, GPS satellite tracking and ' most commonly - the use of radio frequency to monitor house arrest. It originated in the USA in the 1980s and has spread to over 30 countries since then. This book explores the development of EM in a number of countries to give some indication of the diverse ways it has been utilized and of..</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"530\">\n" +
                    "            <marcx:subfield code=\"a\">Introduction: making sense of electronic monitoring, Mike Nellis, Kristel Beyens and Dan Kaminski, Part I, National experiences; 1. The limits of techno-utopianism: electronic monitoring in the United States of America, J. Robert Lilly and Mike Nellis, 2. The evolution of electronic monitoring in Canada: from corrections to sentencing and beyond, Suzanne Wallace-Capretta and Julian Roberts, 3. 'Parallel tracks': probation and electronic monitoring in England and Wales and Scotland, George Mair and Mike Nellis, 4. Extending the electronic net in Australia and New Zealand: developments in electronic monitoring down-under, Russell G. Smith and Anita Gibbs, 5. From voice verification to GPS tracking: the development of electronic monitoring in South Korea, Younoh Cho and Byung Bae Kim, 6. High level support and high level control: an efficient Swedish model of electronic monitoring? Inka Wennerberg, 7. From tagging to tracking: beginnings and devleopment of electronic monitoring in</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"a\">kat151203</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"590\">\n" +
                    "            <marcx:subfield code=\"g\">MAI</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"f\">BSD</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"592\">\n" +
                    "            <marcx:subfield code=\"d\">0001</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"594\">\n" +
                    "            <marcx:subfield code=\"a\">15111811002735</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"630\">\n" +
                    "            <marcx:subfield code=\"a\">socialisering</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">frihedsstraf</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"a\">kriminalpolitik</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"700\">\n" +
                    "            <marcx:subfield code=\"a\">Nellis</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"H\">Mike</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"4\">edt</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"h\">(BDM)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"700\">\n" +
                    "            <marcx:subfield code=\"a\">Beynes</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"h\">Kristel</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"4\">edt</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "        <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"700\">\n" +
                    "            <marcx:subfield code=\"a\">Kaminski</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"H\">Dan</marcx:subfield>\n" +
                    "            <marcx:subfield code=\"4\">edt(BDM)</marcx:subfield>\n" +
                    "        </marcx:datafield>\n" +
                    "    </marcx:record>\n").getBytes();

    @Test
    public void getMarcRecord_marcExchangeRecord4_ok() throws MarcReaderException {
        // Prepare test
        UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        ValidateInstance validateInstance = new ValidateInstance();
        List<ValidateEntry> validateInstances = validateInstance.getValidateEntry();
        // Error message #1
        ValidateEntry validateEntry1 = new ValidateEntry();
        validateEntry1.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry1.setMessage("Delfelt 'e' er gentaget '2' gange i feltet");
        validateEntry1.setOrdinalPositionOfField(BigInteger.valueOf(5));
        validateInstances.add(validateEntry1);
        // Error message #2
        ValidateEntry validateEntry2 = new ValidateEntry();
        validateEntry2.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry2.setMessage("Delfeltet: 'H'\" skal efterfølges af et 'h'\" i felt '700'");
        validateEntry2.setOrdinalPositionOfField(BigInteger.valueOf(19));
        validateInstances.add(validateEntry2);
        // Error message #3
        ValidateEntry validateEntry3 = new ValidateEntry();
        validateEntry3.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry3.setMessage("Værdien 'edt(BDM)' er ikke end del af de valide værdier: 'act', 'aft', 'anm', 'ant', 'arc', 'arr', 'art', 'aud', 'aui', 'aus', 'aut', 'ccp', 'chr', 'cli', 'cll', 'cmm', 'cmp', 'cnd', 'cng', 'com', 'cre', 'ctb', 'ctg', 'cur', 'cwt', 'dnc', 'drm', 'drt', 'dte', 'edt', 'ill', 'inv', 'itr', 'ive', 'ivr', 'lbt', 'ltg', 'lyr', 'mus', 'nrt', 'orm', 'oth', 'pht', 'ppt', 'prd', 'prf', 'prg', 'pro', 'rce', 'res', 'rev', 'sad', 'scl', 'sng', 'stl', 'ths', 'trl', 'wdc', 'dkani', 'dkbea', 'dkbra', 'dkbrm', 'dkdes', 'dkfig', 'dkfvl', 'dkind', 'dkins', 'dkmdd', 'dkmdt', 'dkmed', 'dkmon', 'dkops', 'dkopt', 'dkref', 'dkste', 'dktek', 'dktil', 'dkved'");
        validateEntry3.setOrdinalPositionOfField(BigInteger.valueOf(21));
        validateEntry3.setOrdinalPositionOfSubField(BigInteger.valueOf(3));
        validateInstances.add(validateEntry3);
        // Error message #4
        ValidateEntry validateEntry4 = new ValidateEntry();
        validateEntry4.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry4.setMessage("Delfeltet: 'H'\" skal efterfølges af et 'h'\" i felt '700'");
        validateEntry4.setOrdinalPositionOfField(BigInteger.valueOf(21));
        validateInstances.add(validateEntry4);
        updateRecordResult.setValidateInstance(validateInstance);

        // Test
        UpdateRecordErrorInterpreter interpreter = new UpdateRecordErrorInterpreter();
        List<Diagnostic> diagnostics = interpreter.getDiagnostics(updateRecordResult, marcExchangeRecord4);

        // Test Verification
        assertThat(diagnostics.size(), is(4));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is("Delfelt 'e' er gentaget '2' gange i feltet"));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("021"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
        diagnostic = diagnostics.get(1);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is("Delfeltet: 'H'\" skal efterfølges af et 'h'\" i felt '700'"));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("700"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
        diagnostic = diagnostics.get(2);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is("Værdien 'edt(BDM)' er ikke end del af de valide værdier: 'act', 'aft', 'anm', 'ant', 'arc', 'arr', 'art', 'aud', 'aui', 'aus', 'aut', 'ccp', 'chr', 'cli', 'cll', 'cmm', 'cmp', 'cnd', 'cng', 'com', 'cre', 'ctb', 'ctg', 'cur', 'cwt', 'dnc', 'drm', 'drt', 'dte', 'edt', 'ill', 'inv', 'itr', 'ive', 'ivr', 'lbt', 'ltg', 'lyr', 'mus', 'nrt', 'orm', 'oth', 'pht', 'ppt', 'prd', 'prf', 'prg', 'pro', 'rce', 'res', 'rev', 'sad', 'scl', 'sng', 'stl', 'ths', 'trl', 'wdc', 'dkani', 'dkbea', 'dkbra', 'dkbrm', 'dkdes', 'dkfig', 'dkfvl', 'dkind', 'dkins', 'dkmdd', 'dkmdt', 'dkmed', 'dkmon', 'dkops', 'dkopt', 'dkref', 'dkste', 'dktek', 'dktil', 'dkved'"));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("700"));
        assertThat(diagnostic.getAttribute(), is("4"));
        diagnostic = diagnostics.get(3);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is("Delfeltet: 'H'\" skal efterfølges af et 'h'\" i felt '700'"));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("700"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
    }


    // Private methods

    private DataField createSimpleDataField() {
        DataField dataField = new DataField();
        dataField.setTag("tag");
        dataField.setInd1('a');
        dataField.setInd2('b');
        dataField.setInd3('c');
        SubField subField = new SubField();
        subField.setCode('s');
        subField.setData("subdata");
        dataField.addSubfield(subField);
        return dataField;
    }

    private ValidateEntry createValidateEntry(Integer field, Integer subField) {
        ValidateEntry entry = new ValidateEntry();
        if (field != null) {
            entry.setOrdinalPositionOfField(BigInteger.valueOf(field));
        }
        if (subField != null) {
            entry.setOrdinalPositionOfSubField(BigInteger.valueOf(subField));
        }
        return entry;
    }

}