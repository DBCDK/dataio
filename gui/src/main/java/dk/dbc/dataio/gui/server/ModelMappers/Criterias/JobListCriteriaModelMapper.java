package dk.dbc.dataio.gui.server.ModelMappers.Criterias;

import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;

public final class JobListCriteriaModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private JobListCriteriaModelMapper() {}

    public static JobListCriteria toJobListCriteria(JobListCriteriaModel model) throws IllegalArgumentException {
        //TODO - dummy context - should be removed once implemented in GUI.
        ListFilter jobIdGreaterThanCondition = new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.JOB_ID, ListFilter.Op.GREATER_THAN, Long.valueOf(model.getJobId()).intValue());
        ListOrderBy descendingTimeOfCreation = new ListOrderBy<JobListCriteria.Field>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC);

        return new JobListCriteria().where(jobIdGreaterThanCondition).orderBy(descendingTimeOfCreation);
    }


}
