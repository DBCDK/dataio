package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.service.AbstractJobStoreIT;

public class PgJobStoreRepositoryAbstractIT extends AbstractJobStoreIT {

    protected PgJobStoreRepository pgJobStoreRepository;


    @org.junit.Before
    public void initializePgJobStoreRepository() {
        pgJobStoreRepository = newPgJobStoreRepository();
    }
}
