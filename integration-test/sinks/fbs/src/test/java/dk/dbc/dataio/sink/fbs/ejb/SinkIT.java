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

package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.xml.ws.WebServiceException;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SinkIT {
    private final Chunk testProcessedChunk = getProcessedChunk();

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @AfterClass
    public static void teardown() {
        InMemoryInitialContextFactory.clear();
    }

    /**
     * Given: a processed Chunk with two items, the first one valid and the second one invalid <br/>
     * When: pushing processed Chunk to FBS web-service endpoint <br/>
     * Then: a delivered Chunk with two items is returned <br/>
     * And: the first item has status SUCCESS <br/>
     * And: the second item has status FAILURE <br/>
     */
    @Ignore("Ignored since unresponsive host - jda - 2014.01.27")
    @Test
    public void fbsPusherBean_endpointResponds() throws NamingException {
        // When...
        InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_FBS_WS, System.getProperty("fbs.update.ws.endpoint"));
        final FbsPusherBean fbsPusherBean = getFbsPusherBean();
        final Chunk deliveredChunk = fbsPusherBean.push(testProcessedChunk);

        // Then...
        assertThat(deliveredChunk.size(), is(2));
        // And...
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.SUCCESS));
        // And...
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(ChunkItem.Status.FAILURE));
    }

    @Test(expected = WebServiceException.class)
    public void fbsPusherBean_endpointCommunicationThrowsbWebServiceException_throws() throws NamingException {
        // When...
        InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_FBS_WS, "http://nosuchhost.dbc.dk/test");
        final FbsPusherBean fbsPusherBean = getFbsPusherBean();
        fbsPusherBean.push(testProcessedChunk);
    }

    private FbsPusherBean getFbsPusherBean() throws NamingException {
        final FbsPusherBean fbsPusherBean = new FbsPusherBean();
        fbsPusherBean.fbsUpdateConnector = getFbsUpdateConnectorBean();
        return fbsPusherBean;
    }

    private FbsUpdateConnectorBean getFbsUpdateConnectorBean() throws NamingException {
        final FbsUpdateConnectorBean fbsUpdateConnectorBean = new FbsUpdateConnectorBean();
        fbsUpdateConnectorBean.initializeConnector();
        return fbsUpdateConnectorBean;
    }

    private Chunk getProcessedChunk() {
        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(Collections.<ChunkItem>emptyList()).build();
        processedChunk.insertItem(new ChunkItemBuilder().setId(0).setData(StringUtil.asBytes(
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                        "<marcx:record format=\"danMARC2\">" +
                        "<marcx:leader>00000n    2200000   4500</marcx:leader>" +
                        "<marcx:datafield tag=\"001\" ind1=\"0\" ind2=\"0\">" +
                        "<marcx:subfield code=\"a\">4 539 593 6</marcx:subfield>" +
                        "<marcx:subfield code=\"b\">870970</marcx:subfield>" +
                        "<marcx:subfield code=\"c\">20131114205943</marcx:subfield>" +
                        "<marcx:subfield code=\"d\">20131114</marcx:subfield>" +
                        "<marcx:subfield code=\"f\">a</marcx:subfield>" +
                        "</marcx:datafield>" +
                        "<marcx:datafield tag=\"245\" ind1=\"0\" ind2=\"0\">" +
                        "<marcx:subfield code=\"a\">&#xC0; la recherche du temps perdu</marcx:subfield>" +
                        "</marcx:datafield>" +
                        "</marcx:record>" +
                        "</marcx:collection>"
        )).build());
        processedChunk.insertItem(new ChunkItemBuilder().setId(1).setData(StringUtil.asBytes(
                "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
                        "<marcx:record format=\"danMARC2\">" +
                        "</marcx:record>" +
                        "</marcx:collection>"
        )).build());
        return processedChunk;
    }
}
