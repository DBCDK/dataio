
package dk.dbc.oss.ns.updatemarcxchange;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


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
 *         &lt;element name="updateMarcXchangeResult" type="{http://oss.dbc.dk/ns/updateMarcXchange}UpdateMarcXchangeResult" maxOccurs="unbounded"/&gt;
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the updateMarcXchangeResult property.
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
     */
    public List<UpdateMarcXchangeResult> getUpdateMarcXchangeResult() {
        if (updateMarcXchangeResult == null) {
            updateMarcXchangeResult = new ArrayList<UpdateMarcXchangeResult>();
        }
        return this.updateMarcXchangeResult;
    }

}
