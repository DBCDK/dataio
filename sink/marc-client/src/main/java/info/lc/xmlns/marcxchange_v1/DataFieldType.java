
package info.lc.xmlns.marcxchange_v1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * ISO 2709 data fields
 * 
 * <p>Java class for dataFieldType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="dataFieldType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence maxOccurs="unbounded"&gt;
 *         &lt;element name="subfield" type="{info:lc/xmlns/marcxchange-v1}subfieldatafieldType"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" type="{info:lc/xmlns/marcxchange-v1}idDataType" /&gt;
 *       &lt;attribute name="tag" use="required" type="{info:lc/xmlns/marcxchange-v1}tagDataType" /&gt;
 *       &lt;attribute name="ind1" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind2" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind3" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind4" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind5" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind6" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind7" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind8" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *       &lt;attribute name="ind9" type="{info:lc/xmlns/marcxchange-v1}indicatorDataType" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dataFieldType", propOrder = {
    "subfield"
})
public class DataFieldType {

    @XmlElement(required = true)
    protected List<SubfieldatafieldType> subfield;
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    @XmlAttribute(name = "tag", required = true)
    protected String tag;
    @XmlAttribute(name = "ind1")
    protected String ind1;
    @XmlAttribute(name = "ind2")
    protected String ind2;
    @XmlAttribute(name = "ind3")
    protected String ind3;
    @XmlAttribute(name = "ind4")
    protected String ind4;
    @XmlAttribute(name = "ind5")
    protected String ind5;
    @XmlAttribute(name = "ind6")
    protected String ind6;
    @XmlAttribute(name = "ind7")
    protected String ind7;
    @XmlAttribute(name = "ind8")
    protected String ind8;
    @XmlAttribute(name = "ind9")
    protected String ind9;

    /**
     * Gets the value of the subfield property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subfield property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubfield().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SubfieldatafieldType }
     * 
     * 
     */
    public List<SubfieldatafieldType> getSubfield() {
        if (subfield == null) {
            subfield = new ArrayList<SubfieldatafieldType>();
        }
        return this.subfield;
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

    /**
     * Gets the value of the tag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the value of the tag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTag(String value) {
        this.tag = value;
    }

    /**
     * Gets the value of the ind1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd1() {
        return ind1;
    }

    /**
     * Sets the value of the ind1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd1(String value) {
        this.ind1 = value;
    }

    /**
     * Gets the value of the ind2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd2() {
        return ind2;
    }

    /**
     * Sets the value of the ind2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd2(String value) {
        this.ind2 = value;
    }

    /**
     * Gets the value of the ind3 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd3() {
        return ind3;
    }

    /**
     * Sets the value of the ind3 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd3(String value) {
        this.ind3 = value;
    }

    /**
     * Gets the value of the ind4 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd4() {
        return ind4;
    }

    /**
     * Sets the value of the ind4 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd4(String value) {
        this.ind4 = value;
    }

    /**
     * Gets the value of the ind5 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd5() {
        return ind5;
    }

    /**
     * Sets the value of the ind5 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd5(String value) {
        this.ind5 = value;
    }

    /**
     * Gets the value of the ind6 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd6() {
        return ind6;
    }

    /**
     * Sets the value of the ind6 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd6(String value) {
        this.ind6 = value;
    }

    /**
     * Gets the value of the ind7 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd7() {
        return ind7;
    }

    /**
     * Sets the value of the ind7 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd7(String value) {
        this.ind7 = value;
    }

    /**
     * Gets the value of the ind8 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd8() {
        return ind8;
    }

    /**
     * Sets the value of the ind8 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd8(String value) {
        this.ind8 = value;
    }

    /**
     * Gets the value of the ind9 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInd9() {
        return ind9;
    }

    /**
     * Sets the value of the ind9 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInd9(String value) {
        this.ind9 = value;
    }

}
