/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FtpProxy;
import dk.dbc.ftp.FtpClient;
import dk.dbc.ftp.FtpClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;

public class FtpProxyImpl implements FtpProxy {
    private static final String FTP_USER = "anonymous";
    private static final String FTP_PASS = "dataio-gui";  // Any password will do
    static final String FTP_DATAIO_DIRECTORY = "datain";

    private static final Logger log = LoggerFactory.getLogger(FtpProxyImpl.class);

    private String ftpUrl = null;
    private FtpClient ftpClient = null;

    /**
     * Default Constructor
     * @throws ProxyException The Proxy Exception
     */
    public FtpProxyImpl() throws ProxyException {
        this(new FtpClient());
    }

    /**
     * This version of the constructor is intended to be used for testing. The purpose being, that the FtpClient can be injected in the proxy.
     * @param ftpClient The FTP Client to use
     * @throws ProxyException The Proxy Exception
     */
    public FtpProxyImpl(FtpClient ftpClient) throws ProxyException {
        final String callerMethodName = "FtpProxyImpl";
        this.ftpClient = ftpClient;
        try {
            String jndiFtpUrl = ServiceUtil.getStringValueFromSystemPropertyOrJndi(JndiConstants.URL_RESOURCE_GUI_FTP);
            // There is a risk (for historical reasons), that the path is included here - therefore isolate the hostname...
            String[] splittedFtpUrl = jndiFtpUrl.split("/", 2);
            ftpUrl = splittedFtpUrl[0];
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


    /*
     * Private methods
     */

    /**
     * Handle exceptions thrown by the FtpClient and the Jndi Service and wrap them in ProxyExceptions
     * @param exception generic exception which in turn can be both Checked and Unchecked
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
     *
     * @param caller The name of the caller method
     * @param exceptionText The text for the exception
     * @param proxyError The Proxy Error
     * @param exception The caught exception
     * @throws ProxyException The new common exception
     */
    private void logAndThrowError(String caller, String exceptionText, ProxyError proxyError, Exception exception) throws ProxyException {
        log.error("FtpProxy: " + caller + " - " + exceptionText, exception);
        throw new ProxyException(proxyError, exception);
    }

}
