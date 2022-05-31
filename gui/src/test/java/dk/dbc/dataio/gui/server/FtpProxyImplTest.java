package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.ftp.FtpClient;
import dk.dbc.ftp.FtpClientException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.io.InputStream;
import java.nio.file.Path;

import static dk.dbc.dataio.gui.server.FtpProxyImpl.FTP_DATAIO_DIRECTORY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FtpProxyImplTest {
    private static final String FTP_URL = "ftp://ftp-test.dbc.dk/testing";

    private static FtpClient mockedFtpClient = mock(FtpClient.class);

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setInitialContext() {
        // Sets default response for all FtpClient methods (needs to return the mocked client)
        when(mockedFtpClient.withHost(any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.withPort(any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.withUsername(any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.withPassword(any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.connect()).thenReturn(mockedFtpClient);
        when(mockedFtpClient.close()).thenReturn(mockedFtpClient);
        when(mockedFtpClient.cd(any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.put(any(), (String) any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.put(any(), (Path) any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.put(any())).thenReturn(mockedFtpClient);
        when(mockedFtpClient.put(any(), (InputStream) any())).thenReturn(mockedFtpClient);
    }

    @Test(expected = ProxyException.class)
    public void constructor_noFtpUrlSet_proxyException() throws ProxyException {
        // Test preparation
        InMemoryInitialContextFactory.clear();  // Causes ServiceUtil to throw a NamingException

        // Test subject under test
        new FtpProxyImpl(mockedFtpClient);
    }

    @Test
    public void constructor() throws ProxyException {
        environmentVariables.set("FTP_URL", FTP_URL);

        // Test subject under test
        new FtpProxyImpl(mockedFtpClient);

        // Test Verification
        verifyNoMoreInteractions(mockedFtpClient);
    }

    @Test(expected = ProxyException.class)
    public void put_ftpClientException_proxyException() throws ProxyException {
        // Test preparation
        FtpProxyImpl ftpProxy = new FtpProxyImpl(mockedFtpClient);
        when(mockedFtpClient.put(any(), (String) any())).thenThrow(new FtpClientException("Faked Ftp Put Error"));

        // Test subject under test
        ftpProxy.put("ftp-filename", "ftp-content");
    }

    @Test
    public void put() throws ProxyException {
        environmentVariables.set("FTP_URL", FTP_URL);

        // Test preparation
        FtpProxyImpl ftpProxy = new FtpProxyImpl(mockedFtpClient);

        // Test subject under test
        ftpProxy.put("ftp-filename", "ftp-content");

        // Test Verification
        verify(mockedFtpClient).withHost("ftp-test.dbc.dk");
        verify(mockedFtpClient).withUsername("anonymous");
        verify(mockedFtpClient).withPassword("dataio-gui");
        verify(mockedFtpClient).connect();
        verify(mockedFtpClient).cd(FTP_DATAIO_DIRECTORY);
        verify(mockedFtpClient).put("ftp-filename", "ftp-content\nslut");
        verify(mockedFtpClient).close();
        verifyNoMoreInteractions(mockedFtpClient);
    }
}
