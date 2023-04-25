package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FtpFileModel;
import dk.dbc.dataio.gui.client.proxies.FtpProxy;
import dk.dbc.ftp.FtpClient;
import dk.dbc.ftp.FtpClientException;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public class FtpProxyImpl implements FtpProxy {
    Logger LOGGER = LoggerFactory.getLogger(FtpProxy.class.getName());
    private static final String FTP_USER = "anonymous";
    private static final String FTP_PASS = "dataio-gui";  // Any password will do
    static final String FTP_DATAIO_DIRECTORY = "datain";

    private static final Logger log = LoggerFactory.getLogger(FtpProxyImpl.class);

    private String ftpUrl = null;
    private FtpClient ftpClient = null;

    /**
     * Default Constructor
     *
     * @throws ProxyException The Proxy Exception
     */
    public FtpProxyImpl() throws ProxyException {
        this(new FtpClient());
    }

    /**
     * This version of the constructor is intended to be used for testing. The purpose being, that the FtpClient can be injected in the proxy.
     *
     * @param ftpClient The FTP Client to use
     * @throws ProxyException The Proxy Exception
     */
    public FtpProxyImpl(FtpClient ftpClient) throws ProxyException {
        final String callerMethodName = "FtpProxyImpl";
        this.ftpClient = ftpClient;
        try {
            String ftpUrl = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("FTP_URL");
            // There is a risk (for historical reasons), that the path is included here - therefore isolate the hostname...
            URL url = new URL(ftpUrl);
            this.ftpUrl = url.getHost();
        } catch (Exception exception) {
            handleException(exception, callerMethodName);
        }
    }

    @Override
    public void put(String fileName, String content) throws ProxyException {
        final String callerMethodName = "put";
        if (ftpUrl == null) {
            handleException(new NamingException("Null string found"), callerMethodName);
        }
        try {
            if (content != null &&
                    !(content.endsWith("\nslut") || content.endsWith("\nfinish"))) {
                content += "\nslut";
            }
            ftpClient.withHost(ftpUrl).withUsername(FTP_USER).withPassword(FTP_PASS);
            ftpClient.connect();
            ftpClient.cd(FTP_DATAIO_DIRECTORY);
            ftpClient.put(fileName, content);
        } catch (Exception exception) {
            handleException(exception, callerMethodName);
        } finally {
            ftpClient.close();
        }
    }

    @Override
    public List<FtpFileModel> ftpFiles() throws ProxyException{
        SimpleDateFormat format = new SimpleDateFormat("MMM dd HH:mm");
        if (ftpUrl == null) {
            handleException(new NamingException("No FTP hostname or credentials for it was found."), "list");
        }
        try {
            ftpClient.withHost(ftpUrl).withUsername(FTP_USER).withPassword(FTP_PASS);
            ftpClient.connect();
            ftpClient.cd(FTP_DATAIO_DIRECTORY);
            List<FTPFile>  files = ftpClient.ls();
            LOGGER.info("Files in ftp:{}", files);
            return files.stream().map(s -> new FtpFileModel()
                            .withFileDate(format.format(s.getTimestamp().getTime()))
                            .withName(s.getName())
                            .withFtpSize(String.valueOf(s.getSize())))
                    .collect(Collectors.toList());
        } catch (FtpClientException e) {
            handleException(e, "ftpFiles");
        } finally {
            ftpClient.close();
        }
        return List.of();
    }


    /*
     * Private methods
     */

    /**
     * Handle exceptions thrown by the FtpClient and the Jndi Service and wrap them in ProxyExceptions
     *
     * @param exception        generic exception which in turn can be both Checked and Unchecked
     * @param callerMethodName calling method name for logging
     * @throws ProxyException GUI exception
     */
    private void handleException(Exception exception, String callerMethodName) throws ProxyException {
        if (exception instanceof NamingException) {
            logAndThrowError(callerMethodName, "Naming Exception", ProxyError.NAMING_ERROR, exception);
        } else if (exception instanceof FtpClientException) {
            logAndThrowError(callerMethodName, "Ftp Client Exception", ProxyError.FTP_CONNECTION_ERROR, exception);
        } else {
            logAndThrowError(callerMethodName, "Unknown FtpProxy Exception", ProxyError.ERROR_UNKNOWN, exception);
        }
    }

    /**
     * @param caller        The name of the caller method
     * @param exceptionText The text for the exception
     * @param proxyError    The Proxy Error
     * @param exception     The caught exception
     * @throws ProxyException The new common exception
     */
    private void logAndThrowError(String caller, String exceptionText, ProxyError proxyError, Exception exception) throws ProxyException {
        log.error("FtpProxy: " + caller + " - " + exceptionText, exception);
        throw new ProxyException(proxyError, exception);
    }

}
