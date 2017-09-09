
package dk.dbc.oclc.wciru;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for recordType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="recordType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="recordSchema" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="recordPacking" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="recordData" type="{http://www.loc.gov/zing/srw/}stringOrXmlFragment"/>
 *         &lt;element name="recordPosition" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" minOccurs="0"/>
 *         &lt;element name="extraRecordData" type="{http://www.loc.gov/zing/srw/}extraDataType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
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

    /**
     * Gets the value of the recordSchema property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRecordSchema() {
        return recordSchema;
    }

    /**
     * Sets the value of the recordSchema property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRecordSchema(String value) {
        this.recordSchema = value;
    }

    /**
     * Gets the value of the recordPacking property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRecordPacking() {
        return recordPacking;
    }

    /**
     * Sets the value of the recordPacking property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRecordPacking(String value) {
        this.recordPacking = value;
    }

    /**
     * Gets the value of the recordData property.
     * 
     * @return
     *     possible object is
     *     {@link StringOrXmlFragment }
     *     
     */
    public StringOrXmlFragment getRecordData() {
        return recordData;
    }

    /**
     * Sets the value of the recordData property.
     * 
     * @param value
     *     allowed object is
     *     {@link StringOrXmlFragment }
     *     
     */
    public void setRecordData(StringOrXmlFragment value) {
        this.recordData = value;
    }

    /**
     * Gets the value of the recordPosition property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getRecordPosition() {
        return recordPosition;
    }

    /**
     * Sets the value of the recordPosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setRecordPosition(BigInteger value) {
        this.recordPosition = value;
    }

    /**
     * Gets the value of the extraRecordData property.
     * 
     * @return
     *     possible object is
     *     {@link ExtraDataType }
     *     
     */
    public ExtraDataType getExtraRecordData() {
        return extraRecordData;
    }

    /**
     * Sets the value of the extraRecordData property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtraDataType }
     *     
     */
    public void setExtraRecordData(ExtraDataType value) {
        this.extraRecordData = value;
    }

}
