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

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.sink.util.AddiUtil;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateAction;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ESTaskPackageUtilTest {
    private static final long JOB_ID = 11L;
    private static final long CHUNK_ID = 17L;
    private static final Charset ENCODING = Charset.defaultCharset();
    private static final String DB_NAME = "dbname";
    private static final int USER_ID = 2;
    private static final UpdateAction ACTION = UpdateAction.INSERT;

    private EntityManager em;
    @Before
    public void setUp() throws Exception {
        em=JPATestUtils.createEntityManagerForIntegrationTest("esIT");
    }

    @Test(expected = IllegalStateException.class)
    public void getAddiRecordsFromChunk_twoAddiInOneRecord_throws() throws Exception {
        final String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        final ExternalChunk processedChunk = newProcessedChunk(addiWithTwoRecords);
        AddiUtil.getAddiRecordsFromChunk(processedChunk);
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordsFromChunk_notAddi_throws() throws Exception {
        final String notAddi = "string";
        final ExternalChunk processedChunk = newProcessedChunk(notAddi);
        AddiUtil.getAddiRecordsFromChunk(processedChunk);
    }

    @Test
    public void getAddiRecordsFromChunk_singleSimpleRecordInChunk_happyPath() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ExternalChunk processedChunk = newProcessedChunk(simpleAddiString);

        final List<AddiRecord> addiRecordsFromChunk = AddiUtil.getAddiRecordsFromChunk(processedChunk);
        assertThat(addiRecordsFromChunk.size(), is(1));
    }

    @Test(expected = NullPointerException.class)
    public void getAddiRecordsFromChunkItem_chunkItemArgIsNull_throws() throws Exception {
        AddiUtil.getAddiRecordsFromChunkItem(null);
    }

    @Test
    public void getAddiRecordsFromChunkItem_twoAddiInOneRecord_throws() throws Exception {
        final String addiWithTwoRecords = "1\na\n1\nb\n1\nc\n1\nd\n";
        final ChunkItem chunkItem = newChunkItem(addiWithTwoRecords);
        final List<AddiRecord> addiRecords = AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
        assertThat("Number of Addi records returned", addiRecords.size(), is(2));
        assertThat("first Addi record", addiRecords.get(0), is(notNullValue()));
        assertThat("second Addi record", addiRecords.get(1), is(notNullValue()));
    }

    @Test(expected = NumberFormatException.class)
    public void getAddiRecordsFromChunkItem_chunkItemArgContainsNonAddiData_throws() throws Exception {
        final ChunkItem chunkItem = newChunkItem("non-addi");
        AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
    }

    @Test
    public void getAddiRecordsFromChunkItem_chunkItemArgContainsValidAddi_returnsAddiRecordInstance() throws Exception {
        final String simpleAddiString = "1\na\n1\nb\n";
        final ChunkItem chunkItem = newChunkItem(simpleAddiString);
        final List<AddiRecord> addiRecords = AddiUtil.getAddiRecordsFromChunkItem(chunkItem);
        assertThat("Number of Addi records returned", addiRecords.size(), is(1));
        assertThat("Addi record", addiRecords.get(0), is(notNullValue()));
    }

    @Test
    public void insertTaskPackage_singleSimpleRecordInWorkload_happyPath() throws Exception {

        final String simpleAddiString = "1\na\n1\nb\n";
        final EsWorkload esWorkload = newEsWorkload(simpleAddiString);

        em.getTransaction().begin();
        int targetRefernce=ESTaskPackageUtil.insertTaskPackage(em, DB_NAME, esWorkload);
        em.getTransaction().commit();
        JPATestUtils.clearEntityManagerCache( em );

        TaskSpecificUpdateEntity resultTP=em.find(TaskSpecificUpdateEntity.class, targetRefernce);

        assertThat(targetRefernce, is(resultTP.getTargetreference().intValue()));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_connectionArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(null, DB_NAME, newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_dbnameArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(em, null, newEsWorkload(""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertTaskPackage_dbnameArgIsEmpty_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(em, "", newEsWorkload(""));
    }

    @Test(expected = NullPointerException.class)
    public void insertTaskPackage_esWorkloadArgIsNull_throws() throws Exception {
        ESTaskPackageUtil.insertTaskPackage(em, DB_NAME, null);
    }

    @Test(expected = NullPointerException.class)
    public void chopUp_listArgIsNull_throws() {
        ESTaskPackageUtil.chopUp(null, 42);
    }

    @Test(expected = IllegalArgumentException.class)
    public void chopUp_sublistSizeArgIsZero_throws() {
        ESTaskPackageUtil.chopUp(Collections.emptyList(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void chopUp_sublistSizeArgIsLessThanZero_throws() {
        ESTaskPackageUtil.chopUp(Collections.emptyList(), -1);
    }

    @Test
    public void chopUp_listArgIsEmpty_returnEmptyList() {
        final List<List<Object>> lists = ESTaskPackageUtil.chopUp(Collections.emptyList(), 42);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.isEmpty(), is(true));
    }

    @Test
    public void chopUp_sublistSizeArgIsLargerThanActualListSize_returnsSinglePart() {
        final List<Integer> integers = Arrays.asList(1, 2, 3);
        final List<List<Integer>> lists = ESTaskPackageUtil.chopUp(integers, 42);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(1));
        assertThat(lists.get(0), is(integers));
    }

    @Test
    public void chopUp_sublistSizeArgEqualsActualListSize_returnsSinglePart() {
        final List<Integer> integers = Arrays.asList(1, 2, 3);
        final List<List<Integer>> lists = ESTaskPackageUtil.chopUp(integers, integers.size());
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(1));
        assertThat(lists.get(0), is(integers));
    }

    @Test
    public void chopUp_sublistSizeArgIsLessThatActualListSize_returnsMultipleParts() {
        final List<Integer> integers = Arrays.asList(1, 2, 3, 4);

        List<List<Integer>> lists = ESTaskPackageUtil.chopUp(integers, 2);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(2));
        assertThat(lists.get(0), is(integers.subList(0, 2)));
        assertThat(lists.get(1), is(integers.subList(2, 4)));

        lists = ESTaskPackageUtil.chopUp(integers, 3);
        assertThat(lists, is(notNullValue()));
        assertThat(lists.size(), is(2));
        assertThat(lists.get(0), is(integers.subList(0, 3)));
        assertThat(lists.get(1), is(integers.subList(3, 4)));
    }

    private EsWorkload newEsWorkload(String record) throws IOException {
        return new EsWorkload(new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build(),
                Collections.singletonList(newAddiRecordFromString(record)), USER_ID, ACTION);
    }

    private ChunkItem newChunkItem(String record) {
        return new ChunkItemBuilder()
                .setId(0L)
                .setData(StringUtil.asBytes(record))
                .build();
    }

    private ExternalChunk newProcessedChunk(String record) {
        ExternalChunk processedChunk = new ExternalChunk(JOB_ID, CHUNK_ID, ExternalChunk.Type.PROCESSED);
        processedChunk.insertItem(newChunkItem(record));
        processedChunk.setEncoding(ENCODING);
        return processedChunk;
    }

    private AddiRecord newAddiRecordFromString(String record) throws IOException {
        final AddiReader addiReader = new AddiReader(new ByteArrayInputStream(record.getBytes(ENCODING)));
        return addiReader.getNextRecord();
    }
}
