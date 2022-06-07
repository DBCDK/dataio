
package dk.dbc.oss.ns.updatemarcxchange;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="updateMarcXchangeRequest" type="{http://oss.dbc.dk/ns/updateMarcXchange}UpdateMarcXchangeRequest"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "updateMarcXchangeRequest"
})
@XmlRootElement(name = "updateMarcXchange")
public class UpdateMarcXchange {

    @XmlElement(required = true)
    protected UpdateMarcXchangeRequest updateMarcXchangeRequest;

    /**
     * Gets the value of the updateMarcXchangeRequest property.
     * 
     * @return
     *     possible object is
     *     {@link UpdateMarcXchangeRequest }
     *     
     */
    public UpdateMarcXchangeRequest getUpdateMarcXchangeRequest() {
        return updateMarcXchangeRequest;
    }

    /**
     * Sets the value of the updateMarcXchangeRequest property.
     * 
     * @param value
     *     allowed object is
     *     {@link UpdateMarcXchangeRequest }
     *     
     */
    public void setUpdateMarcXchangeRequest(UpdateMarcXchangeRequest value) {
        this.updateMarcXchangeRequest = value;
    }

}
