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
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class UpdateRecordErrorInterpreterTest extends AbstractOpenUpdateSinkTestBase {

    private UpdateRecordErrorInterpreter interpreter;

    private final byte[] emptyMarcExchangeRecord = ("").getBytes();
    private final byte[] rubbishMarcExchangeRecord = ("rubbish input").getBytes();
    private final byte[] simpleValidMarcExchangeRecord = getSimpleMarcExchangeRecord().getBytes(StandardCharsets.UTF_8);

    private static final String RECORD_PRODUCES_DIAGNOSTIC_WITH_TAG = "/marcExchangeRecordProduceDiagnosticWithTag.xml";
    private static final String RECORD_PRODUCES_DIAGNOSTIC_WITHOUT_TAG_OR_ATTRIBUTE = "/marcExchangeRecordProduceDiagnosticWithoutTagOrAttribute.xml";
    private static final String RECORD_PRODUCES_DIAGNOSTIC_WITH_TAG_AND_ATTRIBUTE = "/marcExchangeRecordProduceDiagnosticWithTagAndAttribute.xml";
    private static final String RECORD_PRODUCES_MULTIPLE_DIAGNOSTICS = "/marcExchangeRecordProducesMultipleDiagnostics.xml.xml";

    private static final String SUB_FIELD_H_MESSAGE = "Delfeltet: 'H'\" skal efterfølges af et 'h'\" i felt '700'";
    private static final String EDT_BDM_VALUE = getEdtBdmValue();

    @Before
    public void newInstance() {
        interpreter = new UpdateRecordErrorInterpreter();
    }

    @Test(expected = NullPointerException.class)
    public void getMarcRecord_nullInput_exception() throws MarcReaderException {
        interpreter.getMarcRecord(null);
    }

    @Test(expected = MarcReaderException.class)
    public void getMarcRecord_emptyInput_exception() throws MarcReaderException {
        interpreter.getMarcRecord(emptyMarcExchangeRecord);
    }

    @Test(expected = MarcReaderException.class)
    public void getMarcRecord_rubbishInput_exception() throws MarcReaderException {
        interpreter.getMarcRecord(rubbishMarcExchangeRecord);
    }

    @Test(expected = NullPointerException.class)
    public void getDataField_nullFieldIndex_MarcReaderException() throws MarcReaderException {
        interpreter.getDataField(null, emptyMarcExchangeRecord);
    }

    @Test(expected = NullPointerException.class)
    public void getDataField_nullMarcExchangeRecord_NullPointerException() throws MarcReaderException {
        interpreter.getDataField(createValidateEntry(33, null), null);
    }

    @Test(expected = MarcReaderException.class)
    public void getDataField_emptyMarcExchangeRecord_MarcReaderException() throws MarcReaderException {
        interpreter.getDataField(createValidateEntry(33, null), emptyMarcExchangeRecord);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void getDataField_simpleOneElementMarcExchangeRecordFetchBeforeFirstRecord_notOk() throws MarcReaderException {
        interpreter.getDataField(createValidateEntry(0, null), simpleValidMarcExchangeRecord);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getDataField_simpleOneElementMarcExchangeRecordFetchSecondRecord_notOk() throws MarcReaderException {
        interpreter.getDataField(createValidateEntry(2, null), simpleValidMarcExchangeRecord);
    }

    @Test
    public void getDataField_noOrdinalPositionOfField_returnsOptionalEmpty() throws MarcReaderException {
        // Subject under test
        final Optional<DataField> result = interpreter.getDataField(new ValidateEntry(), simpleValidMarcExchangeRecord);

        // Verification
        assertThat(result, is(Optional.empty()));
    }

    @Test
    public void getDataField_simpleOneElementMarcExchangeRecordFetchFirstRecord_ok() throws MarcReaderException {
        // Subject under test
        final Optional<DataField> result = interpreter.getDataField(createValidateEntry(1, null), simpleValidMarcExchangeRecord);

        // Verification
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

    @Test(expected = NullPointerException.class)
    public void getLevel_nullInput_nullPointerException() {
        interpreter.getLevel(null);
    }

    @Test
    public void getLevel_errorInput_fatalOutput() {
        // Subject under test
        final Diagnostic.Level result = interpreter.getLevel(ValidateWarningOrErrorEnum.ERROR);

        // Verification
        assertThat(result, is(Diagnostic.Level.FATAL));
    }

    @Test
    public void getLevel_warningInput_warningOutput() {
        // Subject under test
        final Diagnostic.Level result = interpreter.getLevel(ValidateWarningOrErrorEnum.WARNING);

        // Verification
        assertThat(result, is(Diagnostic.Level.WARNING));
    }

    @Test
    public void getAttribute_noOrdinalPositionOfField_returnsNull() {
        // Prepare test
        final DataField dataField = createSimpleDataField();

        // Subject under test
        final String attribute = interpreter.getAttribute(createValidateEntry(null, null), dataField);

        // Verification
        assertThat(attribute, is(nullValue()));
    }

    @Test
    public void getAttribute_validInput_validOutput() {
        final DataField dataField = createSimpleDataField();

        // Subject under test
        final String attribute = interpreter.getAttribute(createValidateEntry(null, 1), dataField);

        // Verification
        assertThat(attribute, is("s"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getAttribute_warningInput_warningOutput() {
        final DataField dataField = createSimpleDataField();

        // Subject under test
        interpreter.getAttribute(createValidateEntry(null, 2), dataField);
    }

    @Test
    public void getMarcRecord_simpleValidMarcExchangeRecord_validOutput() throws MarcReaderException {
        // Subject under test
        final MarcRecord marcRecord = interpreter.getMarcRecord(simpleValidMarcExchangeRecord);

        // Verification
        assertThat(marcRecord.getLeader().getData(), is("00000cape 22000003 4500"));
        List<Field> fields = marcRecord.getFields();
        assertThat(fields.size(), is(1));
        assertThat(fields.get(0).getTag(), is("001"));
    }

    @Test
    public void getDiagnostics_produceDiagnosticWithTag_ok() throws MarcReaderException {
        final String subFieldØMessage = "Delfelt 'ø' er gentaget '2' gange i feltet";

        final ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry.setOrdinalPositionOfField(BigInteger.valueOf(8));
        validateEntry.setMessage(subFieldØMessage);

        final ValidateInstance validateInstance = new ValidateInstance();
        validateInstance.getValidateEntry().add(validateEntry);

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        updateRecordResult.setValidateInstance(validateInstance);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(
                updateRecordResult,
                readTestRecord(RECORD_PRODUCES_DIAGNOSTIC_WITH_TAG));

        // Verification
        assertThat(diagnostics.size(), is(1));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is(subFieldØMessage));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("245"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
    }

    @Test
    public void getDiagnostics_produceDiagnosticWithoutTagOrAttribute_ok() throws MarcReaderException {
        final String message = "Systemfejl ved validering: ReferenceError: \"typename\" is not defined";

        final ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry.setMessage(message);

        final ValidateInstance validateInstance = new ValidateInstance();
        validateInstance.getValidateEntry().add(validateEntry);

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        updateRecordResult.setValidateInstance(validateInstance);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(
                updateRecordResult,
                readTestRecord(RECORD_PRODUCES_DIAGNOSTIC_WITHOUT_TAG_OR_ATTRIBUTE));

        // Verification
        assertThat(diagnostics.size(), is(1));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is(message));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is(nullValue()));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
    }

    @Test
    public void getDiagnostics_produceDiagnosticWithTagAndAttribute_ok() throws MarcReaderException {
        final String message = "Værdien 'p' er ikke end del af de valide værdier: 'm', 's'";

        final ValidateEntry validateEntry = new ValidateEntry();
        validateEntry.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry.setMessage(message);
        validateEntry.setOrdinalPositionOfField(BigInteger.valueOf(3));
        validateEntry.setOrdinalPositionOfSubField(BigInteger.valueOf(1));

        final ValidateInstance validateInstance = new ValidateInstance();
        validateInstance.getValidateEntry().add(validateEntry);

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        updateRecordResult.setValidateInstance(validateInstance);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(
                updateRecordResult,
                readTestRecord(RECORD_PRODUCES_DIAGNOSTIC_WITH_TAG_AND_ATTRIBUTE));

        // Verification
        assertThat(diagnostics.size(), is(1));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is(message));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("008"));
        assertThat(diagnostic.getAttribute(), is("t"));
    }

    @Test
    public void getDiagnostics_producesMultipleDiagnostics_ok() throws MarcReaderException {
        final String subFieldEMessage = "Delfelt 'e' er gentaget '2' gange i feltet";

        // Error message #1
        final ValidateEntry validateEntry1 = new ValidateEntry();
        validateEntry1.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry1.setMessage(subFieldEMessage);
        validateEntry1.setOrdinalPositionOfField(BigInteger.valueOf(5));

        // Error message #2
        final ValidateEntry validateEntry2 = new ValidateEntry();
        validateEntry2.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry2.setMessage(SUB_FIELD_H_MESSAGE);
        validateEntry2.setOrdinalPositionOfField(BigInteger.valueOf(19));

        // Error message #3
        final ValidateEntry validateEntry3 = new ValidateEntry();
        validateEntry3.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry3.setMessage(EDT_BDM_VALUE);
        validateEntry3.setOrdinalPositionOfField(BigInteger.valueOf(21));
        validateEntry3.setOrdinalPositionOfSubField(BigInteger.valueOf(3));

        // Error message #4
        final ValidateEntry validateEntry4 = new ValidateEntry();
        validateEntry4.setWarningOrError(ValidateWarningOrErrorEnum.ERROR);
        validateEntry4.setMessage(SUB_FIELD_H_MESSAGE);
        validateEntry4.setOrdinalPositionOfField(BigInteger.valueOf(21));

        final ValidateInstance validateInstance = new ValidateInstance();
        validateInstance.getValidateEntry().add(validateEntry1);
        validateInstance.getValidateEntry().add(validateEntry2);
        validateInstance.getValidateEntry().add(validateEntry3);
        validateInstance.getValidateEntry().add(validateEntry4);

        final UpdateRecordResult updateRecordResult = new UpdateRecordResult();
        updateRecordResult.setUpdateStatus(UpdateStatusEnum.VALIDATION_ERROR);
        updateRecordResult.setValidateInstance(validateInstance);

        // Subject under test
        final List<Diagnostic> diagnostics = interpreter.getDiagnostics(
                updateRecordResult,
                readTestRecord(RECORD_PRODUCES_MULTIPLE_DIAGNOSTICS));

        // Verification
        assertThat(diagnostics.size(), is(4));
        Diagnostic diagnostic = diagnostics.get(0);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is(subFieldEMessage));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("021"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));

        diagnostic = diagnostics.get(1);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is(SUB_FIELD_H_MESSAGE));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("700"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));

        diagnostic = diagnostics.get(2);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is(EDT_BDM_VALUE));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("700"));
        assertThat(diagnostic.getAttribute(), is("4"));

        diagnostic = diagnostics.get(3);
        assertThat(diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat(diagnostic.getMessage(), is(SUB_FIELD_H_MESSAGE));
        assertThat(diagnostic.getStacktrace(), is(nullValue()));
        assertThat(diagnostic.getTag(), is("700"));
        assertThat(diagnostic.getAttribute(), is(nullValue()));
    }


    /*
     * Private methods
     */

    private static String getEdtBdmValue() {
       return "Værdien 'edt(BDM)' er ikke end del af de valide værdier: " +
                "'act', 'aft', 'anm', 'ant', 'arc', 'arr', 'art', 'aud', " +
                "'aui', 'aus', 'aut', 'ccp', 'chr', 'cli', 'cll', 'cmm', " +
                "'cmp', 'cnd', 'cng', 'com', 'cre', 'ctb', 'ctg', 'cur', " +
                "'cwt', 'dnc', 'drm', 'drt', 'dte', 'edt', 'ill', 'inv', " +
                "'itr', 'ive', 'ivr', 'lbt', 'ltg', 'lyr', 'mus', 'nrt', " +
                "'orm', 'oth', 'pht', 'ppt', 'prd', 'prf', 'prg', 'pro', " +
                "'rce', 'res', 'rev', 'sad', 'scl', 'sng', 'stl', 'ths', " +
                "'trl', 'wdc', 'dkani', 'dkbea', 'dkbra', 'dkbrm', 'dkdes', " +
                "'dkfig', 'dkfvl', 'dkind', 'dkins', 'dkmdd', 'dkmdt', " +
                "'dkmed', 'dkmon', 'dkops', 'dkopt', 'dkref', 'dkste', 'dktek', 'dktil', 'dkved'";
    }

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