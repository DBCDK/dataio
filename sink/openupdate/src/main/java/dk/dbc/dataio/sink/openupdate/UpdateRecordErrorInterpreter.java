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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class interprets an UpdateRecordResult, and fetches the field and subfield positions
 */
public class UpdateRecordErrorInterpreter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRecordErrorInterpreter.class);

    /**
     * Interprets the UpdateRecordResult input data, and returns a list of Diagnostics, detected in the data
     * @param updateRecordResult The input data to interpret
     * @param marcExchangeRecord The corresponding Marc record, to be used to fetch the field and subfield names
     * @return A list of Diagnostics
     */
    public List<Diagnostic> getDiagnostics(UpdateRecordResult updateRecordResult, byte[] marcExchangeRecord) {
        final String NO_STACK_TRACE = null; // We don't need to supply a stacktrace, and since we don't have any right now, we will not do it
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (updateRecordResult != null && updateRecordResult.getValidateInstance() != null) {
            List<ValidateEntry> validateEntries = updateRecordResult.getValidateInstance().getValidateEntry();
            if (!validateEntries.isEmpty()) {
                for (ValidateEntry entry: validateEntries) {
                    String field = null;
                    String subField = null;
                    try {
                        Optional<DataField> dataField = getDataField(entry, marcExchangeRecord);
                        if (dataField.isPresent()) {
                            field = getTag(dataField);
                            subField = getAttribute(entry, dataField);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error detected during extraction of Tag and/or Attribute", e);
                    }
                    diagnostics.add(new Diagnostic(getLevel(entry.getWarningOrError()), entry.getMessage(), NO_STACK_TRACE, field, subField));
                }
            }
        }
        return diagnostics;
    }

    /**
     * Given a Marc field and a byte array, containing a MarcExchangeRecord, this method constructs the
     * corresponding DataField record for the given Marc field
     * @param entry The ValidateEntry containing the field to fetch
     * @param marcExchangeRecord The MarcExchangeRecord byte array
     * @return The constructed DataField structure
     * @throws MarcReaderException on exception caught while creating parser
     */
    Optional<DataField> getDataField(ValidateEntry entry, byte[] marcExchangeRecord) throws MarcReaderException {
        Integer fieldIndex = entry.getOrdinalPositionOfField().intValue() - 1;
        return Optional.of((DataField) getMarcRecord(marcExchangeRecord).getFields().get(fieldIndex));
    }


    /**
     * Gets a MarcRecord from the byte array, supplied as a parameter in the call to the method
     * @param marcExchangeRecord The byte array to read
     * @return The MarcRecord
     * @throws MarcReaderException on exception caught while creating parser
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
     * Gets a specific Tag (Field) from a MarcRecord.
     * @param dataField The DataField, where the Tag lives
     * @return The Tag (Field) given as a String
     */
    String getTag(Optional<DataField> dataField) {
        return dataField.get().getTag();
    }

    /**
     * Gets a specific Attribute (SubField) from a MarcRecord. The attributeIndex points out the Attribute to fetch
     * @param entry The ValidateEntry containing the field to fetch
     * @param dataField The DataField, where the Attribute lives
     * @return The Attribute (SubField) given as a String
     */
    String getAttribute(ValidateEntry entry, Optional<DataField> dataField) {
        Integer attributeIndex = entry.getOrdinalPositionOfSubField().intValue() - 1;
        return dataField.get().getSubfields().get(attributeIndex).getCode().toString();
    }

}
