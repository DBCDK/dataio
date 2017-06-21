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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RawRepoMarcXmlDataPartitionerTest extends AbstractPartitionerTestBase {

    @Test
    public void newInstance_inputStreamArgIsNull_throws() {
        try {
            RawRepoMarcXmlDataPartitioner.newInstance(null, StandardCharsets.UTF_8.name());
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
        assertThat(RawRepoMarcXmlDataPartitioner.newInstance(getEmptyInputStream(), StandardCharsets.UTF_8.name()), is(notNullValue()));
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

        assertThat("has 1st result", iterator.hasNext(), is(true));
        DataPartitionerResult result = iterator.next();
        assertThat("1st result trackingId", result.getChunkItem().getTrackingId(), is("<trackingid-&dataio-1>"));
        assertThat("1st result recordInfo", result.getRecordInfo(), is(notNullValue()));
        assertThat("1st result position in datafile", result.getPositionInDatafile(), is(0));

        assertThat("has 2nd result", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("2nd result trackingId", result.getChunkItem().getTrackingId(), is("\"&trackingid-dataio-2\""));
        assertThat("2nd result recordInfo", result.getRecordInfo(), is(notNullValue()));
        assertThat("2nd result position in datafile", result.getPositionInDatafile(), is(1));

        assertThat("has 3rd result", iterator.hasNext(), is(true));
        result = iterator.next();
        assertThat("3rd result trackingId", result.getChunkItem().getTrackingId(), is("'trackingid-'dataio-3&"));
        assertThat("3rd result recordInfo", result.getRecordInfo(), is(notNullValue()));
        assertThat("3rd result position in datafile", result.getPositionInDatafile(), is(2));

        assertThat("has 4th result", iterator.hasNext(), is(false));
    }

    private String buildXmlWithChildren(List<String>children) {
        return children.stream().collect(Collectors.joining("", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><topLevel>","</topLevel>"));
    }

    private DataPartitioner newPartitionerInstance(String xml) {
        return RawRepoMarcXmlDataPartitioner.newInstance(asInputStream(xml), StandardCharsets.UTF_8.name());
    }

}
