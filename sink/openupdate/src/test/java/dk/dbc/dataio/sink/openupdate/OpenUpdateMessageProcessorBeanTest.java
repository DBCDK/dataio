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

package dk.dbc.dataio.sink.openupdate;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import dk.dbc.dataio.sink.openupdate.connector.OpenUpdateServiceConnector;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenUpdateMessageProcessorBeanTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final OpenUpdateMessageProcessorBean openUpdateMessageProcessorBean = new OpenUpdateMessageProcessorBean();
    private final OpenUpdateConfigBean openUpdateConfigBean = mock(OpenUpdateConfigBean.class);
    private final OpenUpdateServiceConnector openUpdateServiceConnector = mock(OpenUpdateServiceConnector.class);
    {
        openUpdateMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        openUpdateMessageProcessorBean.openUpdateConfigBean = openUpdateConfigBean;
    }

    @Before
    public void setupMocks() throws SinkException {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(openUpdateConfigBean.getConnector(any(ConsumedMessage.class))).thenReturn(openUpdateServiceConnector);
    }

    @Test
    public void handleConsumedMessage_jobStoreCommunicationFails_throws()
            throws InvalidMessageException, SinkException, JobStoreServiceConnectorException {
        final JobStoreServiceConnectorException jobStoreServiceConnectorException = new JobStoreServiceConnectorException("Exception from job-store");
        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong()))
                .thenThrow(jobStoreServiceConnectorException);

        // A single ignored chunk triggers job-store communication
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder()
                                .setStatus(ChunkItem.Status.IGNORE).build()))
                .build();

        try {
            openUpdateMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(chunk));
            fail("No SinkException thrown");
        } catch (SinkException e) {
            assertThat(e.getCause(), is(jobStoreServiceConnectorException));
        }
    }

    private ConsumedMessage getConsumedMessageForChunk(ExternalChunk chunk) {
        try {
            return new ConsumedMessage("42",
                    Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE),
                    jsonbContext.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}