
package dk.dbc.oss.ns.updatemarcxchange;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for updateMarcXchangeStatusEnum.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="updateMarcXchangeStatusEnum"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="ok"/&gt;
 *     &lt;enumeration value="update_failed_please_resend_later"/&gt;
 *     &lt;enumeration value="update_failed_invalid_record"/&gt;
 *     &lt;enumeration value="update_failed_fatal_internal_error"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "updateMarcXchangeStatusEnum")
@XmlEnum
public enum UpdateMarcXchangeStatusEnum {

    @XmlEnumValue("ok")
    OK("ok"),
    @XmlEnumValue("update_failed_please_resend_later")
    UPDATE_FAILED_PLEASE_RESEND_LATER("update_failed_please_resend_later"),
    @XmlEnumValue("update_failed_invalid_record")
    UPDATE_FAILED_INVALID_RECORD("update_failed_invalid_record"),
    @XmlEnumValue("update_failed_fatal_internal_error")
    UPDATE_FAILED_FATAL_INTERNAL_ERROR("update_failed_fatal_internal_error");
    private final String value;

    UpdateMarcXchangeStatusEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static UpdateMarcXchangeStatusEnum fromValue(String v) {
        for (UpdateMarcXchangeStatusEnum c: UpdateMarcXchangeStatusEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
