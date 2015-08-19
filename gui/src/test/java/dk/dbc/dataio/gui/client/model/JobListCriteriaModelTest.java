package dk.dbc.dataio.gui.client.model;

import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


public class JobListCriteriaModelTest {

    private static final String TEST = JobListCriteriaModel.JobType.TEST.name();
    private static final String TRANSIENT = JobListCriteriaModel.JobType.TRANSIENT.name();
    private static final String PERSISTENT = JobListCriteriaModel.JobType.PERSISTENT.name();
    private static final String ACCTEST = JobListCriteriaModel.JobType.ACCTEST.name();

    @Test
    public void createModel_noArgs_returnsNewInstanceWithDefaultValues() {
        JobListCriteriaModel model = new JobListCriteriaModel();
        assertThat(model, is(notNullValue()));
        assertThat(model.getId(), is(0L));
        assertThat(model.getVersion(), is(0L));
        assertThat(model.getSearchType(), is(JobListCriteriaModel.JobSearchType.PROCESSING_FAILED));
        assertThat(model.getSinkId(), is("0"));
        assertThat(model.getSubmitter(), is(nullValue()));
        assertThat(model.getJobTypes().size(), is(4));
        assertThat(model.getJobTypes().contains(TEST), is(true));
        assertThat(model.getJobTypes().contains(TRANSIENT), is(true));
        assertThat(model.getJobTypes().contains(PERSISTENT), is(true));
        assertThat(model.getJobTypes().contains(ACCTEST), is(true));
    }

    @Test
    public void and_mergeNullModel_sameModelAsBeforeMerge() {
        // Test Preparation
        JobListCriteriaModel model = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", true, false, true, false);
        JobListCriteriaModel savedModel = cloneJobListCriteriaModel(model);

        // Test Subject Under Test
        model.and(null);

        // Verify Test
        assertThat(equals(model, savedModel), is(true));
    }

    @Test
    public void and_mergeIdenticalModels_sameModelAsBeforeMerge() {
        // Test Preparation
        JobListCriteriaModel model    = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", true, false, true, false);
        JobListCriteriaModel newModel = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", true, false, true, false);

        // Test Subject Under Test
        model.and(newModel);

        // Verify Test
        assertThat(equals(model, JobListCriteriaModel.JobSearchType.ALL, "1321", true, false, true, false), is(true));
    }

