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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class EsWorkloadTest {
    private static final int USER_ID = 42;
    private static final TaskSpecificUpdateEntity.UpdateAction ACTION = TaskSpecificUpdateEntity.UpdateAction.INSERT;

    @Test(expected = NullPointerException.class)
    public void constructor_chunkResultArgIsNull_throws() {
        new EsWorkload(null, new ArrayList<AddiRecord>(0), USER_ID, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsArgIsNull_throws() {
        new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).build(), null, USER_ID, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_actionArgIsNull_throws() {
        new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final EsWorkload instance = new EsWorkload(new ChunkBuilder(Chunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, ACTION);
        assertThat(instance, is(notNullValue()));
    }
}
