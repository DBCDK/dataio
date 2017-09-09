
package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for extraRequestDataType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="extraRequestDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="authenticationToken" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="projectid" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="editReplace" type="{http://www.loc.gov/zing/srw/update/}editReplaceType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "extraRequestDataType", propOrder = {
    "authenticationToken",
    "projectid",
    "editReplace"
})
public class ExtraRequestDataType {

    @XmlElement(required = true)
    protected String authenticationToken;
    @XmlElement(required = true)
    protected String projectid;
    @XmlElement(required = true, nillable = true)
    protected EditReplaceType editReplace;

    /**
     * Gets the value of the authenticationToken property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAuthenticationToken() {
        return authenticationToken;
    }

    /**
     * Sets the value of the authenticationToken property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAuthenticationToken(String value) {
        this.authenticationToken = value;
    }

    /**
     * Gets the value of the projectid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProjectid() {
        return projectid;
    }

    /**
     * Sets the value of the projectid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProjectid(String value) {
        this.projectid = value;
    }

    /**
     * Gets the value of the editReplace property.
     * 
     * @return
     *     possible object is
     *     {@link EditReplaceType }
     *     
     */
    public EditReplaceType getEditReplace() {
        return editReplace;
    }

    /**
     * Sets the value of the editReplace property.
     * 
     * @param value
     *     allowed object is
     *     {@link EditReplaceType }
     *     
     */
    public void setEditReplace(EditReplaceType value) {
        this.editReplace = value;
    }

}
