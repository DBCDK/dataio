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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class AddiUnwrapperTest {
    private final AddiUnwrapper unwrapper = new AddiUnwrapper();

    @Test
    public void chunkItemWithKnownType() {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.MARCXCHANGE)
                .build();
        try {
            unwrapper.unwrap(chunkItem);
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void chunkItemWithUnknownTypeAndAddiContent() throws JobStoreException {
        final String expectedData = "test";
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.UNKNOWN)
                .setData(getValidAddi(expectedData))
                .build();
        final List<ChunkItem> unwrappedChunkItems = unwrapper.unwrap(chunkItem);
        assertThat("Number of unwrapped items", unwrappedChunkItems.size(), is(1));
        final ChunkItem unwrappedChunkItem = unwrappedChunkItems.get(0);
        assertThat("Unwrapped item type", unwrappedChunkItem.getType(),
                is(Collections.singletonList(ChunkItem.Type.UNKNOWN)));
        assertThat("Unwrapped item data", StringUtil.asString(unwrappedChunkItem.getData()),
                is(expectedData));
    }

    @Test
    public void chunkItemWithUnknownTypeAndNonAddiContent() throws JobStoreException {
        final String expectedData = "test";
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.UNKNOWN)
                .setData(expectedData)  // NOT Addi
                .build();
        final List<ChunkItem> unwrappedChunkItems = unwrapper.unwrap(chunkItem);
        assertThat("Number of unwrapped items", unwrappedChunkItems.size(), is(1));
        assertThat(unwrappedChunkItems.get(0), is(chunkItem));
    }

    @Test
    public void chunkItemWithNonAddiTypes() {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(Arrays.asList(ChunkItem.Type.GENERICXML, ChunkItem.Type.MARCXCHANGE))
                .build();
        try {
            unwrapper.unwrap(chunkItem);
            fail("No JobStoreException thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void chunkItemWithWrappedType() throws JobStoreException {
        final String expectedData = "test";
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(Arrays.asList(ChunkItem.Type.ADDI, ChunkItem.Type.STRING))
                .setData(getValidAddi(expectedData))
                .build();
        final List<ChunkItem> unwrappedChunkItems = unwrapper.unwrap(chunkItem);
        assertThat("Number of unwrapped items", unwrappedChunkItems.size(),
                is(1));
        final ChunkItem unwrappedChunkItem = unwrappedChunkItems.get(0);
        assertThat("Unwrapped item type", unwrappedChunkItem.getType(),
                is(Collections.singletonList(ChunkItem.Type.STRING)));
        assertThat("Unwrapped item data", StringUtil.asString(unwrappedChunkItem.getData()),
                is(expectedData));
    }

    @Test
    public void ChunkItemWithWrappedTypeAndMultipleAddiRecords() throws JobStoreException {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(Arrays.asList(ChunkItem.Type.ADDI, ChunkItem.Type.STRING))
                .setData(getValidAddi("first", "second"))
                .build();
        final List<ChunkItem> unwrappedChunkItems = unwrapper.unwrap(chunkItem);
        assertThat("Number of unwrapped items", unwrappedChunkItems.size(),
                is(2));
        assertThat("First unwrapped item type", unwrappedChunkItems.get(0).getType(),
                is(Collections.singletonList(ChunkItem.Type.STRING)));
        assertThat("First unwrapped item data", StringUtil.asString(unwrappedChunkItems.get(0).getData()),
                is("first"));
        assertThat("Second unwrapped item type", unwrappedChunkItems.get(1).getType(),
                is(Collections.singletonList(ChunkItem.Type.STRING)));
        assertThat("Second unwrapped item data", StringUtil.asString(unwrappedChunkItems.get(1).getData()),
                is("second"));
    }

    public static byte[] getValidAddi(String... content) {
        final StringBuilder addi = new StringBuilder();
        for (String s : content) {
            addi.append(String.format("19\n<es:referencedata/>\n%d\n%s\n",
                    s.getBytes(StandardCharsets.UTF_8).length, s));
        }
        return addi.toString().getBytes(StandardCharsets.UTF_8);
    }
}