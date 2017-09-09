
package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for explainRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="explainRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.loc.gov/zing/srw/}requestType">
 *       &lt;sequence>
 *         &lt;element name="recordPacking" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="extraRequestData" type="{http://www.loc.gov/zing/srw/}extraDataType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "explainRequestType", propOrder = {
    "recordPacking",
    "extraRequestData"
})
public class ExplainRequestType
    extends RequestType
{

    protected String recordPacking;
    protected ExtraDataType extraRequestData;

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
     * Gets the value of the extraRequestData property.
     * 
     * @return
     *     possible object is
     *     {@link ExtraDataType }
     *     
     */
    public ExtraDataType getExtraRequestData() {
        return extraRequestData;
    }

    /**
     * Sets the value of the extraRequestData property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtraDataType }
     *     
     */
    public void setExtraRequestData(ExtraDataType value) {
        this.extraRequestData = value;
    }

}
