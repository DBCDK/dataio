package dk.dbc.oclc.wciru;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

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
        for (OperationStatusType c : OperationStatusType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
