package dk.dbc.dataio.flowstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Persistence domain class for gatekeeperDestination objects where id is auto
 * generated by the underlying store
 */
@Entity
@Table(name = "gatekeeper_destinations")
@NamedQueries({
        @NamedQuery(name = GatekeeperDestinationEntity.QUERY_FIND_ALL, query = "SELECT gatekeeperDestinationEntity FROM GatekeeperDestinationEntity gatekeeperDestinationEntity ORDER BY gatekeeperDestinationEntity.submitterNumber ASC")
})

public class GatekeeperDestinationEntity {

    public static final String QUERY_FIND_ALL = "GatekeeperDestinationEntity.findAll";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String submitterNumber;
    private String destination;
    private String packaging;
    private String format;

    public GatekeeperDestinationEntity() {
    }

    public long getId() {
        return id;
    }

    public String getSubmitterNumber() {
        return submitterNumber;
    }

    public void setSubmitterNumber(String submitterNumber) {
        this.submitterNumber = submitterNumber;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

}
