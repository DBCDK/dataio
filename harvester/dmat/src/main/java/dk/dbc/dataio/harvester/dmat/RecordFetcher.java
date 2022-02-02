package dk.dbc.dataio.harvester.dmat;

import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.MarcExchangeCollection;
import dk.dbc.dmat.service.connector.JacksonConfig;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.persistence.enums.Selection;
import dk.dbc.dmat.service.persistence.enums.UpdateCode;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.rawrepo.record.RecordServiceConnector;
import dk.dbc.rawrepo.record.RecordServiceConnectorException;
import dk.dbc.rawrepo.record.RecordServiceConnectorFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import java.util.Arrays;


public class RecordFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordFetcher.class);

    public static byte[] getRecordCollectionFor(RecordServiceConnector recordServiceConnector, DMatRecord dMatRecord) throws RecordServiceConnectorException, HarvesterException {
        String recordId;

        // Todo: [begin] For use during initial implementation and tests, to be removed!
        final Client client = HttpClient.newClient(new ClientConfig()
                .register(new JacksonConfig())
                .register(new JacksonFeature()));
        RecordServiceConnector con = RecordServiceConnectorFactory.create("http://rawrepo-record-service.basismig.svc.cloud.dbc.dk");
        // Todo: [end]

        // Extract record id to fetch
        // - updateCode NEW and AUTO with selection CREATE and CLONE => matched record
        if( Arrays.asList(UpdateCode.NEW, UpdateCode.AUTO).contains(dMatRecord.getUpdateCode())
                && Arrays.asList(Selection.CREATE, Selection.CLONE).contains(dMatRecord.getSelection())) {
            recordId = dMatRecord.getMatch();
        } else {
            LOGGER.error("Attempt to fetch RR record for invalid combination of updateCode {} and selection {}",
                    dMatRecord.getUpdateCode(), dMatRecord.getSelection());
            throw new HarvesterException(
                    String.format("Attempt to fetch RR record for invalid combination of updateCode %s and selection %s",
                            dMatRecord.getUpdateCode(), dMatRecord.getSelection()));
        }

        // Fetch record and wrap in a collection (although there's always only one record)
        LOGGER.info("Fetch attached record {}:870970", recordId);
        MarcExchangeCollection collection = new MarcExchangeCollection();
        collection.addMember(
                recordServiceConnector.getRecordContent(191919, dMatRecord.getMatch(),
                        new RecordServiceConnector.Params()
                                .withMode(RecordServiceConnector.Params.Mode.EXPANDED)
                                .withKeepAutFields(true)
                                .withUseParentAgency(true)));
        return collection.asBytes();
    }
}
