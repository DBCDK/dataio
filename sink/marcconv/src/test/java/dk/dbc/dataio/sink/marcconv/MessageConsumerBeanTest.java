/*
 * DataIO - Data IO
 *
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

package dk.dbc.dataio.sink.marcconv;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class MessageConsumerBeanTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final EntityManager entityManager = mock(EntityManager.class);
    private final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();

    @Test
    public void handleChunk() throws JSONBException {
        final ConversionParam conversionParam = new ConversionParam().withEncoding("danmarc2");
        final byte[] metadata = StringUtil.asBytes(jsonbContext.marshall(conversionParam));
        final byte[] record = ResourceReader.getResourceAsByteArray(ConversionISO2709Test.class,
                "test-record-1-danmarc2.marcxchange");
        final AddiRecord addiRecord = new AddiRecord(metadata, record);

        final List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecord.getBytes())
                        .build()
        );
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(chunkItems)
                .build();

        final Chunk result = messageConsumerBean.handleChunk(chunk);
        assertThat("number of chunk items", result.size(), is(4));
        assertThat("1st chunk item",
                result.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item",
                result.getItems().get(1).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd chunk item",
                result.getItems().get(2).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("4th chunk item",
                result.getItems().get(3).getStatus(), is(ChunkItem.Status.SUCCESS));

        assertThat("2nd chunk item diagnostics",
                result.getItems().get(1).getDiagnostics(), is(notNullValue()));
    }

    private MessageConsumerBean newMessageConsumerBean() {
        final MessageConsumerBean messageConsumerBean = new MessageConsumerBean();
        messageConsumerBean.entityManager = entityManager;
        return messageConsumerBean;
    }
}