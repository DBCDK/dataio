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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import java.math.BigInteger;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "recordType", namespace = "http://www.loc.gov/zing/srw/", propOrder = {
    "recordSchema",
    "recordPacking",
    "recordData",
    "recordPosition",
    "extraRecordData"
})
public class RecordType {

    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected String recordSchema;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected String recordPacking;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/", required = true)
    protected StringOrXmlFragment recordData;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger recordPosition;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/")
    protected ExtraDataType extraRecordData;

    public String getRecordSchema() {
        return recordSchema;
    }

    public void setRecordSchema(String value) {
        this.recordSchema = value;
    }

    public String getRecordPacking() {
        return recordPacking;
    }

    public void setRecordPacking(String value) {
        this.recordPacking = value;
    }

    public StringOrXmlFragment getRecordData() {
        return recordData;
    }

    public void setRecordData(StringOrXmlFragment value) {
        this.recordData = value;
    }

    public BigInteger getRecordPosition() {
        return recordPosition;
    }

    public void setRecordPosition(BigInteger value) {
        this.recordPosition = value;
    }

    public ExtraDataType getExtraRecordData() {
        return extraRecordData;
    }

    public void setExtraRecordData(ExtraDataType value) {
        this.extraRecordData = value;
    }
}
