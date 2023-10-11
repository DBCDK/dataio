
package dk.dbc.oss.ns.updatemarcxchange;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UpdateMarcXchangeRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="UpdateMarcXchangeRequest">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="marcXchangeRecord" type="{http://oss.dbc.dk/ns/updateMarcXchange}MarcXchangeRecord" maxOccurs="unbounded"/>
 *         <element ref="{http://oss.dbc.dk/ns/updateMarcXchange}trackingId" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UpdateMarcXchangeRequest", propOrder = {
    "marcXchangeRecord",
    "trackingId"
})
public class UpdateMarcXchangeRequest {

    @XmlElement(required = true)
    protected List<MarcXchangeRecord> marcXchangeRecord;
    protected String trackingId;

    /**
     * Gets the value of the marcXchangeRecord property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the marcXchangeRecord property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMarcXchangeRecord().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MarcXchangeRecord }
     * 
     * 
     * @return
     *     The value of the marcXchangeRecord property.
     */
    public List<MarcXchangeRecord> getMarcXchangeRecord() {
        if (marcXchangeRecord == null) {
            marcXchangeRecord = new ArrayList<>();
        }
        return this.marcXchangeRecord;
    }

    /**
     * Gets the value of the trackingId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     * Sets the value of the trackingId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrackingId(String value) {
        this.trackingId = value;
    }

}
