/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

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

    public PeriodicJobsDelivery() {}

    PeriodicJobsDelivery(Integer jobId) {
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
}
