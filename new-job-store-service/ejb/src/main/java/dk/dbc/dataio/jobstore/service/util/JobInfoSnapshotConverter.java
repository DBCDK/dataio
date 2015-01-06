package dk.dbc.dataio.jobstore.service.util;

public final class JobInfoSnapshotConverter {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private JobInfoSnapshotConverter() {}

    /**
     *
     * Maps information from a job entity to a JobInfoSnapshot
     *
     * @param jobEntity the job entity
     * @return jobInfoSnapshot containing information about the job from one exact moment in time (now)
     */
    /* TODO - temporarily commented out as not in use yet
    private static JobInfoSnapshot toJobInfoSnapshot(JobEntity jobEntity) {
        JobInfoSnapshot jobInfoSnapshot = new JobInfoSnapshot();
        jobInfoSnapshot.setJobId(jobEntity.getId());
        jobInfoSnapshot.setEoj(jobEntity.isEoj());
        jobInfoSnapshot.setPartNumber(jobEntity.getPartNumber());
        jobInfoSnapshot.setNumberOfChunks(jobEntity.getNumberOfChunks());
        jobInfoSnapshot.setNumberOfItems(jobEntity.getNumberOfItems());
        jobInfoSnapshot.setTimeOfCreation(jobEntity.getTimeOfCreation());
        jobInfoSnapshot.setTimeOfLastModification(jobEntity.getTimeOfLastModification());
        jobInfoSnapshot.setTimeOfCompletion(jobEntity.getTimeOfCompletion());
        jobInfoSnapshot.setSpecification(jobEntity.getSpecification());
        jobInfoSnapshot.setFlowName(jobEntity.getFlowName());
        jobInfoSnapshot.setSinkName(jobEntity.getSinkName());
        jobInfoSnapshot.setState(jobEntity.getState());
        return jobInfoSnapshot;
    }
    */
}
