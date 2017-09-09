
package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for explainResponseType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="explainResponseType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.loc.gov/zing/srw/}responseType">
 *       &lt;sequence>
 *         &lt;element name="record" type="{http://www.loc.gov/zing/srw/}recordType"/>
 *         &lt;element name="diagnostics" type="{http://www.loc.gov/zing/srw/}diagnosticsType" minOccurs="0"/>
 *         &lt;element name="extraResponseData" type="{http://www.loc.gov/zing/srw/}extraDataType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "explainResponseType", propOrder = {
    "record",
    "diagnostics",
    "extraResponseData"
})
public class ExplainResponseType
    extends ResponseType
{

    @XmlElement(required = true)
    protected RecordType record;
    protected DiagnosticsType diagnostics;
    protected ExtraDataType extraResponseData;

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
     * Gets the value of the diagnostics property.
     * 
     * @return
     *     possible object is
     *     {@link DiagnosticsType }
     *     
     */
    public DiagnosticsType getDiagnostics() {
        return diagnostics;
    }

    /**
     * Sets the value of the diagnostics property.
     * 
     * @param value
     *     allowed object is
     *     {@link DiagnosticsType }
     *     
     */
    public void setDiagnostics(DiagnosticsType value) {
        this.diagnostics = value;
    }

    /**
     * Gets the value of the extraResponseData property.
     * 
     * @return
     *     possible object is
     *     {@link ExtraDataType }
     *     
     */
    public ExtraDataType getExtraResponseData() {
        return extraResponseData;
    }

    /**
     * Sets the value of the extraResponseData property.
     * 
     * @param value
     *     allowed object is
     *     {@link ExtraDataType }
     *     
     */
    public void setExtraResponseData(ExtraDataType value) {
        this.extraResponseData = value;
    }

}
