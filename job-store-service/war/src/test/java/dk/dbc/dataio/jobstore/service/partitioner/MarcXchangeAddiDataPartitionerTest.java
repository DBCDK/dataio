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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class MarcXchangeAddiDataPartitionerTest extends AbstractPartitionerTestBase {
    private final static InputStream EMPTY_STREAM = StringUtil.asInputStream("");
    private final static String UTF_8_ENCODING = "UTF-8";

    @Test(expected = NullPointerException.class)
    public void newInstance_inputStreamArgIsNull_throws() {
        MarcXchangeAddiDataPartitioner.newInstance(null, UTF_8_ENCODING);
    }

    @Test
    public void newInstance_inputStreamArgIsInvalid_throws() {
        assertThat(() -> MarcXchangeAddiDataPartitioner.newInstance(
                new InputStream() {
                    @Override public boolean markSupported() {return false;}
                    @Override public int read() throws IOException {return 0;}},
                UTF_8_ENCODING),
                    isThrowing(IllegalArgumentException.class));
    }

    @Test(expected = NullPointerException.class)
    public void newInstance_encodingArgIsNull_throws() {
        MarcXchangeAddiDataPartitioner.newInstance(EMPTY_STREAM, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void newInstance_encodingArgIsEmpty_throws() {
        MarcXchangeAddiDataPartitioner.newInstance(EMPTY_STREAM, " ");
    }

    @Test
    public void partitioner_readingValidRecord_returnsResultWithChunkItemWithMarcXchangeType() {
        byte[] contentData = AbstractPartitionerTestBase.getResourceAsByteArray("test-record-1-danmarc2.marcXChange");
        AddiRecord addiRecord = new AddiRecord("{}".getBytes(), contentData);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(addiRecord.getBytes());
        final MarcXchangeAddiDataPartitioner partitioner =
                MarcXchangeAddiDataPartitioner.newInstance(byteArrayInputStream, UTF_8_ENCODING);
        final Iterator<DataPartitionerResult> iterator = partitioner.iterator();
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        final ChunkItem chunkItem = dataPartitionerResult.getChunkItem();
        assertThat("chunkItem", chunkItem, is(notNullValue()));
        assertThat("chunkItem.getType()", chunkItem.getType(), is(Collections.singletonList(ChunkItem.Type.MARCXCHANGE)));
        assertThat("chunkItem.getData", StringUtil.asString(chunkItem.getData()), is(StringUtil.asString(contentData)));
        assertThat("recordInfo", dataPartitionerResult.getRecordInfo(), is(notNullValue()));
    }
}