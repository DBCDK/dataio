
package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for updateRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="updateRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.loc.gov/zing/srw/}requestType">
 *       &lt;sequence>
 *         &lt;element name="action" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="recordIdentifier" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="recordVersions" type="{http://Update.os.oclc.ORG}ArrayOf_tns1_recordVersionType" minOccurs="0"/>
 *         &lt;element name="record" type="{http://www.loc.gov/zing/srw/}recordType"/>
 *         &lt;element name="extraRequestData" type="{http://www.loc.gov/zing/srw/update/}extraRequestDataType"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "updateRequestType", propOrder = {
    "action",
    "recordIdentifier",
    "recordVersions",
    "record",
    "extraRequestData"
})
public class UpdateRequestType
    extends RequestType
{

    @XmlElement(required = true)
    protected String action;
    protected String recordIdentifier;
    protected ArrayOfTns1RecordVersionType recordVersions;
    @XmlElement(required = true)
    protected RecordType record;
    @XmlElement(required = true)
    protected ExtraRequestDataType extraRequestData;

    /**
     * Gets the value of the action property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAction(String value) {
        this.action = value;
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
     * Gets the value of the extraRequestData property.
     * 
     * @return
     *     possible object is
     *     {@link ExtraRequestDataType }
     *     
     */
    public ExtraRequestDataType getExtraRequestData() {
        return extraRequestData;
    }

    /**
     * Sets the value of the extraRequestData property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtraRequestDataType }
     *     
     */
    public void setExtraRequestData(ExtraRequestDataType value) {
        this.extraRequestData = value;
    }

}
