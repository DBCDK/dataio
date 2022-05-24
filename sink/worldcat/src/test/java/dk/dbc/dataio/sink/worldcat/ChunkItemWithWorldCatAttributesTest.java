package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static dk.dbc.dataio.commons.types.ChunkItem.successfulChunkItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ChunkItemWithWorldCatAttributesTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final WorldCatAttributes attributes = new WorldCatAttributes().withOcn("ocn42");
    private final AddiRecord addiRecord = new AddiRecord(marshallToBytes(attributes), "data".getBytes());

    @Test
    public void of_invalidAddi_throws() {
        assertThat(() -> ChunkItemWithWorldCatAttributes.of(successfulChunkItem().withData("not ADDI")),
                isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void of_multipleAddi_throws() throws JSONBException, IOException {
        final ChunkItem chunkItem = successfulChunkItem()
                .withData(marshallAddiRecords(addiRecord, addiRecord))
                .withType(ChunkItem.Type.ADDI);
        assertThat(() -> ChunkItemWithWorldCatAttributes.of(chunkItem), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void of_chunkItemWithoutType_throws() throws IOException {
        final ChunkItem chunkItem = successfulChunkItem()
                .withData(addiRecord.getBytes());
        assertThat(() -> ChunkItemWithWorldCatAttributes.of(chunkItem), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void of_addi() throws IOException {
        final ChunkItem chunkItem = successfulChunkItem()
                .withData(addiRecord.getBytes())
                .withType(ChunkItem.Type.ADDI);
        final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                ChunkItemWithWorldCatAttributes.of(chunkItem);
        assertThat("WorldCat attributes", chunkItemWithWorldCatAttributes.getWorldCatAttributes(), is(attributes));
        assertThat("Chunk item data", chunkItemWithWorldCatAttributes.getData(), is(addiRecord.getContentData()));
    }

    @Test
    public void getActiveHoldingSymbols() {
        final WorldCatAttributes attributes = new WorldCatAttributes().withHoldings(Arrays.asList(
                new Holding().withSymbol("ABC").withAction(Holding.Action.INSERT),
                new Holding().withSymbol("DEF").withAction(Holding.Action.DELETE),
                new Holding().withSymbol("GHI").withAction(Holding.Action.DELETE),
                new Holding().withSymbol("JKL").withAction(Holding.Action.INSERT)
        ));
        final ChunkItem chunkItem = successfulChunkItem()
                .withData( new AddiRecord(marshallToBytes(attributes), "data".getBytes()).getBytes())
                .withType(ChunkItem.Type.ADDI);
        final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                ChunkItemWithWorldCatAttributes.of(chunkItem);
        assertThat(chunkItemWithWorldCatAttributes.getActiveHoldingSymbols(), is(Arrays.asList("ABC", "JKL")));
    }

    @Test
    public void getActiveHoldingSymbols_whenWorldCatAttributesHoldingsListIsNull() {
        final ChunkItem chunkItem = successfulChunkItem()
                .withData( new AddiRecord(marshallToBytes(new WorldCatAttributes()), "data".getBytes()).getBytes())
                .withType(ChunkItem.Type.ADDI);
        final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                ChunkItemWithWorldCatAttributes.of(chunkItem);
        assertThat(chunkItemWithWorldCatAttributes.getActiveHoldingSymbols(), is(Collections.emptyList()));
    }

    @Test
    public void addDiscontinuedHoldings_whenFormerActiveHoldingSymbolsIsNull_holdingsAreLeftUnchanged() {
        final Holding originalHolding = new Holding().withSymbol("ABC").withAction(Holding.Action.INSERT);
        final WorldCatAttributes attributes = new WorldCatAttributes()
                .withHoldings(Collections.singletonList(originalHolding));
        final ChunkItem chunkItem = successfulChunkItem()
                .withData( new AddiRecord(marshallToBytes(attributes), "data".getBytes()).getBytes())
                .withType(ChunkItem.Type.ADDI);
        final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                ChunkItemWithWorldCatAttributes.of(chunkItem);
        chunkItemWithWorldCatAttributes.addDiscontinuedHoldings(null);

        assertThat(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings(),
                is(Collections.singletonList(originalHolding)));
    }

    @Test
    public void addDiscontinuedHoldings_whenFormerActiveHoldingSymbolsIsEmpty_holdingsAreLeftUnchanged() {
        final Holding originalHolding = new Holding().withSymbol("ABC").withAction(Holding.Action.INSERT);
        final WorldCatAttributes attributes = new WorldCatAttributes()
                .withHoldings(Collections.singletonList(originalHolding));
        final ChunkItem chunkItem = successfulChunkItem()
                .withData( new AddiRecord(marshallToBytes(attributes), "data".getBytes()).getBytes())
                .withType(ChunkItem.Type.ADDI);
        final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                ChunkItemWithWorldCatAttributes.of(chunkItem);
        chunkItemWithWorldCatAttributes.addDiscontinuedHoldings(Collections.emptyList());

        assertThat(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings(),
                is(Collections.singletonList(originalHolding)));
    }

    @Test
    public void addDiscontinuedHoldings_whenAttributesContainsNoHoldings() {
        final ChunkItem chunkItem = successfulChunkItem()
                .withData( new AddiRecord(marshallToBytes(new WorldCatAttributes()), "data".getBytes()).getBytes())
                .withType(ChunkItem.Type.ADDI);
        final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                ChunkItemWithWorldCatAttributes.of(chunkItem);
        chunkItemWithWorldCatAttributes.addDiscontinuedHoldings(Arrays.asList("ABC", "DEF", "GHI"));

        assertThat(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings(),
                is(Arrays.asList(
                        new Holding().withSymbol("ABC").withAction(Holding.Action.DELETE),
                        new Holding().withSymbol("DEF").withAction(Holding.Action.DELETE),
                        new Holding().withSymbol("GHI").withAction(Holding.Action.DELETE))));
    }

    @Test
    public void addDiscontinuedHoldings() {
        final Holding originalHolding = new Holding().withSymbol("ABC").withAction(Holding.Action.INSERT);
        final WorldCatAttributes attributes = new WorldCatAttributes()
                .withHoldings(Collections.singletonList(originalHolding));
        final ChunkItem chunkItem = successfulChunkItem()
                .withData( new AddiRecord(marshallToBytes(attributes), "data".getBytes()).getBytes())
                .withType(ChunkItem.Type.ADDI);
        final ChunkItemWithWorldCatAttributes chunkItemWithWorldCatAttributes =
                ChunkItemWithWorldCatAttributes.of(chunkItem);
        chunkItemWithWorldCatAttributes.addDiscontinuedHoldings(Arrays.asList("ABC", "DEF", "GHI"));

        assertThat(chunkItemWithWorldCatAttributes.getWorldCatAttributes().getHoldings(),
                is(Arrays.asList(originalHolding,
                        new Holding().withSymbol("DEF").withAction(Holding.Action.DELETE),
                        new Holding().withSymbol("GHI").withAction(Holding.Action.DELETE))));
    }

    private byte[] marshallAddiRecords(AddiRecord... addiRecords) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (AddiRecord addiRecord : addiRecords) {
            out.write(addiRecord.getBytes());
        }
        return out.toByteArray();
    }

    private <T> byte[] marshallToBytes(T obj) {
        try {
            return jsonbContext.marshall(obj).getBytes();
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
