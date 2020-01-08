/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore.service.partitioner;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.common.utils.io.ByteCountingInputStream;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.service.util.CharacterEncodingScheme;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.invariant.InvariantUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * Factory for JSON data partitioner
 *
 * <p>
 * Example JSON input - array of objects:
 * <pre>
 * {@code
 * [
 *    {"id": "record1", ...},
 *    {"id": "record2", ...}
 * ]
 * }
 * </pre>
 * Example JSON input - standalone object:
 * <pre>
 * {@code
 *    {"id": "record1", ...}
 * }
 * </pre>
 */
public class JsonDataPartitioner implements DataPartitioner {
    private final ByteCountingInputStream inputStream;
    private final Charset encoding;
    private Iterator<DataPartitionerResult> drainItemsIterator;
    private int positionInDatafile = 0;

    /**
     * Creates new instance of DataPartitioner for JSON records
     * @param inputStream stream from which JSON records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     * @return new instance of JsonDataPartitioner
     */
    public static JsonDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new JsonDataPartitioner(inputStream, encodingName);
    }

    /**
     * Super class constructor
     * @param inputStream stream from which JSON records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     */
    JsonDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(encodingName, "encodingName");
        this.inputStream = new ByteCountingInputStream(inputStream);
        this.encoding = CharacterEncodingScheme.charsetOf(encodingName);
    }

    @Override
    public Charset getEncoding() throws InvalidEncodingException {
        return encoding;
    }

    @Override
    public long getBytesRead() {
        return inputStream.getBytesRead();
    }

    @Override
    public Iterator<DataPartitionerResult> iterator() {
        if (drainItemsIterator != null) {
            // Since we need to reuse the same JSON parser used for drainage
            return drainItemsIterator;
        }

        return new Iterator<DataPartitionerResult>() {
            final JsonParser jsonParser = newJsonParser();
            boolean positionedAtNextObject = false;
            JsonToken token;

            @Override
            public boolean hasNext() {
                try {
                    if (token != JsonToken.START_OBJECT) {
                        while ((token = jsonParser.nextToken()) != null) {
                            if (token == JsonToken.START_OBJECT) {
                                positionedAtNextObject = true;
                                break;
                            }
                        }
                    }
                    return token == JsonToken.START_OBJECT;
                } catch (IOException e) {
                    throw new PrematureEndOfDataException(e);
                }
            }

            @Override
            public DataPartitionerResult next() {
                if (!positionedAtNextObject) {
                    hasNext();
                }
                if (token == null) {
                    return DataPartitionerResult.EMPTY;
                }
                try {
                    // consume object at current position in input stream
                    final JsonNode jsonNode = jsonParser.readValueAsTree();
                    final ChunkItem chunkItem = ChunkItem.successfulChunkItem()
                            .withId(0)
                            .withData(jsonNode.toString())
                            .withType(ChunkItem.Type.JSON);
                    return new DataPartitionerResult(chunkItem, null, positionInDatafile++);
                } catch (IOException e) {
                    throw new PrematureEndOfDataException(e);
                } finally {
                    token = null;
                    positionedAtNextObject = false;
                }
            }
        };
    }

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void drainItems(int itemsToRemove) {
        if (itemsToRemove < 0) {
            throw new IllegalArgumentException("Unable to drain a negative number of items");
        }
        if (drainItemsIterator == null) {
            drainItemsIterator = this.iterator();
        }
        while (--itemsToRemove >=0) {
            try {
                drainItemsIterator.next();
            } catch (PrematureEndOfDataException e) {
                throw e;    // to potentially trigger a retry
            } catch (Exception e) {
                // we simply swallow these as they have already been handled in chunk items
            }
        }
    }

    private JsonParser newJsonParser() {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.getFactory().createParser(inputStream);
        } catch (IOException e) {
            throw new PrematureEndOfDataException(e);
        }
    }
}
