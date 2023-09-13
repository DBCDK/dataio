package dk.dbc.oclc.wciru;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOf_tns1_recordVersionType", namespace = "http://Update.os.oclc.ORG", propOrder = {
        "item"
})
public class ArrayOfTns1RecordVersionType {

    @XmlElement(namespace = "http://Update.os.oclc.ORG")
    protected List<RecordVersionType> item;

    public List<RecordVersionType> getItem() {
        if (item == null) {
            item = new ArrayList<RecordVersionType>();
        }
        return this.item;
    }
}
