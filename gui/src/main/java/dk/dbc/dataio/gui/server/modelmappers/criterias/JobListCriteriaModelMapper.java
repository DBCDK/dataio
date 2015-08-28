package dk.dbc.dataio.gui.server.modelmappers.criterias;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListOrderBy;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.LinkedList;

public final class JobListCriteriaModelMapper {

    /**
     * Private Constructor prevents instantiation of this static class
     */
    private JobListCriteriaModelMapper() {}

    public static JobListCriteria toJobListCriteria(JobListCriteriaModel model) {
        final JobListCriteria jobListCriteria = new JobListCriteria();

        if (model.getId() != 0) {
            // Where job ID equals...
            buildJobListCriteriaWithJobIdClause(jobListCriteria, model);
        }
        else {
            if (Long.valueOf(model.getSinkId()).intValue() != 0) {
                // Where Sink ID equals...
                buildJobListCriteriaWithSinkClause(jobListCriteria, model);
            }

            if( model.getSubmitter() != null ) {
                // Where Mode
                jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS,
                        String.format("{ \"submitterId\": %1$s}", model.getSubmitter())));
            }
            // And where search type equals...
            buildJobListCriteriaWithSearchType(jobListCriteria, model);

            // And where/or job type equals...
            buildJobListCriteriaWithJobTypeClauses(jobListCriteria, model);

            // Order by...
            orderByDescendingTimeOfCreation(jobListCriteria);
        }

        jobListCriteria.limit( model.getLimit() );
        jobListCriteria.offset( model.getOffset() );
        return jobListCriteria;
    }

    /**
     * Builds and adds a where clause uniquely identifying a sink
     * @param jobListCriteria the job list criteria to modify
     * @param model the job list criteria model
     */
    private static void buildJobListCriteriaWithSinkClause(JobListCriteria jobListCriteria, JobListCriteriaModel model) {
        jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SINK_ID, ListFilter.Op.EQUAL, Long.valueOf(model.getSinkId()).intValue()));
    }

    /**
     * Builds and adds a where clause uniquely identifying a job
     * @param jobListCriteria the job list criteria to modify
     * @param model the job list criteria model
     */
    private static void buildJobListCriteriaWithJobIdClause(JobListCriteria jobListCriteria, JobListCriteriaModel model) {
        jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, model.getId()));
    }

    /**
     * Builds and adds a where clause for job type. If several job types are listed, all but one are added as or clauses
     * @param jobListCriteria the job list criteria to modify
     * @param model the job list criteria model
     */
    private static void buildJobListCriteriaWithJobTypeClauses(JobListCriteria jobListCriteria, JobListCriteriaModel model) {
        LinkedList<JobSpecification.Type> jobTypeList = new LinkedList<JobSpecification.Type>();
        for (String jobTypeString : model.getJobTypes()) {
            jobTypeList.add(JobSpecification.Type.valueOf(jobTypeString));
        }
        // Where
        jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, buildJsonString(jobTypeList.removeFirst().name())));

        // Or
        for(JobSpecification.Type type : jobTypeList) {
            jobListCriteria.or(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, buildJsonString(type.name())));
        }
    }

    /**
     * Builds and adds a new ListFilter for jobs that are failed in processing or delivering, based on the search type of the model
     * @param jobListCriteria the job list criteria to modify
     */
    private static void buildJobListCriteriaWithSearchType(JobListCriteria jobListCriteria, JobListCriteriaModel model) {
        switch (model.getSearchType()) {
            case PROCESSING_FAILED:
                jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.STATE_PROCESSING_FAILED));
                break;
            case DELIVERING_FAILED:
                jobListCriteria.where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.STATE_DELIVERING_FAILED));
                break;
            //TODO - failedInJobCreation
        }
    }

    /**
     * Builds and adds a new ListOrdeBy with sorting is set to descending time of creation
     * @param jobListCriteria the job list criteria to modify
     */
    private static void orderByDescendingTimeOfCreation(JobListCriteria jobListCriteria) {
        ListOrderBy descendingTimeOfCreation = new ListOrderBy<JobListCriteria.Field>(JobListCriteria.Field.TIME_OF_CREATION, ListOrderBy.Sort.DESC);
        jobListCriteria.orderBy(descendingTimeOfCreation);
    }

    /**
     * Builds a json String
     * @param jobType the job Type
     * @return json as String
     */
    private static String buildJsonString(String jobType) {
        final JsonObject jsonObject =  Json.createObjectBuilder()
                .add("type", jobType)
                .build();
        return jsonObject.toString();
    }

}

