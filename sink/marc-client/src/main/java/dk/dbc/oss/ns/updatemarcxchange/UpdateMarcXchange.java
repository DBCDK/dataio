
package dk.dbc.oss.ns.updatemarcxchange;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="updateMarcXchangeRequest" type="{http://oss.dbc.dk/ns/updateMarcXchange}UpdateMarcXchangeRequest"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
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
