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
import dk.dbc.commons.es.ESUtil;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class EsWorkloadTest {
    private static final int USER_ID = 42;
    private static final ESUtil.PackageType PACKAGE_TYPE = ESUtil.PackageType.DATABASE_UPDATE;
    private static final ESUtil.Action ACTION = ESUtil.Action.INSERT;

    @Test(expected = NullPointerException.class)
    public void constructor_chunkResultArgIsNull_throws() {
        new EsWorkload(null, new ArrayList<AddiRecord>(0), USER_ID, PACKAGE_TYPE, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_addiRecordsArgIsNull_throws() {
        new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), null, USER_ID, PACKAGE_TYPE, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_packageTypeArgIsNull_throws() {
        new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, null, ACTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_actionArgIsNull_throws() {
        new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, PACKAGE_TYPE, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final EsWorkload instance = new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(), new ArrayList<AddiRecord>(0),
                USER_ID, PACKAGE_TYPE, ACTION);
        assertThat(instance, is(notNullValue()));
    }
}
