package dk.dbc.dataio.sink.openupdate.bindings;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * This class represent extra data to associated with the record in an update
 * request.
 *
 * <h3>Properties</h3>
 *
 * <dl>
 *     <dt>providerName</dt>
 *     <dd>
 *         Provider name is a string that is used to specify which queue in rawrepo that the updated record should
 *         be placed in when the record is updated.
 *     </dd>
 * </dl>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(namespace = BibliographicRecordExtraData.NAMESPACE, name = "updateRecordExtraData")
public class BibliographicRecordExtraData {
    public static final String NAMESPACE = "http://oss.dbc.dk/ns/updateRecordExtraData";

    private String providerName;
    private Integer priority;

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
