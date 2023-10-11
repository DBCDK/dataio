
package dk.dbc.oss.ns.updatemarcxchange;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UpdateMarcXchangeResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="UpdateMarcXchangeResult">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="updateMarcXchangeStatus" type="{http://oss.dbc.dk/ns/updateMarcXchange}updateMarcXchangeStatusEnum"/>
 *         <element ref="{http://oss.dbc.dk/ns/updateMarcXchange}updateMarcXchangeMessage" minOccurs="0"/>
 *         <element ref="{http://oss.dbc.dk/ns/updateMarcXchange}MarcXchangeRecordId"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UpdateMarcXchangeResult", propOrder = {
    "updateMarcXchangeStatus",
    "updateMarcXchangeMessage",
    "marcXchangeRecordId"
})
public class UpdateMarcXchangeResult {

    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected UpdateMarcXchangeStatusEnum updateMarcXchangeStatus;
    protected String updateMarcXchangeMessage;
    @XmlElement(name = "MarcXchangeRecordId", required = true)
    protected String marcXchangeRecordId;

    /**
     * Gets the value of the updateMarcXchangeStatus property.
     * 
     * @return
     *     possible object is
     *     {@link UpdateMarcXchangeStatusEnum }
     *     
     */
    public UpdateMarcXchangeStatusEnum getUpdateMarcXchangeStatus() {
        return updateMarcXchangeStatus;
    }

    /**
     * Sets the value of the updateMarcXchangeStatus property.
     * 
     * @param value
     *     allowed object is
     *     {@link UpdateMarcXchangeStatusEnum }
     *     
     */
    public void setUpdateMarcXchangeStatus(UpdateMarcXchangeStatusEnum value) {
        this.updateMarcXchangeStatus = value;
    }

    /**
     * Gets the value of the updateMarcXchangeMessage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUpdateMarcXchangeMessage() {
        return updateMarcXchangeMessage;
    }

    /**
     * Sets the value of the updateMarcXchangeMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUpdateMarcXchangeMessage(String value) {
        this.updateMarcXchangeMessage = value;
    }

    /**
     * Gets the value of the marcXchangeRecordId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMarcXchangeRecordId() {
        return marcXchangeRecordId;
    }

    /**
     * Sets the value of the marcXchangeRecordId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMarcXchangeRecordId(String value) {
        this.marcXchangeRecordId = value;
    }

}
