package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.commons.types.FileStoreUrn;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;

import java.net.URISyntaxException;

/**
 * Created by ThomasBerg on 14/09/15.
 */
public abstract class ParamBaseTest {

    protected static final String ERROR_MESSAGE = "Error Message";
    protected static final String DATA_FILE_ID = "42";

    private static final FileStoreUrn FILE_STORE_URN;
    protected static final JobSpecification jobSpecification;

    static {
        try {
            FILE_STORE_URN = FileStoreUrn.create(DATA_FILE_ID);
            jobSpecification = new JobSpecificationBuilder().setDataFile(FILE_STORE_URN.toString()).build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
