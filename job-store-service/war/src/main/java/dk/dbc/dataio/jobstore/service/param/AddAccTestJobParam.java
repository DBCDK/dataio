package dk.dbc.dataio.jobstore.service.param;

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.RecordSplitter;
import dk.dbc.dataio.jobstore.types.AccTestJobInputStream;

/**
 * This class is a parameter abstraction for the PgJobStore.addAccTestJob() method.
 * <p>
 * Parameter initialization failures will result in fatal diagnostics being added
 * to the internal diagnostics list, and the corresponding parameter field being
 * given a null value.
 * </p>
 */
public class AddAccTestJobParam extends AddJobParam {

    public AddAccTestJobParam(AccTestJobInputStream accTestJobInputStream, FlowStoreServiceConnector flowStoreServiceConnector) throws NullPointerException {
        super(accTestJobInputStream, flowStoreServiceConnector);
    }

    @Override
    protected Flow lookupFlow() {
        return ((AccTestJobInputStream) jobInputStream).getFlow();
    }

    @Override
    protected RecordSplitter lookupTypeOfDataPartitioner() {
        return ((AccTestJobInputStream) jobInputStream).getTypeOfDataPartitioner();
    }
}
