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
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "diagnosticsType", namespace = "http://www.loc.gov/zing/srw/", propOrder = {
    "diagnostic",
    "extraDiagData"
})
public class DiagnosticsType {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/diagnostic/", required = true)
    protected List<Diagnostic> diagnostic;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    protected String extraDiagData;

    public List<Diagnostic> getDiagnostic() {
        if (diagnostic == null) {
            diagnostic = new ArrayList<Diagnostic>();
        }
        return this.diagnostic;
    }

    public String getExtraDiagData() {
        return extraDiagData;
    }

    public void setExtraDiagData(String value) {
        this.extraDiagData = value;
    }
}
