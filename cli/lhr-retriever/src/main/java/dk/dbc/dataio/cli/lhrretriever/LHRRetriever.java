package dk.dbc.dataio.cli.lhrretriever;

import dk.dbc.dataio.cli.lhrretriever.arguments.ArgParseException;
import dk.dbc.dataio.cli.lhrretriever.arguments.Arguments;
import dk.dbc.dataio.cli.lhrretriever.config.ConfigJson;
import dk.dbc.dataio.cli.lhrretriever.config.ConfigParseException;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.utils.rawrepo.RawRepoConnector;
import dk.dbc.openagency.client.OpenAgencyServiceFromURL;
import dk.dbc.rawrepo.AgencySearchOrder;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.RelationHints;
import dk.dbc.rawrepo.RelationHintsOpenAgency;
import dk.dbc.rawrepo.showorder.AgencySearchOrderFromShowOrder;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import javax.ws.rs.client.Client;
import java.sql.SQLException;

public class LHRRetriever {
    private final DataSource dataSource;
    private final RawRepoConnector rawRepoConnector;
    private final Ocn2PidServiceConnector ocn2PidServiceConnector;
    private final FlowStoreServiceConnector flowStoreServiceConnector;

    public LHRRetriever(Arguments arguments) throws SQLException,
            RawRepoException, ConfigParseException {
        ConfigJson config = ConfigJson.parseConfig(arguments.configPath);
        dataSource = setupDataSource(config);
        final Client client = HttpClient.newClient();
        rawRepoConnector = setupRRConnector(config.getOpenAgencyTarget(),
            dataSource);
        ocn2PidServiceConnector = new Ocn2PidServiceConnector(
            client, config.getOcn2pidServiceTarget());
        flowStoreServiceConnector = new FlowStoreServiceConnector(client,
            config.getFlowStoreEndpoint());
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = Arguments.parseArgs(args);
            LHRRetriever lhrRetriever = new LHRRetriever(arguments);
        } catch(ArgParseException | SQLException | RawRepoException |
                ConfigParseException e) {
            System.err.println(String.format("unexpected error: %s",
                e.toString()));
            System.exit(1);
        }
    }

    /**
     * Sets up raw repo data source
     *
     * @param config parsed values from config file
     * @return raw repo data source
     */
    public DataSource setupDataSource(ConfigJson config) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setDatabaseName(config.getDbName());
        dataSource.setServerName(config.getDbHost());
        dataSource.setPortNumber(config.getDbPort());
        dataSource.setUser(config.getDbUser());
        dataSource.setPassword(config.getDbPassword());
        return dataSource;
    }

    /**
     * Sets up raw repo connector
     *
     * @param openAgencyTargetString url for open agency target
     * @param dataSource raw repo data source
     * @return raw repo connector
     */
    public RawRepoConnector setupRRConnector(String openAgencyTargetString,
            DataSource dataSource) {
        OpenAgencyTarget openAgencyTarget = new OpenAgencyTarget();
        openAgencyTarget.setUrl(openAgencyTargetString);
        OpenAgencyServiceFromURL openAgencyService = OpenAgencyServiceFromURL
            .builder().build(openAgencyTarget.getUrl());
        final AgencySearchOrder agencySearchOrder =
            new AgencySearchOrderFromShowOrder(openAgencyService);
        final RelationHints relationHints = new RelationHintsOpenAgency(
            openAgencyService);
        return new RawRepoConnector(dataSource, agencySearchOrder,
            relationHints);
    }
}
