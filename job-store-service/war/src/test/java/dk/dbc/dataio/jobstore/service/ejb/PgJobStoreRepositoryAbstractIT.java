package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;
import org.junit.Before;

public class PgJobStoreRepositoryAbstractIT extends AbstractJobStoreIT {

    protected PgJobStoreRepository pgJobStoreRepository;


    @Before
    public void initializePgJobStoreRepository() {
        pgJobStoreRepository = newPgJobStoreRepository();
    }
}
