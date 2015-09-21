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

package dk.dbc.dataio.jobstore.service.partitioner;

import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class DefaultXmlDataPartitionerFactoryTest {
    private static final InputStream INPUT_STREAM = mock(InputStream.class);
    private static final String ENCODING = "UTF-8";
    
    @Test
    public void constructor_returnsNewInstance() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        assertThat(factory, is(notNullValue()));
    }

    @Test
    public void createDataPartitioner_inputStreamArgIsNull_throws() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        try {
            factory.createDataPartitioner(null, ENCODING);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void createDataPartitioner_encodingArgIsNull_throws() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        try {
            factory.createDataPartitioner(INPUT_STREAM, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void createDataPartitioner_encodingArgIsEmpty_throws() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        try {
            factory.createDataPartitioner(INPUT_STREAM, "");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void createDataPartitioner_allArgsAreValid_returnsNewDataPartitioner() {
        final DefaultXmlDataPartitionerFactory factory = new DefaultXmlDataPartitionerFactory();
        assertThat(factory.createDataPartitioner(INPUT_STREAM, ENCODING), is(notNullValue()));
    }
}