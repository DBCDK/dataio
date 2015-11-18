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
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class ChunkItemTest {
    private static final long ID = 1L;
    private static final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);
    private static final ChunkItem.Status STATUS = ChunkItem.Status.SUCCESS;

    @Test(expected = NullPointerException.class)
    public void constructor_dataArgIsNull_throws() {
        new ChunkItem(ID, null, STATUS);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_statusArgIsNull_throws() {
        new ChunkItem(ID, DATA, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsLessThanLowerBound_throws() {
        new ChunkItem(Constants.CHUNK_ITEM_ID_LOWER_BOUND - 1, DATA, null);
    }

    @Test
    public void constructor_dataArgIsEmpty_returnsNewInstance() {
        assertThat(new ChunkItem(ID, new byte[0], STATUS), is(notNullValue()));
    }

    @Test
    public void constructor_combatallArgsAreValid_returnsNewInstance() {
        final ChunkItem instance = new ChunkItem(ID, DATA, STATUS);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getId(), is(ID));
        assertThat(instance.getData(), is(DATA));
        assertThat(instance.getStatus(), is(STATUS));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        ArrayList<Type> types=new ArrayList<>(Arrays.asList(Type.UNKNOWN, Type.GENERICXML));
        final ChunkItem instance = new ChunkItem(ID, DATA, STATUS, types, "UTF-8");
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getId(), is(ID));
        assertThat(instance.getData(), is(DATA));
        assertThat(instance.getStatus(), is(STATUS));
        assertThat(instance.getType(), is( Arrays.asList(Type.UNKNOWN, Type.GENERICXML)));
        assertThat(instance.getEncoding(), is("UTF-8"));
    }

    @Test
    public void appenddiagnostics_setsStatusToFaiolure() throws Exception {
        final ChunkItem instance = new ChunkItem(ID, DATA, STATUS);
        assertThat(instance.getDiagnostics(), is(nullValue()));
        assertThat(instance.getStatus(), is( STATUS ));

        instance.appendDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, "Test Fatal"));
        assertThat(instance.getDiagnostics(), notNullValue());
        assertThat(instance.getDiagnostics().size(),is(1));
        assertThat(instance.getStatus(), is(ChunkItem.Status.FAILURE ));
        instance.appendDiagnostics(new Diagnostic(Diagnostic.Level.FATAL, "Test Fatal2"));

        assertThat(instance.getDiagnostics(), notNullValue());
        assertThat(instance.getDiagnostics().size(),is(2));

    }

    public static ChunkItem newChunkItemInstance() {
        return new ChunkItem(ID, DATA, STATUS);
    }



}
