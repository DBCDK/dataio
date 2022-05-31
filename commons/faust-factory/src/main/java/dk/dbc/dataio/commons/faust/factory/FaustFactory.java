package dk.dbc.dataio.commons.faust.factory;

import dk.dbc.opennumberroll.OpennumberRollConnector;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;

public class FaustFactory {
    private final OpennumberRollConnector openNumberRollConnector;
    private final OpennumberRollConnector.Params openNumberRollConnectorParams;

    public FaustFactory(OpennumberRollConnector openNumberRollConnector, String numberRoll) {
        this.openNumberRollConnector = openNumberRollConnector;
        this.openNumberRollConnectorParams = new OpennumberRollConnector.Params()
                .withRollName(numberRoll);
    }

    public String newFaust() throws IllegalStateException {
        try {
            return openNumberRollConnector.getId(openNumberRollConnectorParams);
        } catch (OpennumberRollConnectorException e) {
            throw new IllegalStateException("Unable to obtain new faust from number roll " +
                    openNumberRollConnectorParams.getRollName(), e);
        }
    }
}
