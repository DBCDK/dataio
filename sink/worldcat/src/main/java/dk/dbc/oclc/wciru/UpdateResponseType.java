
/*
 * DataIO - Data IO
 *
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

package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for updateResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="updateResponseType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.loc.gov/zing/srw/}responseType">
 *       &lt;sequence>
 *         &lt;element name="operationStatus" type="{http://www.loc.gov/zing/srw/update/}operationStatusType"/>
 *         &lt;element name="recordIdentifier" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="recordVersions" type="{http://Update.os.oclc.ORG}ArrayOf_tns1_recordVersionType" minOccurs="0"/>
 *         &lt;element name="record" type="{http://www.loc.gov/zing/srw/}recordType"/>
 *         &lt;element name="diagnostics" type="{http://www.loc.gov/zing/srw/}diagnosticsType" minOccurs="0"/>
 *         &lt;element name="extraResponseData" type="{http://www.loc.gov/zing/srw/}extraDataType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "updateResponseType", propOrder = {
    "operationStatus",
    "recordIdentifier",
    "recordVersions",
    "record",
    "diagnostics",
    "extraResponseData"
})
public class UpdateResponseType
    extends ResponseType
{
    @XmlElement(required = true, namespace = "http://www.loc.gov/zing/srw/update/" )
    protected OperationStatusType operationStatus;
    @XmlElement(required = false, namespace = "http://www.loc.gov/zing/srw/update/" )
    protected String recordIdentifier;
    protected ArrayOfTns1RecordVersionType recordVersions;
    @XmlElement(required = true, namespace = "http://www.loc.gov/zing/srw/" )
    protected RecordType record;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/" )
    protected DiagnosticsType diagnostics;
    @XmlElement(namespace = "http://www.loc.gov/zing/srw/" )
    protected ExtraDataType extraResponseData;

    /**
     * Gets the value of the operationStatus property.
     * 
     * @return
     *     possible object is
     *     {@link OperationStatusType }
     *     
     */
    public OperationStatusType getOperationStatus() {
        return operationStatus;
    }

    /**
     * Sets the value of the operationStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link OperationStatusType }
     *     
     */
    public void setOperationStatus(OperationStatusType value) {
        this.operationStatus = value;
    }

    /**
     * Gets the value of the recordIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    /**
     * Sets the value of the recordIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRecordIdentifier(String value) {
        this.recordIdentifier = value;
    }

    /**
     * Gets the value of the recordVersions property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfTns1RecordVersionType }
     *     
     */
    public ArrayOfTns1RecordVersionType getRecordVersions() {
        return recordVersions;
    }

    /**
     * Sets the value of the recordVersions property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfTns1RecordVersionType }
     *     
     */
    public void setRecordVersions(ArrayOfTns1RecordVersionType value) {
        this.recordVersions = value;
    }

    /**
     * Gets the value of the record property.
     * 
     * @return
     *     possible object is
     *     {@link RecordType }
     *     
     */
    public RecordType getRecord() {
        return record;
    }

    /**
     * Sets the value of the record property.
     * 
     * @param value
     *     allowed object is
     *     {@link RecordType }
     *     
     */
    public void setRecord(RecordType value) {
        this.record = value;
    }

    /**
     * Gets the value of the diagnostics property.
     * 
     * @return
     *     possible object is
     *     {@link DiagnosticsType }
     *     
     */
    public DiagnosticsType getDiagnostics() {
        return diagnostics;
    }

    /**
     * Sets the value of the diagnostics property.
     * 
     * @param value
     *     allowed object is
     *     {@link DiagnosticsType }
     *     
     */
    public void setDiagnostics(DiagnosticsType value) {
        this.diagnostics = value;
    }

    /**
     * Gets the value of the extraResponseData property.
     * 
     * @return
     *     possible object is
     *     {@link ExtraDataType }
     *     
     */
    public ExtraDataType getExtraResponseData() {
        return extraResponseData;
    }

    /**
     * Sets the value of the extraResponseData property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtraDataType }
     *     
     */
    public void setExtraResponseData(ExtraDataType value) {
        this.extraResponseData = value;
    }

}