    @Test
    public void and_mergeModelsDifferentJobSearchTypes_newVaueOverrules() {
        // Test Preparation
        JobListCriteriaModel model    = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL,               "1321", true, false, true, false);
        JobListCriteriaModel newModel = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.DELIVERING_FAILED, "1321", true, false, true, false);

        // Test Subject Under Test
        model.and(newModel);

        // Verify Test
        assertThat(equals(model, JobListCriteriaModel.JobSearchType.DELIVERING_FAILED, "1321", true, false, true, false), is(true));
    }

    @Test
    public void and_mergeModelsDifferentSinkIds_newVaueOverrules() {
        // Test Preparation
        JobListCriteriaModel model    = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", true, false, true, false);
        JobListCriteriaModel newModel = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1",    true, false, true, false);

        // Test Subject Under Test
        model.and(newModel);

        // Verify Test
        assertThat(equals(model, JobListCriteriaModel.JobSearchType.ALL, "1", true, false, true, false), is(true));
    }


    @Test
    public void and_mergeModelsNonOverlapingJobTypes_noJobTypes() {
        // Test Preparation
        JobListCriteriaModel model    = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", true,  false, false, false);
        JobListCriteriaModel newModel = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", false, true,  false, false);

        // Test Subject Under Test
        model.and(newModel);

        // Verify Test
        assertThat(equals(model, JobListCriteriaModel.JobSearchType.ALL, "1", false, false, false, false), is(false));
    }

    @Test
    public void and_mergeModelsOverlapingJobTypes_onlyOverlappingJobTypes() {
        // Test Preparation
        JobListCriteriaModel model    = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", true, false, true, false);
        JobListCriteriaModel newModel = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", false, true,  true, false);

        // Test Subject Under Test
        model.and(newModel);

        // Verify Test
        assertThat(equals(model, JobListCriteriaModel.JobSearchType.ALL, "1", false, false, true, false), is(false));
    }


    @Test
    public void add_mergeModelsDifferentSubmitter_newValueOverrules() throws Exception {
        JobListCriteriaModel model    = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", "42", true, false, true, false);
        JobListCriteriaModel newModel = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", "870970", false, true,  true, false);

        // Test Subject Under Test
        model.and(newModel);

        // Verify Test
        assertThat(equals(model, JobListCriteriaModel.JobSearchType.ALL, "1", "870970", false, false, true, false), is(false));
    }

    @Test
    public void add_mergeModelsDifferentSubmitter_newValueOverrules_onlyIfSet() throws Exception {
        JobListCriteriaModel model    = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", "42", true, false, true, false);
        JobListCriteriaModel newModel = constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType.ALL, "1321", "870970", false, true,  true, false);

        // Test Subject Under Test
        model.and(newModel);

        // Verify Test
        assertThat(equals(model, JobListCriteriaModel.JobSearchType.ALL, "1", "870970", false, false, true, false), is(false));
    }


    /*
         * Private utility methods
         */
    private JobListCriteriaModel constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType jobSearchType, String sinkId,
                                                                   boolean test, boolean trans, boolean persistent, boolean  acctest) {
        return constructJobListCriteriaModel(jobSearchType, sinkId, null, test, trans, persistent, acctest);
    }

    private JobListCriteriaModel constructJobListCriteriaModel(JobListCriteriaModel.JobSearchType jobSearchType, String sinkId,
                                                               String submitter,
                                                               boolean test, boolean trans, boolean persistent, boolean  acctest) {
        JobListCriteriaModel model = new JobListCriteriaModel();
        model.setSearchType(jobSearchType);
        model.setSinkId(sinkId);
        model.setSubmitter(submitter);
        Set<String> types = model.getJobTypes();
        types.clear();
        if (test) {
            types.add(TEST);
        }
        if (trans) {
            types.add(TRANSIENT);
        }
        if (persistent) {
            types.add(PERSISTENT);
        }
        if (acctest) {
            types.add(ACCTEST);
        }
        return model;
    }

    private JobListCriteriaModel cloneJobListCriteriaModel(JobListCriteriaModel source) {
        Set<String> types = source.getJobTypes();
        return constructJobListCriteriaModel(source.getSearchType(), source.getSinkId(),
                types.contains(TEST), types.contains(TRANSIENT), types.contains(PERSISTENT), types.contains(ACCTEST));
    }



    private boolean equals(JobListCriteriaModel model,
                           JobListCriteriaModel.JobSearchType jobSearchType, String sinkId,
                           boolean test, boolean trans, boolean persistent, boolean  acctest) {
        return equals( model, jobSearchType, sinkId, null, test, trans, persistent, acctest );
    }

    private boolean equals(JobListCriteriaModel model,
                           JobListCriteriaModel.JobSearchType jobSearchType, String sinkId,
                           String submitter,
                           boolean test, boolean trans, boolean persistent, boolean  acctest) {
        if (model.getSearchType() != jobSearchType) {
            return false;
        }
        if (!model.getSinkId().equals(sinkId)) {
            return false;
        }
        if( model.getSubmitter() != null ) {
            if (!model.getSubmitter().equals(submitter)) {
                return false;
            }
        } else {
            if (submitter != null) {
                return false;
            }
        }
        Set<String> types1 = model.getJobTypes();
        if (types1.contains(TEST) != test) {
            return false;
        }
        if (types1.contains(TRANSIENT) != trans) {
            return false;
        }
        if (types1.contains(PERSISTENT) != persistent) {
            return false;
        }
        if (types1.contains(ACCTEST) != acctest) {
            return false;
        }
        return true;
    }

    private boolean equals(JobListCriteriaModel model1, JobListCriteriaModel model2) {
        Set<String> types = model2.getJobTypes();
        return equals(model1, model2.getSearchType(), model2.getSinkId(), model2.getSubmitter(), types.contains(TEST), types.contains(TRANSIENT), types.contains(PERSISTENT), types.contains(ACCTEST));
    }

}
