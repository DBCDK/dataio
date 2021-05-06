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

package dk.dbc.dataio.jobstore.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.SinkContent;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class MarcRecordInfoTest {
    private final String id = "42";
    private final String parentRelation = "headOf42";
    private final MarcRecordInfo.RecordType type = MarcRecordInfo.RecordType.STANDALONE;
    private final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, false, parentRelation);

    @Test
    public void constructor_parentRelationArgIsNull_parentRelationIsNull() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, false, null);
        assertThat("getParentRelation()", recordInfo.getParentRelation(), is(nullValue()));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
    }

    @Test
    public void constructor_parentRelationArgIsEmpty_parentRelationIsNull() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, false, "  ");
        assertThat("getParentRelation()", recordInfo.getParentRelation(), is(nullValue()));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(false));
    }

    @Test
    public void constructor_parentRelationArgIsNonEmpty() {
        assertThat("getParentRelation()", recordInfo.getParentRelation(), is(parentRelation));
        assertThat("hasParentRelation()", recordInfo.hasParentRelation(), is(true));
    }

    @Test
    public void constructor_typeArgIsStandalone() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, MarcRecordInfo.RecordType.STANDALONE, false, parentRelation);
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.STANDALONE));
        assertThat("isHead()", recordInfo.isHead(), is(false));
        assertThat("isSection()", recordInfo.isSection(), is(false));
        assertThat("isVolume()", recordInfo.isVolume(), is(false));
    }

    @Test
    public void constructor_typeArgIsVolume() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, MarcRecordInfo.RecordType.VOLUME, false, parentRelation);
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.VOLUME));
        assertThat("isHead()", recordInfo.isHead(), is(false));
        assertThat("isSection()", recordInfo.isSection(), is(false));
        assertThat("isVolume()", recordInfo.isVolume(), is(true));
    }

    @Test
    public void constructor_typeArgIsHead() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, MarcRecordInfo.RecordType.HEAD, false, parentRelation);
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.HEAD));
        assertThat("isHead()", recordInfo.isHead(), is(true));
        assertThat("isSection()", recordInfo.isSection(), is(false));
        assertThat("isVolume()", recordInfo.isVolume(), is(false));
    }

    @Test
    public void constructor_typeArgIsSection() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, MarcRecordInfo.RecordType.SECTION, false, parentRelation);
        assertThat("getType()", recordInfo.getType(), is(MarcRecordInfo.RecordType.SECTION));
        assertThat("isHead()", recordInfo.isHead(), is(false));
        assertThat("isSection()", recordInfo.isSection(), is(true));
        assertThat("isVolume()", recordInfo.isVolume(), is(false));
    }

    @Test
    public void constructor_isDeleteArgIsFalse() {
        assertThat("isDelete()", recordInfo.isDelete(), is(false));
    }

    @Test
    public void constructor_isDeleteArgIsTrue() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, true, parentRelation);
        assertThat("isDelete()", recordInfo.isDelete(), is(true));
    }

    @Test
    public void getKeys_idIsNullAndParentRelationIsNull_returnsEmptySet() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(null, type, false, null);
        Set<String> keys = recordInfo.getKeys(SinkContent.SequenceAnalysisOption.ALL);
        assertThat("keys", keys, is(Collections.emptySet()));
    }

    @Test
    public void getKeys_idIsNullAndParentRelationIsNotNullAndSequenceAnalysisOptionIsAll_returnsSetWithParentRelationAsKey() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(null, type, false, parentRelation);
        Set<String> keys = recordInfo.getKeys(SinkContent.SequenceAnalysisOption.ALL);
        assertThat("keys.size", keys.size(), is(1));
        assertThat("keys.parentRelation", keys.contains(parentRelation), is(true));
    }

    @Test
    public void getKeys_idIsNullAndParentRelationIsNotNullAndSequenceAnalysisIsNull_returnsEmptySet() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(null, type, false, parentRelation);
        Set<String> keys = recordInfo.getKeys(null);
        assertThat("keys", keys, is(Collections.emptySet()));
    }

    @Test
    public void getKeys_idIsNullAndParentRelationIsNotNullAndSequenceAnalysisOptionIsIdOnly_returnsEmptySet() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(null, type, false, parentRelation);
        Set<String> keys = recordInfo.getKeys(SinkContent.SequenceAnalysisOption.ID_ONLY);
        assertThat("keys", keys, is(Collections.emptySet()));
    }

    @Test
    public void getKeys_idIsNotNullAndParentRelationIsNull_returnsSetWithIdAsKey() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, false, null);
        Set<String> keys = recordInfo.getKeys(SinkContent.SequenceAnalysisOption.ALL);
        assertThat("keys.size", keys.size(), is(1));
        assertThat("keys.id", keys.contains(id), is(true));
    }

    @Test
    public void getKeys_idIsNotNullAndParentRelationIsNotNullAndSequenceAnalysisOptionIsAll_returnsSetWithIdAndParentRelationAsKeys() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, false, parentRelation);
        Set<String> keys = recordInfo.getKeys(SinkContent.SequenceAnalysisOption.ALL);
        assertThat("keys.size", keys.size(), is(2));
        assertThat("keys.id", keys.contains(id), is(true));
        assertThat("keys.parentRelation", keys.contains(parentRelation), is(true));
    }

    @Test
    public void getKeys_idIsNotNullAndParentRelationIsNotNullAndSequenceAnalysisOptionIsIdOnly_returnsSetWithIdAsKey() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, false, parentRelation);
        Set<String> keys = recordInfo.getKeys(SinkContent.SequenceAnalysisOption.ID_ONLY);
        assertThat("keys.size", keys.size(), is(1));
        assertThat("keys.id", keys.contains(id), is(true));
    }

    @Test
    public void getKeys_idIsNotNullAndParentRelationIsNotNullAndSequenceAnalysisOptionIsNull_returnsSetWithIdAsKey() {
        final MarcRecordInfo recordInfo = new MarcRecordInfo(id, type, false, parentRelation);
        Set<String> keys = recordInfo.getKeys(null);
        assertThat("keys.size", keys.size(), is(1));
        assertThat("keys.id", keys.contains(id), is(true));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final MarcRecordInfo unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(recordInfo), MarcRecordInfo.class);
        assertThat(unmarshalled, is(recordInfo));
    }

    @Test
    public void marshalling_marcRecordInfoCanBeUnmarshalledAsRecordInfo() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final RecordInfo unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(recordInfo), RecordInfo.class);
        assertThat(unmarshalled, is(recordInfo));
    }
}