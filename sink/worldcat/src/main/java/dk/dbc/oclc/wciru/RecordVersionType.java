
package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for recordVersionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="recordVersionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="versionType" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="versionValue" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "recordVersionType", propOrder = {
    "versionType",
    "versionValue"
})
public class RecordVersionType {

    @XmlElement(required = true)
    protected String versionType;
    @XmlElement(required = true)
    protected String versionValue;

    /**
     * Gets the value of the versionType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersionType() {
        return versionType;
    }

    /**
     * Sets the value of the versionType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersionType(String value) {
        this.versionType = value;
    }

    /**
     * Gets the value of the versionValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersionValue() {
        return versionValue;
    }

    /**
     * Sets the value of the versionValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersionValue(String value) {
        this.versionValue = value;
    }

}
