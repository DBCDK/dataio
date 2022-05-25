package dk.dbc.dataio.cli.jobreplicator;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.JobSpecification;

public class JobReplicatorInfo {
    private JobSpecification jobSpecification;
    // six-digit submitter number in human readable format
    private long submitterNumber;
    // database id of submitter entity
    private long submitterId;
    private String targetSinkName;
    private FlowStoreServiceConnector sourceFlowStoreConnector,
            targetFlowStoreConnector;

    public JobSpecification getJobSpecification() {
        return jobSpecification;
    }

    public JobReplicatorInfo withJobSpecification(JobSpecification jobSpecification) {
        this.jobSpecification = jobSpecification;
        return this;
    }

    public long getSubmitterNumber() {
        return submitterNumber;
    }

    public long getSubmitterId() {
        return submitterId;
    }

    public JobReplicatorInfo withSubmitterId(long submitterId) {
        this.submitterId = submitterId;
        return this;
    }

    public JobReplicatorInfo withSubmitterNumber(long submitterNumber) {
        this.submitterNumber = submitterNumber;
        return this;
    }

    public String getTargetSinkName() {
        return targetSinkName;
    }

    public JobReplicatorInfo withTargetSinkName(String targetSinkName) {
        this.targetSinkName = targetSinkName;
        return this;
    }

    public FlowStoreServiceConnector getSourceFlowStoreConnector() {
        return sourceFlowStoreConnector;
    }

    public JobReplicatorInfo withSourceFlowStoreConnector(
            FlowStoreServiceConnector sourceFlowStoreConnector) {
        this.sourceFlowStoreConnector = sourceFlowStoreConnector;
        return this;
    }

    public FlowStoreServiceConnector getTargetFlowStoreConnector() {
        return targetFlowStoreConnector;
    }

    public JobReplicatorInfo withTargetFlowStoreConnector(
            FlowStoreServiceConnector targetFlowStoreConnector) {
        this.targetFlowStoreConnector = targetFlowStoreConnector;
        return this;
    }
}
