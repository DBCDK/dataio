
package info.lc.xmlns.marcxchange_v1;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for recordType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>{@code
 * <complexType name="recordType">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence minOccurs="0">
 *         <element name="leader" type="{info:lc/xmlns/marcxchange-v1}leaderFieldType"/>
 *         <element name="controlfield" type="{info:lc/xmlns/marcxchange-v1}controlFieldType" maxOccurs="unbounded" minOccurs="0"/>
 *         <element name="datafield" type="{info:lc/xmlns/marcxchange-v1}dataFieldType" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *       <attribute name="format" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" />
 *       <attribute name="type" type="{http://www.w3.org/2001/XMLSchema}NMTOKEN" />
 *       <attribute name="id" type="{info:lc/xmlns/marcxchange-v1}idDataType" />
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "recordType", propOrder = {
    "leader",
    "controlfield",
    "datafield"
})
public class RecordType {

    protected LeaderFieldType leader;
    protected List<ControlFieldType> controlfield;
    protected List<DataFieldType> datafield;
    @XmlAttribute(name = "format")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    protected String format;
    @XmlAttribute(name = "type")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NMTOKEN")
    protected String type;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;

    /**
     * Gets the value of the leader property.
     * 
     * @return
     *     possible object is
     *     {@link LeaderFieldType }
     *     
     */
    public LeaderFieldType getLeader() {
        return leader;
    }

    /**
     * Sets the value of the leader property.
     * 
     * @param value
     *     allowed object is
     *     {@link LeaderFieldType }
     *     
     */
    public void setLeader(LeaderFieldType value) {
        this.leader = value;
    }

    /**
     * Gets the value of the controlfield property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the controlfield property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getControlfield().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ControlFieldType }
     * 
     * 
     * @return
     *     The value of the controlfield property.
     */
    public List<ControlFieldType> getControlfield() {
        if (controlfield == null) {
            controlfield = new ArrayList<>();
        }
        return this.controlfield;
    }

    /**
     * Gets the value of the datafield property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the datafield property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDatafield().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DataFieldType }
     * 
     * 
     * @return
     *     The value of the datafield property.
     */
    public List<DataFieldType> getDatafield() {
        if (datafield == null) {
            datafield = new ArrayList<>();
        }
        return this.datafield;
    }

    /**
     * Gets the value of the format property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the value of the format property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFormat(String value) {
        this.format = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
