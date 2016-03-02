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

package dk.dbc.dataio.jobstore.service.util;

import dk.dbc.dataio.jobstore.types.MarcRecordInfo;
import dk.dbc.marc.binding.DataField;
import dk.dbc.marc.binding.MarcRecord;
import dk.dbc.marc.binding.SubField;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MarcRecordInfoBuilderTest {
    private final MarcRecordInfoBuilder recordInfoBuilder = new MarcRecordInfoBuilder();
    private final String id = "42";
    private final String parent = "42parent";
    private final DataField f001 = get001(id);
    private final DataField f004 = get004("e", "c");    // produces non-delete, standalone
    private final DataField f014 = get014(parent);

    @Test
    public void parse_marcRecordArgIsNull_returnsEmpty() {
        assertThat(recordInfoBuilder.parse(null).isPresent(), is(false));
    }

    @Test
    public void parse_without014() {
        final MarcRecord marcRecord = getMarcRecord(f001, f004);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.STANDALONE));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
        assertKeys(recordInfo.getKeys(), Collections.singletonList(id));
    }

    @Test
    public void parse_standaloneWith014_014IsNeverParsed() {
        final MarcRecord marcRecord = getMarcRecord(f001, f004, f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.STANDALONE));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertKeys(recordInfo.getKeys(), Collections.singletonList(id));
    }

    @Test
    public void parse_headWith014_014IsNeverParsed() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("h", "c"), f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.HEAD));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertKeys(recordInfo.getKeys(), Collections.singletonList(id));
    }

    @Test
    public void parse_sectionWith014() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("s", "c"), f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.SECTION));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(true));
        assertThat("getParentRelation()", recordInfo.getParentRelation(), is(parent));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertKeys(recordInfo.getKeys(), Arrays.asList(id, parent));
    }

    @Test
    public void parse_volumeWith014() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("b", "c"), f014);
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getId()", recordInfo.getId(), is(id));
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.VOLUME));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(true));
        assertThat("getParentRelation()", recordInfo.getParentRelation(), is(parent));
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
        assertKeys(recordInfo.getKeys(), Arrays.asList(id, parent));
    }

    @Test
    public void parse_empty004a_defaultsToStandaloneType() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("", "c"));
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.STANDALONE));
        assertKeys(recordInfo.getKeys(), Collections.singletonList(id));
    }

    @Test
    public void parse_deleteMarked() {
        final MarcRecord marcRecord = getMarcRecord(f001, get004("e", "d"));
        final Optional<MarcRecordInfo> recordInfoOptional = recordInfoBuilder.parse(marcRecord);

        assertThat("Optional is present", recordInfoOptional.isPresent(), is(true));
        final MarcRecordInfo recordInfo = recordInfoOptional.get();
        assertThat("isDelete()", recordInfo.isDelete(), is(true));
        assertKeys(recordInfo.getKeys(), Collections.singletonList(id));
    }

    public static MarcRecord getMarcRecord(DataField... dataFields) {
        return new MarcRecord()
                .addAllFields(Arrays.asList(dataFields));
    }

    public static DataField get001(String a) {
        return new DataField()
                .setTag("001")
                .addSubfield(
                    new SubField()
                        .setCode('f')
                        .setData("danmarc2"))
                .addSubfield(
                    new SubField()
                        .setCode('a')
                        .setData(a));
    }

    public static DataField get004(String a, String r) {
        return new DataField()
                .setTag("004")
                .addSubfield(
                    new SubField()
                        .setCode('a')
                        .setData(a))
                .addSubfield(
                    new SubField()
                        .setCode('r')
                        .setData(r));
    }

    public static DataField get014(String a) {
        return new DataField()
                .setTag("014")
                .addSubfield(
                    new SubField()
                        .setCode('x')
                        .setData("SMS"))
                .addSubfield(
                    new SubField()
                        .setCode('a')
                        .setData(a));
    }

    /*
     * Private methods
     */

    private void assertKeys(Set<String> actualKeys, List<String> expectedKeys) {
        assertThat("Keys.size", actualKeys.size(), is(expectedKeys.size()));
        for(String key : expectedKeys) {
            assertThat("key", actualKeys.contains(key), is(true));
        }
    }
}