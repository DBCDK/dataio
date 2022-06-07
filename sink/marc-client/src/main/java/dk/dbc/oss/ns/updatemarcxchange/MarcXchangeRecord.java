
package dk.dbc.oss.ns.updatemarcxchange;

import info.lc.xmlns.marcxchange_v1.CollectionType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MarcXchangeRecord complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MarcXchangeRecord"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{info:lc/xmlns/marcxchange-v1}collection"/&gt;
 *         &lt;element ref="{http://oss.dbc.dk/ns/updateMarcXchange}MarcXchangeRecordId"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
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
