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
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.reader.MarcReaderException;
import dk.dbc.marc.reader.MarcXchangeV1Reader;
import dk.dbc.oss.ns.catalogingupdate.UpdateRecordResult;
import dk.dbc.oss.ns.catalogingupdate.ValidateEntry;
import dk.dbc.oss.ns.catalogingupdate.ValidateWarningOrErrorEnum;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This class interprets an UpdateRecordResult, and fetches the field and subfield positions
 */
public class UpdateRecordErrorInterpreter {

    /**
     * Interprets the UpdateRecordResult input data, and returns a list of Diagnostics, detected in the data
     * @param updateRecordResult The input data to interpret
     * @param marcExchangeRecord The corresponding Marc record, to be used to fetch the field and subfield names
     * @return A list of Diagnostics
     * @throws MarcReaderException
     */
    public List<Diagnostic> getDiagnostics(UpdateRecordResult updateRecordResult, byte[] marcExchangeRecord) throws MarcReaderException {
        final String NO_STACK_TRACE = null; // We don't need to supply a stacktrace, and since we don't have any right now, we will not do it
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (updateRecordResult != null && updateRecordResult.getValidateInstance() != null) {
            List<ValidateEntry> validateEntries = updateRecordResult.getValidateInstance().getValidateEntry();
            if (!validateEntries.isEmpty()) {
                for (ValidateEntry entry: validateEntries) {
                    DataField dataField;
                    String field = null;
                    String subField = null;
                    try {
                        dataField = getDataField(entry.getOrdinalPositionOfField().intValue() - 1, marcExchangeRecord);
                        field = getTag(dataField);
                        subField = getAttribute(entry.getOrdinalPositionOfSubField().intValue() - 1, dataField);
                    } catch (Exception exception) {
                        // If an exception is thrown (typically a null pointer exception), leave the field and/or subField values as null values
                    }
                    diagnostics.add(new Diagnostic(getLevel(entry.getWarningOrError()), entry.getMessage(), NO_STACK_TRACE, field, subField));
                }
            }
        }
        return diagnostics;
    }

    /**
     * Given an index to the a Marc field and a byte array, containing the MarcExchangeRecord, this method constructs the
     * corresponding DataField record for the given Marc field
     * @param fieldIndex The index of the field to fetch
     * @param marcExchangeRecord The MarcExchangeRecord byte array
     * @return The constructed DataField structure
     * @throws MarcReaderException
     */
    DataField getDataField(Integer fieldIndex, byte[] marcExchangeRecord) throws MarcReaderException {
        return (DataField) getMarcRecord(marcExchangeRecord).getFields().get(fieldIndex);
    }


    /**
     * Gets a MarcRecord from the byte array, supplied as a parameter in the call to the method
     * @param marcExchangeRecord The byte array to read
     * @return The MarcRecord
     * @throws MarcReaderException
     */
    MarcRecord getMarcRecord(byte[] marcExchangeRecord) throws MarcReaderException {
        BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(marcExchangeRecord));
        return new MarcXchangeV1Reader(stream, StandardCharsets.UTF_8).read();
    }

    /**
     * Converts a Diagnostic.Level from a ValidateWarningOrErrorEnum
     * @param warningOrError The ValidateWarningOrErrorEnum
     * @return The Diagnostic.Level
     */
    Diagnostic.Level getLevel(ValidateWarningOrErrorEnum warningOrError) {
        switch (warningOrError) {
            case ERROR:
                return Diagnostic.Level.FATAL;
            case WARNING:
                return Diagnostic.Level.WARNING;
            default:
                throw new IllegalArgumentException(warningOrError.value());
        }
    }

    /**
     * Gets a specific Tag (Field) from a MarcRecord. The tagIndex points out the Tag to fetch
     * @param dataField The DataField, where the Tag lives
     * @return The Tag (Field) given as a String
     */
    String getTag(DataField dataField) {
        return dataField.getTag();
    }

    /**
     * Gets a specific Attribute (SubField) from a MarcRecord. The attributeIndex points out the Attribute to fetch
     * @param attributeIndex The index of the Attribute
     * @param dataField The DataField, where the Attribute lives
     * @return The Attribute (SubField) given as a String
     */
    String getAttribute(Integer attributeIndex, DataField dataField) {
        return dataField.getSubfields().get(attributeIndex).getCode().toString();
    }

}
