
package dk.dbc.oss.ns.updatemarcxchange;

import java.util.ArrayList;
import java.util.List;
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
 *         <element name="updateMarcXchangeResult" type="{http://oss.dbc.dk/ns/updateMarcXchange}UpdateMarcXchangeResult" maxOccurs="unbounded"/>
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
    "updateMarcXchangeResult"
})
@XmlRootElement(name = "updateMarcXchangeResponse")
public class UpdateMarcXchangeResponse {

    @XmlElement(required = true)
    protected List<UpdateMarcXchangeResult> updateMarcXchangeResult;

    /**
     * Gets the value of the updateMarcXchangeResult property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the updateMarcXchangeResult property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUpdateMarcXchangeResult().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link UpdateMarcXchangeResult }
     * 
     * 
     * @return
     *     The value of the updateMarcXchangeResult property.
     */
    public List<UpdateMarcXchangeResult> getUpdateMarcXchangeResult() {
        if (updateMarcXchangeResult == null) {
            updateMarcXchangeResult = new ArrayList<>();
        }
        return this.updateMarcXchangeResult;
    }

}
