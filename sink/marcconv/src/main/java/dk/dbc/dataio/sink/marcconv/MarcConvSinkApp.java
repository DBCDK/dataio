package dk.dbc.dataio.sink.marcconv;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.app.MessageConsumerApp;
import dk.dbc.dataio.jse.artemis.common.db.JPAHelper;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.marcconv.jms.MessageConsumer;
import jakarta.ws.rs.client.ClientBuilder;

import java.util.function.Supplier;

public class MarcConvSinkApp extends MessageConsumerApp {
    private static final ServiceHub serviceHub = ServiceHub.defaultHub();
    private static final FileStoreServiceConnector fileStore = new FileStoreServiceConnector(ClientBuilder.newClient(), SinkConfig.FILESTORE_URL.asString());
    private static final Supplier<MessageConsumer> messageConsumer = () -> new MessageConsumer(serviceHub, fileStore,
            JPAHelper.makeEntityManager("marcconv_PU", SinkConfig.MARCCONV_DB_URL));

    public MarcConvSinkApp() {
        JPAHelper.migrate(SinkConfig.MARCCONV_DB_URL);
    }

    public static void main(String[] args) {
        new MarcConvSinkApp().go(serviceHub, messageConsumer);
    }
}
