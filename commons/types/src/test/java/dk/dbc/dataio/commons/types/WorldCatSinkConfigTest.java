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
package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * WorldCatSinkConfig unit tests
 */
public class WorldCatSinkConfigTest {
    private static final String USER_ID = "userId";
    private static final String PASSWORD = "password";
    private static final String PROJECT_ID = "projectId";
    private static final String ENDPOINT = "endpoint";
    private static final List<String> RETRY_DIAGNOSTICS = Arrays.asList("rt1", "rt2");

    @Test
    public void withRetryDiagnostics_retryDiagnosticsArgIsEmpty_returnsNewInstance() {
        WorldCatSinkConfig worldCatSinkConfig = new WorldCatSinkConfig().withRetryDiagnostics(Collections.emptyList());
        assertThat(worldCatSinkConfig.getRetryDiagnostics(), is(Collections.emptyList()));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstanceWithValuesSet() {
        final WorldCatSinkConfig worldCatSinkConfig = new WorldCatSinkConfig()
                .withUserId(USER_ID)
                .withPassword(PASSWORD)
                .withProjectId(PROJECT_ID)
                .withEndpoint(ENDPOINT)
                .withRetryDiagnostics(RETRY_DIAGNOSTICS);

        assertThat(worldCatSinkConfig.getUserId(), is(USER_ID));
        assertThat(worldCatSinkConfig.getPassword(), is(PASSWORD));
        assertThat(worldCatSinkConfig.getProjectId(), is(PROJECT_ID));
        assertThat(worldCatSinkConfig.getEndpoint(), is(ENDPOINT));
        assertThat(worldCatSinkConfig.getRetryDiagnostics(), is(RETRY_DIAGNOSTICS));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final WorldCatSinkConfig worldCatSinkConfig = new WorldCatSinkConfig();
        final WorldCatSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(worldCatSinkConfig), WorldCatSinkConfig.class);
        assertThat(unmarshalled, is(worldCatSinkConfig));
    }
}
