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


import dk.dbc.dataio.commons.types.ChunkItem.Type;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ChunkItemTest {
    private static final long ID = 1L;
    private static final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);
    private static final ChunkItem.Status STATUS = ChunkItem.Status.SUCCESS;

    @Test
    public void withData_dataArgIsNull_throws() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat(() -> chunkItem.withData((byte[]) null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withDiagnostics_diagnosticsArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
                .withDiagnostics(null);
        assertThat(chunkItem.getDiagnostics(), is(nullValue()));
    }

    @Test
    public void withDiagnostics_diagnosticsCanBeAppended() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat("diagnostics before append", chunkItem.getDiagnostics(), is(nullValue()));

        chunkItem.withDiagnostics(ObjectFactory.buildFatalDiagnostic("Test Fatal"));
        assertThat("diagnostics after first append", chunkItem.getDiagnostics(), notNullValue());
        assertThat("number of diagnostics after first append", chunkItem.getDiagnostics().size(),is(1));
        assertThat("chunk item status", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));

        chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Test Fatal2"));
        assertThat("diagnostics after second append", chunkItem.getDiagnostics(), notNullValue());
        assertThat("number of diagnostics after second append", chunkItem.getDiagnostics().size(),is(2));
    }

    @Test
    public void withEncoding_encodingArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
                .withEncoding(null);
        assertThat(chunkItem.getEncoding(), is(nullValue()));
    }

    @Test
    public void withId_idArgIsInvalid_throws() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat(() -> chunkItem.withId(Constants.CHUNK_ITEM_ID_LOWER_BOUND - 1), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void withStatus_statusArgIsNull_throws() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat(() -> chunkItem.withStatus(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withTrackingId_trackingIdArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
                .withTrackingId(null);
        assertThat(chunkItem.getTrackingId(), is(nullValue()));
    }

    @Test
    public void withTrackingId_trackingIdArgCanBeEmpty() {
        final ChunkItem chunkItem = new ChunkItem()
                .withTrackingId("");
        assertThat(chunkItem.getTrackingId(), is(""));
    }

    @Test
    public void withType_typeArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
            .withType(null);
        assertThat(chunkItem.getType(), is(nullValue()));
    }

    @Test
    public void constructor5arg_allArgsAreValid_returnsNewInstance() throws JSONBException {
        final ChunkItem instance = new ChunkItem(ID, DATA, STATUS, Arrays.asList(Type.UNKNOWN, Type.GENERICXML), StandardCharsets.UTF_8);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getId(), is(ID));
        assertThat(instance.getData(), is(DATA));
        assertThat(instance.getStatus(), is(STATUS));
        assertThat(instance.getType(), is( Arrays.asList(Type.UNKNOWN, Type.GENERICXML)));
        assertThat(instance.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void unmarshallingChunkItem() throws JSONBException {
        final String json = "{\"id\":0,\"data\":\"MQ==\",\"status\":\"SUCCESS\"}";
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    @Test
    public void unmarshallingChunkItemWithTypeAndEncoding() throws JSONBException {
        final String json = "{\"id\":1,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\",\"type\":[\"STRING\",\"UNKNOWN\"],\"encoding\":\"UTF-8\"}";
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    public static ChunkItem newChunkItemInstance() {
        return new ChunkItem()
                .withId(ID)
                .withData(DATA)
                .withStatus(STATUS);
    }
}
