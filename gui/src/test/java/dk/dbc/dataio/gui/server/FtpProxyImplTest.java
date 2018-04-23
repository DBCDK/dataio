/*
 * DataIO - Data IO
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.ftp.FtpClient;
import dk.dbc.ftp.FtpClientException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.naming.Context;
import java.io.InputStream;
import java.nio.file.Path;

import static dk.dbc.dataio.gui.server.FtpProxyImpl.FTP_DATAIO_DIRECTORY;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FtpProxyImplTest {

    private static final String FTP_NAME = "url/dataio/gui/ftp";
    private static final String FTP_VALUE = "ftp://ftp-test.dbc.dk/testing";

    @Mock private static FtpClient mockedFtpClient;

    @BeforeClass
    public static void setupTest() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setInitialContext() {
        // Bind known values
        InMemoryInitialContextFactory.bind(FTP_NAME, FTP_VALUE);
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

    @After
    public void clearInitialContext() {
        InMemoryInitialContextFactory.clear();
    }


    /*
     * Test constructor
     */

    @Test(expected = ProxyException.class)
    public void constructor_jndiException_proxyException() throws ProxyException {
        // Test preparation
        InMemoryInitialContextFactory.clear();  // Causes ServiceUtil to throw a NamingException

        // Test subject under test
        new FtpProxyImpl(mockedFtpClient);
    }

    @Test
    public void constructor_normalCase_ok() throws ProxyException {
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
    public void put_normalCase_noException_ok() throws ProxyException {
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
