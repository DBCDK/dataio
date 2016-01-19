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

package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ChunkTest {
    private static final ChunkItem CHUNK_ITEM = new ChunkItem(0L, "data".getBytes(), ChunkItem.Status.SUCCESS);
    private Chunk chunk;
    private final JSONBContext jsonbContext = new JSONBContext();

    @Before
    public void newChunk() {
        chunk = new Chunk(1L, 1L, Chunk.Type.PARTITIONED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negativeJobId_throws() {
        new Chunk(-1L, 1L, Chunk.Type.PARTITIONED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negativeChunkId_throws() {
        new Chunk(1L, -1L, Chunk.Type.PARTITIONED);
    }

    @Test
    public void constructor_lowestLegalArguments_success() {
        Chunk chunk = new Chunk(0L, 0L, Chunk.Type.PARTITIONED);
        assertThat("job id", chunk.getJobId(), is(0L));
        assertThat("chunk id", chunk.getChunkId(), is(0L));
        assertThat("type", chunk.getType(), is(Chunk.Type.PARTITIONED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_itemArgIsNull_throws() {
        chunk.insertItem(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_outOfOrderItemId_throws() {
        chunk.insertItem(new ChunkItem(1L, "data".getBytes(), ChunkItem.Status.IGNORE));
    }

    @Test
    public void insertItem_itemArgIsValid_addsItemToChunk() {
        chunk.insertItem(CHUNK_ITEM);
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_2arg_currentItemArgIsNull_throws() {
        chunk.insertItem(ChunkItem.UNDEFINED, CHUNK_ITEM);
    }

    @Test
    public void insertItem_2arg_nextItemArgIsNull_addsItemToChunk() {
        chunk.insertItem(CHUNK_ITEM, ChunkItem.UNDEFINED);
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test
    public void insertItem_2arg_nextItemArgIsNonNull_addsItemsToChunk() {
        chunk.insertItem(CHUNK_ITEM, CHUNK_ITEM);
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertItem_2arg_nextItemIdDiffersFromCurrentItemId_throws() {
        final ChunkItem nextChunkItem = new ChunkItem(1L, "data".getBytes(), ChunkItem.Status.SUCCESS);
        chunk.insertItem(CHUNK_ITEM, nextChunkItem);
    }

    @Test
    public void addAllItems_addsItemsToChunk() {
        chunk.addAllItems(Collections.singletonList(CHUNK_ITEM));
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test
    public void addAllItems_2arg_addsItemsToChunk() {
        chunk.addAllItems(Collections.singletonList(CHUNK_ITEM), Collections.singletonList(CHUNK_ITEM));
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(true));
    }

    @Test
    public void addAllItems_2arg_nextArgIsNull_addsItemsToChunk() {
        chunk.addAllItems(Collections.singletonList(CHUNK_ITEM), null);
        assertThat("chunk size", chunk.size(), is(1));
        assertThat("is chunk empty?", chunk.isEmpty(), is(false));
        assertThat("chunk has next items?", chunk.hasNextItems(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addAllItems_2arg_listsDifferInSize_throws() {
        final ChunkItem firstItem = new ChunkItem(0L, "First".getBytes(), ChunkItem.Status.IGNORE);
        final ChunkItem secondItem = new ChunkItem(1L, "Second".getBytes(), ChunkItem.Status.SUCCESS);
        chunk.addAllItems(Arrays.asList(firstItem, secondItem), Collections.singletonList(firstItem));
    }

    @Test
    public void chunk_iterator() {
        final ChunkItem firstItem = new ChunkItem(0L, "First".getBytes(), ChunkItem.Status.IGNORE);
        final ChunkItem secondItem = new ChunkItem(1L, "Second".getBytes(), ChunkItem.Status.SUCCESS);
        final ChunkItem thirdItem = new ChunkItem(2L, "Third".getBytes(), ChunkItem.Status.FAILURE);
        chunk.insertItem(firstItem);
        chunk.insertItem(secondItem);
        chunk.insertItem(thirdItem);

        assertThat("chunk size", chunk.size(), is(3));

        final Iterator<ChunkItem> it = chunk.iterator();
        assertThat("chunk has first item", it.hasNext(), is(true));
        assertThat("first item", it.next(), is(firstItem));
        assertThat("chunk has second item", it.hasNext(), is(true));
        assertThat("second item", it.next(), is(secondItem));
        assertThat("chunk has third item", it.hasNext(), is(true));
        assertThat("third item", it.next(), is(thirdItem));
        assertThat("chunk has fourth item", it.hasNext(), is(false));
    }

    @Test
    public void convertToJsonAndBackAgain() throws JSONBException {
        final ChunkItem firstItem = new ChunkItem(0L, "First".getBytes(), ChunkItem.Status.IGNORE );
        final ChunkItem secondItem = new ChunkItem(1L, "Second".getBytes(), ChunkItem.Status.SUCCESS);
        final ChunkItem thirdItem = new ChunkItem(2L, "Third".getBytes(), ChunkItem.Status.FAILURE);
        chunk.insertItem(firstItem);
        chunk.insertItem(secondItem);
        chunk.insertItem(thirdItem);

        final Chunk unmarshalledChunk = jsonbContext.unmarshall(jsonbContext.marshall(chunk), Chunk.class);
        final Iterator<ChunkItem> it = unmarshalledChunk.iterator();
        assertThat("chunk has first item", it.hasNext(), is(true));
        assertThat("first item", it.next(), is(firstItem));
        assertThat("chunk has second item", it.hasNext(), is(true));
        assertThat("second item", it.next(), is(secondItem));
        assertThat("chunk has third item", it.hasNext(), is(true));
        assertThat("third item", it.next(), is(thirdItem));
        assertThat("chunk has fourth item", it.hasNext(), is(false));
    }

    @Test(expected = JSONBException.class)
    public void unmarshallFromJsonWhichDoNotUpholdInvariant() throws JSONBException {
        final String illegalJson = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":1,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\"},{\"id\":0,\"data\":\"Second\",\"status\":\"SUCCESS\"}]}";
        jsonbContext.unmarshall(illegalJson, Chunk.class);
    }

    @Test
    public void unmarshallFromJsonWithoutNext() throws JSONBException {
        final String json = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":0,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\", \"type\":[\"UNKNOWN\"],\"encoding\":\"UTF-8\"}]}";
        final Chunk chunk = jsonbContext.unmarshall(json, Chunk.class);
        assertThat(chunk, is(notNullValue()));
    }

    @Test
    public void addItemWithStatusSuccess() throws JSONBException {

        // Preconditions
        final String json = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":0,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\",\"type\":[\"UNKNOWN\"],\"encoding\":\"UTF-8\"}]}";
        Chunk chunk = jsonbContext.unmarshall(json, Chunk.class);
        assertThat(chunk, is(notNullValue()));
        assertTrue(chunk.size() == 1);

        // Subject Under Test
        chunk.addItemWithStatusSuccess(1l, new byte[0]);
        assertTrue(chunk.size() == 2);
        assertStatus(chunk, 1l, ChunkItem.Status.SUCCESS);
    }

    @Test
    public void addItemWithStatusIgnored() throws JSONBException {

        // Preconditions
        final String json = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":0,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\",\"type\":[\"UNKNOWN\"],\"encoding\":\"UTF-8\"}]}";
        Chunk chunk = jsonbContext.unmarshall(json, Chunk.class);
        assertThat(chunk, is(notNullValue()));
        assertTrue(chunk.size() == 1);

        // Subject Under Test
        chunk.addItemWithStatusIgnored(1l, new byte[0]);
        assertTrue(chunk.size() == 2);
        assertStatus(chunk, 1l, ChunkItem.Status.IGNORE);
    }

    @Test
    public void addItemWithStatusFailed() throws JSONBException {

        // Preconditions
        final String json = "{\"jobId\":1,\"chunkId\":1,\"type\":\"PROCESSED\",\"items\":[{\"id\":0,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\",\"type\":[\"UNKNOWN\"],\"encoding\":\"UTF-8\"}]}";
        Chunk chunk = jsonbContext.unmarshall(json, Chunk.class);
        assertThat(chunk, is(notNullValue()));
        assertTrue(chunk.size() == 1);

        // Subject Under Test
        chunk.addItemWithStatusFailed(1l, new byte[0]);
        assertTrue(chunk.size() == 2);
        assertStatus(chunk, 1l, ChunkItem.Status.FAILURE);
    }

    private void assertStatus(Chunk chunk, long itemIdToStatusMatch, ChunkItem.Status expectedStatus) {
        final ChunkItem ITEM_NOT_FOUND = null;
        ChunkItem itemToMatch = ITEM_NOT_FOUND;
        for (ChunkItem item : chunk) {
            if(item.getId() == itemIdToStatusMatch) {
                itemToMatch = item;
            }
        }

        if(itemToMatch == null) {
            fail("Matching ChunkItem expected with itemId: " + itemIdToStatusMatch);
        } else {
            assertTrue(itemToMatch.getId() == itemIdToStatusMatch);
            assertEquals(expectedStatus, itemToMatch.getStatus());
        }
    }
}
