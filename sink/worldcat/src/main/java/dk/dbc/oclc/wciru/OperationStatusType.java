
package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for operationStatusType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="operationStatusType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="success"/>
 *     &lt;enumeration value="fail"/>
 *     &lt;enumeration value="partial"/>
 *     &lt;enumeration value="delayed"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "operationStatusType")
@XmlEnum
public enum OperationStatusType {

    @XmlEnumValue("success")
    SUCCESS("success"),
    @XmlEnumValue("fail")
    FAIL("fail"),
    @XmlEnumValue("partial")
    PARTIAL("partial"),
    @XmlEnumValue("delayed")
    DELAYED("delayed");
    private final String value;

    OperationStatusType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static OperationStatusType fromValue(String v) {
        for (OperationStatusType c: OperationStatusType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
