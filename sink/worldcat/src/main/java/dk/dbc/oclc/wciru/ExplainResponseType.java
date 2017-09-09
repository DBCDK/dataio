/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "explainResponseType", propOrder = {
    "record",
    "diagnostics",
    "extraResponseData"
})
public class ExplainResponseType
    extends ResponseType
{

    @XmlElement(required = true)
    protected RecordType record;
    protected DiagnosticsType diagnostics;
    protected ExtraDataType extraResponseData;

    public RecordType getRecord() {
        return record;
    }

    public void setRecord(RecordType value) {
        this.record = value;
    }

    public DiagnosticsType getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(DiagnosticsType value) {
        this.diagnostics = value;
    }

    public ExtraDataType getExtraResponseData() {
        return extraResponseData;
    }

    public void setExtraResponseData(ExtraDataType value) {
        this.extraResponseData = value;
    }
}
