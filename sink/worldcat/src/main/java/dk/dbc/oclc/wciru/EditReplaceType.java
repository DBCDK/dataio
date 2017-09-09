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
@XmlType(name = "editReplaceType", propOrder = {
    "dataIdentifier",
    "oldValue",
    "newValue",
    "editReplaceType"
})
public class EditReplaceType {

    @XmlElement(required = true)
    protected String dataIdentifier;
    @XmlElement(required = true)
    protected String oldValue;
    @XmlElement(required = true)
    protected String newValue;
    @XmlElement(required = true)
    protected String editReplaceType;

    public String getDataIdentifier() {
        return dataIdentifier;
    }

    public void setDataIdentifier(String value) {
        this.dataIdentifier = value;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String value) {
        this.oldValue = value;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String value) {
        this.newValue = value;
    }

    public String getEditReplaceType() {
        return editReplaceType;
    }

    public void setEditReplaceType(String value) {
        this.editReplaceType = value;
    }
}
