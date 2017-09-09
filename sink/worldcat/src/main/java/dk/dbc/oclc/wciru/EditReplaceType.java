
package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for editReplaceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="editReplaceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="dataIdentifier" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="oldValue" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="newValue" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="editReplaceType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "editReplaceType", propOrder = {
    "dataIdentifier",
    "oldValue",
    "newValue",
    "editReplaceType"
})
public class EditReplaceType {

    @XmlElement(required = true)
    protected String dataIdentifier;
    @XmlElement(required = true)
    protected String oldValue;
    @XmlElement(required = true)
    protected String newValue;
    @XmlElement(required = true)
    protected String editReplaceType;

    /**
     * Gets the value of the dataIdentifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataIdentifier() {
        return dataIdentifier;
    }

    /**
     * Sets the value of the dataIdentifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataIdentifier(String value) {
        this.dataIdentifier = value;
    }

    /**
     * Gets the value of the oldValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOldValue() {
        return oldValue;
    }

    /**
     * Sets the value of the oldValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOldValue(String value) {
        this.oldValue = value;
    }

    /**
     * Gets the value of the newValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNewValue() {
        return newValue;
    }

    /**
     * Sets the value of the newValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNewValue(String value) {
        this.newValue = value;
    }

    /**
     * Gets the value of the editReplaceType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEditReplaceType() {
        return editReplaceType;
    }

    /**
     * Sets the value of the editReplaceType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEditReplaceType(String value) {
        this.editReplaceType = value;
    }

}
