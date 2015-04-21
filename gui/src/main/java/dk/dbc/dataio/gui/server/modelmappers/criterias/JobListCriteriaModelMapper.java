package dk.dbc.dataio.gui.server.modelmappers.criterias;

import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;

public final class JobListCriteriaModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private JobListCriteriaModelMapper() {}

    public static JobListCriteria toJobListCriteria(JobListCriteriaModel model) {
        JobListCriteria jobListCriteria = new JobListCriteria();
        switch (model.getSearchType()) {
            case PROCESSING_FAILED:
                jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.STATE_PROCESSING_FAILED));
                break;
            case DELIVERING_FAILED:
                jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.STATE_DELIVERING_FAILED));
                break;
        }
        ListOrderBy descendingTimeOfCreation = new ListOrderBy<JobListCriteria.Field>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC);
        jobListCriteria.orderBy(descendingTimeOfCreation);
        return jobListCriteria;
    }
}
