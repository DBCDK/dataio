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

package dk.dbc.dataio.marc.writer;

import dk.dbc.dataio.marc.binding.ControlField;
import dk.dbc.dataio.marc.binding.DataField;
import dk.dbc.dataio.marc.binding.Field;
import dk.dbc.dataio.marc.binding.Leader;
import dk.dbc.dataio.marc.binding.MarcRecord;
import dk.dbc.dataio.marc.binding.SubField;

import java.nio.charset.Charset;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml10;

/**
 * MarcWriter implementation for transforming MarcRecord instances into
 * MarcXchange v1.1 representations.
 *
 * This class is thread safe.
 */
public class MarcXchangeV11Writer implements MarcWriter {
    @Override
    public byte[] write(MarcRecord marcRecord, Charset encoding) {
        final StringBuilder buffer = new StringBuilder();
        addXmlDeclaration(buffer, encoding);
        addRecord(buffer, marcRecord);
        return buffer.toString().getBytes(encoding);
    }

    private void addRecord(StringBuilder buffer, MarcRecord marcRecord) {
        buffer.append("<record xmlns='info:lc/xmlns/marcxchange-v1' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd'>");
        addLeader(buffer, marcRecord.getLeader());
        marcRecord.getFields().stream()
                .forEach(field -> addField(buffer, field));
        buffer.append("</record>");
    }

    private void addLeader(StringBuilder buffer, Leader leader) {
        buffer.append(String.format("<leader>%s</leader>",
                escape(leader.getData())));
    }

    private void addField(StringBuilder buffer, Field field) {
        if (field instanceof ControlField) {
            addControlField(buffer, (ControlField) field);
        } else {
            addDataField(buffer, (DataField) field);
        }
    }

    private void addControlField(StringBuilder buffer, ControlField field) {
        buffer.append(String.format("<controlfield tag='%s'>%s</controlfield>",
                escape(field.getTag()), escape(field.getData())));
    }

    private void addDataField(StringBuilder buffer, DataField field) {
        buffer.append(String.format("<datafield ind1='%s' ind2='%s' tag='%s'>",
                escape(field.getInd1()), escape(field.getInd2()), escape(field.getTag())));
        field.getSubfields().stream()
                .forEach(subfield -> addSubField(buffer, subfield));
        buffer.append("</datafield>");
    }

    private void addSubField(StringBuilder buffer, SubField subField) {
        buffer.append(String.format("<subfield code='%s'>%s</subfield>",
                escape(subField.getCode()), escape(subField.getData())));
    }

    private void addXmlDeclaration(StringBuilder buffer, Charset encoding) {
        buffer.append(String.format("<?xml version='1.0' encoding='%s'?>\n", encoding.name()));
    }

    private String escape(String s) {
        /* Note that Unicode characters greater than 0x7f are not escaped.
           If we at some point need this functionality, we can achieve it via the following:
           StringEscapeUtils.ESCAPE_XML10.with( NumericEntityEscaper.between(0x7f, Integer.MAX_VALUE) );
        */
        return escapeXml10(s);
    }

    private String escape(char c) {
        return escape(String.valueOf(c));
    }
}
