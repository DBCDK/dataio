package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Main {
    public static void main(String[] args) throws IOException, FileStoreServiceConnectorException {
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        final Client client = HttpClient.newClient(config);

        final FileStoreServiceConnector fileStoreServiceConnector = new FileStoreServiceConnector(client, "http://jbn:8080/file-store-service");

        final String fileId = write(fileStoreServiceConnector);
        System.out.println(fileId);
        read(fileStoreServiceConnector, fileId);
    }

    private static String write(FileStoreServiceConnector fileStoreServiceConnector)
            throws IOException, FileStoreServiceConnectorException {
        //try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream("/home/jbn/downloads/kubuntu-14.04-desktop-amd64.iso"))) {
        try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream("/home/jbn/downloads/SoapUI-x32-5.0.0.sh"))) {
        //try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream("/home/jbn/downloads/test.file"))) {
            return fileStoreServiceConnector.addFile(bis);
        }
    }

    private static void read(FileStoreServiceConnector fileStoreServiceConnector, String fileId)
            throws IOException, FileStoreServiceConnectorException {
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/home/jbn/out.dat"));
             final InputStream data = fileStoreServiceConnector.getFile(fileId)) {
            final byte[] buf = new byte[8192];
            int bytesRead;
            while ((bytesRead = data.read(buf)) > 0) {
                bos.write(buf, 0, bytesRead);
            }
            bos.flush();
        }
    }
}
