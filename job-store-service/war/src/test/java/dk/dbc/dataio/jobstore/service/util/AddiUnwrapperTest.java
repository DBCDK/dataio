package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AddiUnwrapperTest {
    private final AddiUnwrapper unwrapper = new AddiUnwrapper();

    @Test
    public void chunkItemWithKnownType() {
        final ChunkItem chunkItem = new ChunkItemBuilder()
                .setType(ChunkItem.Type.MARCXCHANGE)
                .build();
        try {
            unwrapper.unwrap(chunkItem);
            Assertions.fail("No JobStoreException thrown");
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
            Assertions.fail("No JobStoreException thrown");
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
