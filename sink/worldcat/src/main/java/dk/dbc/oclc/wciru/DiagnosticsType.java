
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
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for diagnosticsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="diagnosticsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="diagnostic" type="{http://www.loc.gov/zing/srw/diagnostic/}diagnostic" maxOccurs="unbounded"/>
 *         &lt;element name="extraDiagData" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
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

    /**
     * Gets the value of the diagnostic property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the diagnostic property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDiagnostic().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Diagnostic }
     * 
     * 
     */
    public List<Diagnostic> getDiagnostic() {
        if (diagnostic == null) {
            diagnostic = new ArrayList<Diagnostic>();
        }
        return this.diagnostic;
    }

    /**
     * Gets the value of the extraDiagData property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExtraDiagData() {
        return extraDiagData;
    }

    /**
     * Sets the value of the extraDiagData property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExtraDiagData(String value) {
        this.extraDiagData = value;
    }

}
