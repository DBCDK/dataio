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

import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * This class eases building dataio.commons.types objects used repetitively across modules
 */
public class ObjectFactory {

    private ObjectFactory() {}

    /*
     * ChunkItem
     */

    /**
     * Builds a new chunk item with given id and data
     * @param itemId of the item
     * @param data of the item
     * @return chunkItem with status: IGNORE, UTF_8 encoding and type: STRING
     */
    public static ChunkItem buildIgnoredChunkItem(long itemId, String data) {
        return buildIgnoredChunkItem(itemId, data, null);
    }

    /**
     * Builds a new chunk item with given id and data
     * @param itemId of the item
     * @param data of the item
     * @param trackingId of the item
     * @return chunkItem with tracking id, status: IGNORE, UTF_8 encoding and type: STRING
     */
    public static ChunkItem buildIgnoredChunkItem(long itemId, String data, String trackingId) {
        return buildChunkItem(itemId, StringUtil.asBytes(data, StandardCharsets.UTF_8),
                ChunkItem.Status.IGNORE, Collections.singletonList(ChunkItem.Type.STRING), trackingId);
    }

    /**
     * Builds a new chunk item with given id and data
     * @param itemId of the item
     * @param data of the item
     * @return chunkItem with status: FAILURE, UTF_8 encoding and type: STRING
     */
    public static ChunkItem buildFailedChunkItem(long itemId, String data) {
        return buildFailedChunkItem(itemId, data, null);
    }

    /**
     * Builds a new chunk item with given id and data
     * @param itemId of the item
     * @param data of the item
     * @param trackingId of the item
     * @return chunkItem with tracking id, status: FAILURE, UTF_8 encoding and type: STRING
     */
    public static ChunkItem buildFailedChunkItem(long itemId, String data, String trackingId) {
        return buildChunkItem(itemId, StringUtil.asBytes(data, StandardCharsets.UTF_8),
                ChunkItem.Status.FAILURE, Collections.singletonList(ChunkItem.Type.STRING), trackingId);
    }

    /**
     * Builds a new chunk item with given id and data
     * @param itemId of the item
     * @param data of the item
     * @return chunkItem with status: FAILURE, UTF_8 encoding and type: STRING
     */
    public static ChunkItem buildFailedChunkItem(long itemId, byte[] data) {
        return buildChunkItem(itemId, data, ChunkItem.Status.FAILURE,
                Collections.singletonList(ChunkItem.Type.STRING), null);
    }

    /**
     * Builds a new chunk item with given id, data and type
     * @param itemId of the item
     * @param data of the item
     * @param type of the item
     * @return chunkItem with given id, data and type. With status: SUCCESS and UTF_8 encoding
     */
    public static ChunkItem buildSuccessfulChunkItem(long itemId, String data, ChunkItem.Type type) {
        return buildSuccessfulChunkItem(itemId, data, type, null);
    }

    /**
     * Builds a new chunk item with given id, data and type
     * @param itemId of the item
     * @param data of the item
     * @param type of the item
     * @param trackingId of the item
     * @return chunkItem with given id, data, tracking id and type. With status: SUCCESS and UTF_8 encoding
     */
    public static ChunkItem buildSuccessfulChunkItem(long itemId, String data, ChunkItem.Type type, String trackingId) {
        return buildChunkItem(itemId, StringUtil.asBytes(data, StandardCharsets.UTF_8),
                ChunkItem.Status.SUCCESS, Collections.singletonList(type), trackingId);
    }

    /**
     * Builds a new chunk item with given id, data and type
     * @param itemId of the item
     * @param data of the item
     * @param type of the item
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
     * @param message of the diagnostic
     * @param t stacktrace of the diagnostic
     * @return diagnostic
     */
    public static Diagnostic buildFatalDiagnostic(String message, Throwable t) {
        return new Diagnostic(Diagnostic.Level.FATAL, message, t);
    }

    /**
     * Builds a new fatal diagnostic with given message
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
     * @param itemId of the item
     * @param data of the item
     * @param status of the item
     * @param types list of types
     * @param trackingId of the item
     * @return processed chunk item
     */
    private static ChunkItem buildChunkItem(long itemId, byte[] data, ChunkItem.Status status, List<ChunkItem.Type> types, String trackingId) {
        ChunkItem chunkItem = new ChunkItem(itemId, data, status, types, StandardCharsets.UTF_8);
        chunkItem.setTrackingId(trackingId);
        return chunkItem;
    }
}
