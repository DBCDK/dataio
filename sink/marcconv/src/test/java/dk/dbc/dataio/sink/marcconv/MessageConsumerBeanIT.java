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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MessageConsumerBeanIT extends IntegrationTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final AddiRecord addiRecord1 = newAddiRecord(
            "test-record-1-danmarc2.marcxchange");
    private final byte[] isoRecord1 = ResourceReader.getResourceAsByteArray(
            ConversionISO2709Test.class, "test-record-1-danmarc2.iso");
    private final AddiRecord addiRecord2 = newAddiRecord(
            "test-record-2-danmarc2.marcxchange");
    private final byte[] isoRecord2 = ResourceReader.getResourceAsByteArray(
            ConversionISO2709Test.class, "test-record-2-danmarc2.iso");

    @Test
    public void handleChunk() {
        final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();

        final List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecord1.getBytes())
                        .build(),
                new ChunkItemBuilder().setId(4L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecord2.getBytes())
                        .build()
        );
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = env().getPersistenceContext().run(() ->
                messageConsumerBean.handleChunk(chunk));
        assertThat("number of chunk items", result.size(), is(5));
        assertThat("1st chunk item",
                result.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item",
                result.getItems().get(1).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd chunk item",
                result.getItems().get(2).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("4th chunk item",
                result.getItems().get(3).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th chunk item",
                result.getItems().get(4).getStatus(), is(ChunkItem.Status.SUCCESS));

        assertThat("2nd chunk item diagnostics",
                result.getItems().get(1).getDiagnostics(), is(notNullValue()));

        final ConversionBlock block = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(ConversionBlock.class,
                        new ConversionBlock.Key(chunk.getJobId(), chunk.getChunkId())));

        assertThat("block written", block, is(notNullValue()));

        final byte[] expectedBytes = new byte[isoRecord1.length + isoRecord2.length];
        System.arraycopy(isoRecord1, 0, expectedBytes, 0, isoRecord1.length);
        System.arraycopy(isoRecord2, 0, expectedBytes, isoRecord1.length, isoRecord2.length);
        assertThat("block bytes", block.getBytes(), is(expectedBytes));
    }

    @Test
    public void overwriteExistingBlock() {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(7L)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder()
                                .setId(0L)
                                .setStatus(ChunkItem.Status.SUCCESS)
                                .setData(addiRecord1.getBytes())
                                .build()
                ))
                .build();

        final ConversionBlock existingBlock = new ConversionBlock();
        existingBlock.setKey(new ConversionBlock.Key(chunk.getJobId(), chunk.getChunkId()));
        existingBlock.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(existingBlock);
        });

        final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();

        env().getPersistenceContext().run(() ->
                messageConsumerBean.handleChunk(chunk));

        final ConversionBlock updatedBlock = env().getPersistenceContext().run(() ->
                env().getEntityManager()
                        .find(ConversionBlock.class, existingBlock.getKey()));

        assertThat("block bytes", updatedBlock.getBytes(), is(isoRecord1));
    }

    private MessageConsumerBean newMessageConsumerBean() {
        final MessageConsumerBean messageConsumerBean = new MessageConsumerBean();
        messageConsumerBean.entityManager = env().getEntityManager();
        return messageConsumerBean;
    }

    private AddiRecord newAddiRecord(String resourceFile) {
        try {
            final ConversionParam conversionParam = new ConversionParam()
                    .withEncoding("danmarc2");
            final byte[] metadata = StringUtil.asBytes(
                    jsonbContext.marshall(conversionParam));
            final byte[] record = ResourceReader.getResourceAsByteArray(
                    ConversionISO2709Test.class, resourceFile);
            return new AddiRecord(metadata, record);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
