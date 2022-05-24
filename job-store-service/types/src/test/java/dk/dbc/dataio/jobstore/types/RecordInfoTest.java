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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RecordInfoTest {
    private final String id = "42";
    private final SinkContent.SequenceAnalysisOption sequenceAnalysisOption = SinkContent.SequenceAnalysisOption.ALL;

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final RecordInfo recordInfo = new RecordInfo("42");
        recordInfo.withPid("pid");
        final RecordInfo unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(recordInfo), RecordInfo.class);
        assertThat(unmarshalled, is(recordInfo));
    }

    @Test
    public void removesWhitespaces() {
        final RecordInfo recordInfo = new RecordInfo(" 4 2 ");
        assertThat(recordInfo.getId(), is(id));
    }

    @Test
    public void getKeys_idIsNull_returnsEmptySet() {
        RecordInfo recordInfo = new RecordInfo(null);
        assertThat(recordInfo.getKeys(sequenceAnalysisOption), is(Collections.emptySet()));
    }

    @Test
    public void getKeys_idIsNotNull_returnsSetContainingId() {
        RecordInfo recordInfo = new RecordInfo(id);
        assertThat(recordInfo.getKeys(sequenceAnalysisOption).size(), is(1));
        assertThat(recordInfo.getKeys(sequenceAnalysisOption).contains(id), is(true));
    }
}
