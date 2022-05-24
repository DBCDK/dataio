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

package dk.dbc.dataio.sink.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AbstractSinkMessageConsumerBeanTest {
    private static final String MESSAGE_ID = "id";
    private String PAYLOAD;
    private final Map<String, Object> headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
    private final JSONBContext jsonbContext = new JSONBContext();
    
    @Before
    public void setup() throws JSONBException {
        Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        PAYLOAD = jsonbContext.marshall(processedChunk);
    }

    @Test(expected = NullPointerException.class)
    public void unmarshallPayload_consumedMessageArgIsNull_throws() throws InvalidMessageException {
        getInitializedBean().unmarshallPayload(null);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgContainsUnexpectedPayloadType_throws() throws InvalidMessageException {
        final Map<String, Object> invalidPayloadProperty = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, "notExpectedPayloadType");
        final ConsumedMessage consumedMessage = new ConsumedMessage(
                MESSAGE_ID, invalidPayloadProperty, PAYLOAD);
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgPayloadCanNotBeUnmarshalled_throws() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, headers, "payload");
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void unmarshallPayload_consumedMessageArgPayloadIsEmptyProcessedChunk_throws() throws InvalidMessageException, JSONBException {
        Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setItems(Collections.<ChunkItem>emptyList()).build();
        final String emptyProcessedChunkJson = jsonbContext.marshall(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, headers, emptyProcessedChunkJson);
        getInitializedBean().unmarshallPayload(consumedMessage);
    }

    @Test
    public void unmarshallPayload_consumedMessageArgIsValid_returnsProcessedChunkInstance() throws InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, headers, PAYLOAD);
        final Chunk processedChunk = getInitializedBean().unmarshallPayload(consumedMessage);
        assertThat(processedChunk, is(notNullValue()));
        assertThat(processedChunk.getType(), is(Chunk.Type.PROCESSED));
    }

    private TestableMessageConsumerBean getInitializedBean() {
        final TestableMessageConsumerBean messageConsumerBean = new TestableMessageConsumerBean();
        messageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        return messageConsumerBean;
    }

    private static class TestableMessageConsumerBean extends AbstractSinkMessageConsumerBean {
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
        @Override
        public void handleConsumedMessage(ConsumedMessage consumedMessage) throws ServiceException, InvalidMessageException {
        }
    }
}
