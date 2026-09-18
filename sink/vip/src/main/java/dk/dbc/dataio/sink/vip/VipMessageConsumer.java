package dk.dbc.dataio.sink.vip;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Diagnostic;
import dk.dbc.dataio.commons.types.VipSinkConfig;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.jobstore.types.ItemDeliveryResult;
import dk.dbc.dataio.jse.artemis.common.jms.SinkMessageConsumerAdapter;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnector;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorException;
import dk.dbc.dataio.sink.vip.connector.VipCoreConnectorUnexpectedStatusCodeException;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VipMessageConsumer extends SinkMessageConsumerAdapter {
    private static final String QUEUE = SinkConfig.QUEUE.fqnAsQueue();
    private static final String ADDRESS = SinkConfig.QUEUE.fqnAsAddress();
    private final ConfigBean configBean;
    private VipSinkConfig config;
    private VipCoreConnector vipCoreConnector;
    private final JSONBContext jsonbContext = new JSONBContext();

    public VipMessageConsumer(ServiceHub serviceHub, ConfigBean configBean) {
        super(serviceHub);
        this.configBean = configBean;
    }

    /**
     * Creates a consumer already holding the connector to deliver through, for tests having no
     * VIP-CORE endpoint to build one against
     *
     * @param config sink config the given connector was built from, so that a message carrying the
     *               same config leaves the connector in place
     */
    VipMessageConsumer(ServiceHub serviceHub, ConfigBean configBean, VipSinkConfig config,
                       VipCoreConnector vipCoreConnector) {
        this(serviceHub, configBean);
        this.config = config;
        this.vipCoreConnector = vipCoreConnector;
    }

    /**
     * Uploads a successfully processed item's records to VIP-CORE, and passes any other item
     * through as ignored
     * <p>
     * An item the processor failed or ignored is reported as ignored rather than as delivered, so
     * that it counts towards the job's ignored items and advances no delivery watermark for a
     * record nothing was sent for.
     */
    @Override
    protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item) {
        VipCoreConnector connector = refreshState(configBean.getConfig(message));
        switch (item.getStatus()) {
            case FAILURE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        outcome(item)
                                .withStatus(ChunkItem.Status.IGNORE)
                                .withData("Failed by processor"));
            case IGNORE:
                return ItemDeliveryResult.of(ItemDeliveryResult.Status.IGNORED,
                        outcome(item)
                                .withStatus(ChunkItem.Status.IGNORE)
                                .withData("Ignored by processor"));
            default:
                return deliverToVipCore(connector, item);
        }
    }

    @Override
    public String getQueue() {
        return QUEUE;
    }

    @Override
    public String getAddress() {
        return ADDRESS;
    }

    /**
     * Uploads the records of one item, reporting an upload VIP-CORE did not accept as failed
     * <p>
     * A rejection is failed rather than retried: the connector exhausts a retry policy of its own
     * before reporting one, so having the JMS session roll back would retry a second time on top of
     * that. A failed item advances no delivery watermark, leaving a later version of the record
     * free to reach VIP-CORE.
     */
    private ItemDeliveryResult deliverToVipCore(VipCoreConnector connector, ChunkItem item) {
        try {
            vipLoad(connector, item);
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.DELIVERED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.SUCCESS)
                            .withData("Loaded"));
        } catch (Exception e) {
            String errorMessage = errorMessage(e);
            return ItemDeliveryResult.of(ItemDeliveryResult.Status.FAILED,
                    outcome(item)
                            .withStatus(ChunkItem.Status.FAILURE)
                            .withDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, errorMessage, e))
                            .withData(errorMessage));
        }
    }

    /**
     * Creates the delivering outcome item of a delivered item, without the status and data naming
     * what became of it
     */
    private ChunkItem outcome(ChunkItem item) {
        return new ChunkItem()
                .withId(item.getId())
                .withTrackingId(item.getTrackingId())
                .withType(ChunkItem.Type.STRING)
                .withEncoding(StandardCharsets.UTF_8);
    }

    /**
     * Names what went wrong, adding what VIP-CORE said to the status code it said it with
     */
    private String errorMessage(Exception e) {
        if (e instanceof VipCoreConnectorUnexpectedStatusCodeException) {
            Optional<VipCoreConnector.Error> error =
                    ((VipCoreConnectorUnexpectedStatusCodeException) e).getError();
            if (error.isPresent()) {
                return e.getMessage() + " - " + error.get();
            }
        }
        return e.getMessage();
    }

    private void vipLoad(VipCoreConnector connector, ChunkItem chunkItem)
            throws VipCoreConnectorException, IOException, JSONBException {
        AddiReader addiReader = new AddiReader(new ByteArrayInputStream(chunkItem.getData()));
        while (addiReader.hasNext()) {
            AddiRecord addiRecord = addiReader.next();
            AddiMetaData addiMetaData = jsonbContext.unmarshall(StringUtil.asString(addiRecord.getMetaData()), AddiMetaData.class);
            connector.vipload(addiMetaData.format(), StringUtil.asString(addiRecord.getContentData()));
        }
    }

    /**
     * Rebuilds the VIP-CORE connector when the sink config has changed, and hands back the one to
     * deliver through
     * <p>
     * The connector is returned rather than only stored, so that a delivery uses one connector for
     * the whole item. Several consumer threads share this consumer, and reading the field outside
     * this method would let one of them pick up a connector another thread is in the middle of
     * replacing and closing.
     */
    private synchronized VipCoreConnector refreshState(VipSinkConfig latestConfig) {
        if (!latestConfig.equals(config)) {
            config = latestConfig;
            vipCoreConnector = createVipCoreConnector(config);
        }
        return vipCoreConnector;
    }

    @SuppressWarnings("java:S2095")
    private VipCoreConnector createVipCoreConnector(VipSinkConfig config) {
        if (vipCoreConnector != null) {
            vipCoreConnector.close();
        }
        return new VipCoreConnector(ClientBuilder.newClient().register(new JacksonFeature()),
                UserAgent.forInternalRequests(), config.getEndpoint());
    }
}
