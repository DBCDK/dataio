
package dk.dbc.oss.ns.updatemarcxchange;

import info.lc.xmlns.marcxchange_v1.CollectionType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MarcXchangeRecord complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="MarcXchangeRecord">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element ref="{info:lc/xmlns/marcxchange-v1}collection"/>
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
@XmlType(name = "MarcXchangeRecord", propOrder = {
    "collection",
    "marcXchangeRecordId"
})
public class MarcXchangeRecord {

    @XmlElement(namespace = "info:lc/xmlns/marcxchange-v1", required = true, nillable = true)
    protected CollectionType collection;
    @XmlElement(name = "MarcXchangeRecordId", required = true)
    protected String marcXchangeRecordId;

    /**
     * Gets the value of the collection property.
     * 
     * @return
     *     possible object is
     *     {@link CollectionType }
     *     
     */
    public CollectionType getCollection() {
        return collection;
    }

    /**
     * Sets the value of the collection property.
     * 
     * @param value
     *     allowed object is
     *     {@link CollectionType }
     *     
     */
    public void setCollection(CollectionType value) {
        this.collection = value;
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
