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
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RawRepoMarcXmlDataPartitionerTest extends AbstractPartitionerTestBase {

    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        try {
            RawRepoMarcXmlDataPartitioner.newInstance(null, getUft8Encoding());
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsNull_throws() {
        try {
            RawRepoMarcXmlDataPartitioner.newInstance(getEmptyInputStream(), null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void newInstance_encodingArgIsEmpty_throws() {
        try {
            RawRepoMarcXmlDataPartitioner.newInstance(getEmptyInputStream(), "");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void newInstance_allArgsAreValid_returnsNewDataPartitioner() {
        assertThat(RawRepoMarcXmlDataPartitioner.newInstance(getEmptyInputStream(), getUft8Encoding()), is(notNullValue()));
    }

    @Test
    public void iterator_next_erroneousXMLContainingUnfinishedSecondChildThrows() {
        final String id = "123456";
        final String child =
                "<child>"
                + "<collection xmlns=\"info:lc/xmlns/marcxchange-v1\">"
                +   "<record>"
                +     "<marcx:datafield xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" tag=\"001\">"
                +       "<marcx:subfield code=\"a\">" + id + "</marcx:subfield>"
                +     "</marcx:datafield>"
                +   "</record>"
                + "</collection>"
                +"</child>";

        final String erroneousChild = "<child><grandChild>Pirate so brave on the seven seas</grand";

        final ChunkItem expectedResult = new ChunkItemBuilder()
                .setData(buildXmlWithChildren(Collections.singletonList(child)))
                .setType(Arrays.asList(ChunkItem.Type.DATACONTAINER, ChunkItem.Type.MARCXCHANGE))
                .build();

        final DataPartitioner dataPartitioner = newPartitionerInstance(buildXmlWithChildren(Arrays.asList(child, erroneousChild)));
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        final DataPartitionerResult dataPartitionerResult = iterator.next();
        assertThat("dataPartitioner.chunkItem", dataPartitionerResult.getChunkItem(), is(expectedResult));
        assertThat("dataPartitioner.recordInfo.id", dataPartitionerResult.getRecordInfo().getId(), is(id));
        assertThat(iterator.hasNext(), is(true));
        try {
            iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void iterator_next_marcRecordIsNullThrows() {
        final String child = "<child><grandChild>This is the tale of Captain Jack Sparrow</grandChild></child>";

        final DataPartitioner dataPartitioner = newPartitionerInstance(buildXmlWithChildren(Collections.singletonList(child)));
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();

        assertThat(iterator.hasNext(), is(true));
        try {
         iterator.next();
            fail("No exception thrown");
        } catch (InvalidDataException e) {
        }
    }

    @Test
    public void iterator_next_returnsThreeChunkItemsWithUnescapedTrackingIdSet() {
        final DataPartitioner dataPartitioner = newPartitionerInstance(getDataContainerXmlWithMarcExchangeAndTrackingIds());
        final Iterator<DataPartitionerResult> iterator = dataPartitioner.iterator();
        assertThat(iterator.hasNext(), is(true));

        final DataPartitionerResult dataPartitionerResult0 = iterator.next();
        assertThat("chunkItem0.trackingId", dataPartitionerResult0.getChunkItem().getTrackingId(), is("<trackingid-&dataio-1>"));
        assertThat("recordInfo0", dataPartitionerResult0.getRecordInfo(), is(notNullValue()));
        assertThat(iterator.hasNext(), is(true));

        final DataPartitionerResult dataPartitionerResult1 = iterator.next();
        assertThat("chunkItem1.trackingId", dataPartitionerResult1.getChunkItem().getTrackingId(), is("\"&trackingid-dataio-2\""));
        assertThat("recordInfo1", dataPartitionerResult1.getRecordInfo(), is(notNullValue()));
        assertThat(iterator.hasNext(), is(true));

        final DataPartitionerResult dataPartitionerResult2 = iterator.next();
        assertThat("chunkItem2.trackingId", dataPartitionerResult2.getChunkItem().getTrackingId(), is("'trackingid-'dataio-3&"));
        assertThat("recordInfo2", dataPartitionerResult2.getRecordInfo(), is(notNullValue()));
        assertThat(iterator.hasNext(), is(false));
    }

    private String buildXmlWithChildren(List<String>children) {
        StringBuilder stringBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>");
        children.forEach(stringBuilder::append);
        stringBuilder.append("</topLevel>");
        return stringBuilder.toString();
    }

    private DataPartitioner newPartitionerInstance(String xml) {
        return RawRepoMarcXmlDataPartitioner.newInstance(asInputStream(xml), getUft8Encoding());
    }

}
