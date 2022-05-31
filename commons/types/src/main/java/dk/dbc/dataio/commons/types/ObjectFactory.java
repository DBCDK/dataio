package dk.dbc.dataio.commons.types;

import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * This class eases building dataio.commons.types objects used repetitively across modules
 */
public class ObjectFactory {

    private ObjectFactory() {
    }

    /*
     * IGNORED chunk item
     */

    /**
     * Builds a new chunk item with given id and data
     *
     * @param itemId of the item
     * @param data   of the item
     * @return chunkItem with status: IGNORE, UTF_8 encoding and type: STRING
     */
    public static ChunkItem buildIgnoredChunkItem(long itemId, String data) {
        return buildIgnoredChunkItem(itemId, data, null);
    }

    /**
     * Builds a new chunk item with given id and data
     *
     * @param itemId     of the item
     * @param data       of the item
     * @param trackingId of the item
     * @return chunkItem with tracking id, status: IGNORE, UTF_8 encoding and type: STRING
     */
    public static ChunkItem buildIgnoredChunkItem(long itemId, String data, String trackingId) {
        return buildChunkItem(itemId, StringUtil.asBytes(data, StandardCharsets.UTF_8),
                ChunkItem.Status.IGNORE, Collections.singletonList(ChunkItem.Type.STRING), trackingId);
    }

    /*
     * FAILED chunk item
     */

    /**
     * Builds a new chunk item with given id and data
     *
     * @param itemId of the item
     * @param data   of the item
     * @param type   of the item
     * @return chunkItem with status: FAILURE and UTF_8 encoding
     */
    public static ChunkItem buildFailedChunkItem(long itemId, String data, ChunkItem.Type type) {
        return buildFailedChunkItem(itemId, data, type, null);
    }

    /**
     * Builds a new chunk item with given id and data
     *
     * @param itemId     of the item
     * @param data       of the item
     * @param type       of the item
     * @param trackingId of the item
     * @return chunkItem with tracking id, status: FAILURE and UTF_8 encoding
     */
    public static ChunkItem buildFailedChunkItem(long itemId, String data, ChunkItem.Type type, String trackingId) {
        return buildFailedChunkItem(itemId, StringUtil.asBytes(data, StandardCharsets.UTF_8), type, trackingId);
    }

    /**
     * Builds a new chunk item with given id and data
     *
     * @param itemId     of the item
     * @param data       of the item
     * @param type       of the item
     * @param trackingId of the item
     * @return chunkItem with with tracking id, status: FAILURE and UTF_8 encoding
     */
    public static ChunkItem buildFailedChunkItem(long itemId, byte[] data, ChunkItem.Type type, String trackingId) {
        return buildChunkItem(itemId, data, ChunkItem.Status.FAILURE, Collections.singletonList(type), trackingId);
    }

    /**
     * Builds a new chunk item with given id and data
     *
     * @param itemId of the item
     * @param data   of the item
     * @param type   of the item
     * @return chunkItem with status: FAILURE and UTF_8 encoding
     */
    public static ChunkItem buildFailedChunkItem(long itemId, byte[] data, ChunkItem.Type type) {
        return buildChunkItem(itemId, data, ChunkItem.Status.FAILURE, Collections.singletonList(type), null);
    }

    /*
     * SUCCESSFUL chunk item
     */


    /**
     * Builds a new chunk item with given id, data and type
     *
     * @param itemId of the item
     * @param data   of the item
     * @param type   of the item
     * @return chunkItem with given id, data and type. With status: SUCCESS and UTF_8 encoding
     */
    public static ChunkItem buildSuccessfulChunkItem(long itemId, String data, ChunkItem.Type type) {
        return buildSuccessfulChunkItem(itemId, data, type, null);
    }

    /**
     * Builds a new chunk item with given id, data and type
     *
     * @param itemId     of the item
     * @param data       of the item
     * @param type       of the item
     * @param trackingId of the item
     * @return chunkItem with given id, data, tracking id and type. With status: SUCCESS and UTF_8 encoding
     */
    public static ChunkItem buildSuccessfulChunkItem(long itemId, String data, ChunkItem.Type type, String trackingId) {
        return buildChunkItem(itemId, StringUtil.asBytes(data, StandardCharsets.UTF_8),
                ChunkItem.Status.SUCCESS, Collections.singletonList(type), trackingId);
    }

    /**
     * Builds a new chunk item with given id, data and type
     *
     * @param itemId of the item
     * @param data   of the item
     * @param type   of the item
     * @return chunkItem with given id, data and type with status: SUCCESS and UTF_8 encoding
     */
    public static ChunkItem buildSuccessfulChunkItem(long itemId, byte[] data, ChunkItem.Type type) {
        return buildChunkItem(itemId, data, ChunkItem.Status.SUCCESS, Collections.singletonList(type), null);
    }


    /*
     * Diagnostic
     */


    /**
     * Builds a new fatal diagnostic with given message and stacktrace
     *
     * @param message of the diagnostic
     * @param t       stacktrace of the diagnostic
     * @return diagnostic
     */
    public static Diagnostic buildFatalDiagnostic(String message, Throwable t) {
        return new Diagnostic(Diagnostic.Level.FATAL, message, t);
    }

    /**
     * Builds a new fatal diagnostic with given message
     *
     * @param message of the diagnostic
     * @return diagnostic
     */
    public static Diagnostic buildFatalDiagnostic(String message) {
        return new Diagnostic(Diagnostic.Level.FATAL, message);
    }

    /*
     * Private methods
     */

    /**
     * Builds a new chunk item with given id, data, status and types.
     *
     * @param itemId     of the item
     * @param data       of the item
     * @param status     of the item
     * @param types      list of types
     * @param trackingId of the item
     * @return processed chunk item
     */
    private static ChunkItem buildChunkItem(long itemId, byte[] data, ChunkItem.Status status, List<ChunkItem.Type> types, String trackingId) {
        ChunkItem chunkItem = new ChunkItem(itemId, data, status, types, StandardCharsets.UTF_8);
        chunkItem.withTrackingId(trackingId);
        return chunkItem;
    }
}
