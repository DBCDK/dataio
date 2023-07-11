package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "delivery")
@NamedQueries({
        @NamedQuery(
                name = PeriodicJobsDelivery.DELETE_DELIVERY_QUERY_NAME,
                query = PeriodicJobsDelivery.DELETE_DELIVERY_QUERY)
})
public class PeriodicJobsDelivery {
    public static final String DELETE_DELIVERY_QUERY =
            "DELETE FROM PeriodicJobsDelivery delivery" +
                    " WHERE delivery.jobId = :jobId";
    public static final String DELETE_DELIVERY_QUERY_NAME =
            "PeriodicJobsDelivery.delete";

    @Id
    private Integer jobId;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = PeriodicJobsHarvesterConfigConverter.class)
    private PeriodicJobsHarvesterConfig config;

    public PeriodicJobsDelivery() {
    }

    public PeriodicJobsDelivery(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getJobId() {
        return jobId;
    }

    public PeriodicJobsHarvesterConfig getConfig() {
        return config;
    }

    public void setConfig(PeriodicJobsHarvesterConfig config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "PeriodicJobsDelivery{" +
                "jobId=" + jobId +
                ", config=" + config +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PeriodicJobsDelivery delivery = (PeriodicJobsDelivery) o;

        if (!Objects.equals(jobId, delivery.jobId)) {
            return false;
        }
        return Objects.equals(config, delivery.config);
    }

    @Override
    public int hashCode() {
        int result = jobId != null ? jobId.hashCode() : 0;
        result = 31 * result + (config != null ? config.hashCode() : 0);
        return result;
    }
}
