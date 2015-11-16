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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import dk.dbc.marc.DanMarc2Charset;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DanMarc2LineFormatDataPartitionerTest {

    private final static String SPECIFIED_ENCODING = "latin1";

    @Test
    public void specifiedEncodingDiffersFromActualEncoding_throws() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory()
                .createDataPartitioner(asInputStream(""), StandardCharsets.UTF_8.name());
        try {
            dataPartitioner.iterator();
            fail("No exception thrown");
        } catch (InvalidEncodingException e) { }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncoding_ok() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
        createDataPartitioner(asInputStream(""), SPECIFIED_ENCODING);
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingInLowerCase_ok() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
                createDataPartitioner(asInputStream(""), "LATIN1");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterTrim_ok() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
                createDataPartitioner(asInputStream(""), " latin1 ");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void specifiedEncodingIdenticalToActualEncodingAfterDashReplace_ok() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
                createDataPartitioner(asInputStream(""), "latin-1");
        try {
            dataPartitioner.iterator();
        } catch (InvalidEncodingException e) {
            fail("Exception thrown");
        }
    }

    @Test
    public void getEncoding_expectedEncodingReturned() throws IOException, URISyntaxException {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
                createDataPartitioner(asInputStream(""), SPECIFIED_ENCODING);
        assertThat("Encoding", dataPartitioner.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void dm2LineFormatDataPartitioner_inputStreamIsEmpty_returnsNull()  {
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
                createDataPartitioner(asInputStream(""), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat("Empty input => hasNext() expected to be false", iterator.hasNext(), is(false));
        assertThat("Empty input => nex() expected to return null", iterator.next(), is(nullValue()));
    }

    @Test
    public void dm2LineFormatDataPartitioner_readValidRecord_returnsChunkItemWithMarcRecordAsMarcXchange() {
        final String simpleRecordInLineFormat = "245 00 *aA @*programmer is born*beveryday@@dbc\n";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
                createDataPartitioner(asInputStream(simpleRecordInLineFormat, new DanMarc2Charset()), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
        assertThat("Empty input => hasNext() expected to be false", iterator.hasNext(), is(true));
        assertThat(iterator.next(), not(nullValue()));
    }

    @Test
    public void dm2LineFormatDataPartitioner_readInvalidRecord_throwsInvalidDataException() {
        final String faultyRecordInLineFormat = "245 00 *aA @*programmer is born*beveryday@dbc\n";
        final DataPartitionerFactory.DataPartitioner dataPartitioner = new DanMarc2LineFormatDataPartitionerFactory().
                createDataPartitioner(asInputStream(faultyRecordInLineFormat, new DanMarc2Charset()), SPECIFIED_ENCODING);
        final Iterator<ChunkItem> iterator = dataPartitioner.iterator();
            assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) { }
    }

    /*
     *  Private methods
     */

    private InputStream asInputStream(String xml) {
        return asInputStream(xml, StandardCharsets.UTF_8);
    }

    private InputStream asInputStream(String xml, Charset encoding) {
        return new ByteArrayInputStream(xml.getBytes(encoding));
    }

}
