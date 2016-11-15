package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import org.junit.Before;

public abstract class ParamBaseTest {

    protected static final String ERROR_MESSAGE = "Error Message";
    protected static final String DATA_FILE_ID = "42";

    protected JobSpecificationBuilder jobSpecificationBuilder;
    protected JobSpecification jobSpecification;

    @Before
    public void createJobSpecification() {
        jobSpecificationBuilder = new JobSpecificationBuilder()
                .setDataFile(FileStoreUrn.create(DATA_FILE_ID).toString());
        jobSpecification = jobSpecificationBuilder.build();
    }
}
