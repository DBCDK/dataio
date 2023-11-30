package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.junit.jupiter.api.BeforeEach;

public abstract class ParamBaseTest {

    protected static final String ERROR_MESSAGE = "Error Message";
    protected static final String DATA_FILE_ID = "42";

    protected JobSpecification jobSpecification;

    @BeforeEach
    public void createJobSpecification() {
        jobSpecification = new JobSpecification().withPackaging("packaging")
                .withFormat("format")
                .withCharset("utf8")
                .withSubmitterId(123456)
                .withDataFile(FileStoreUrn.create(DATA_FILE_ID).toString())
                .withType(JobSpecification.Type.TEST);
    }
}
