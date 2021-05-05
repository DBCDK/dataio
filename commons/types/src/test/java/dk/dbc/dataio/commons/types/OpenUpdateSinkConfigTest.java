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

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * OpenUpdateSinkConfig unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class OpenUpdateSinkConfigTest {
    private static final String USER_ID = "userId";
    private static final String PASSWORD = "password";
    private static final String ENDPOINT = "endpoint";
    private static final List<String> AVAILABLE_QUEUE_PROVIDERS = Arrays.asList("qp1", "qp2");

    @Test
    public void withUserId_userIdArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withUserId(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withUserId_userIdArgIsEmpty_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withUserId(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void withPassword_passwordArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withPassword(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withPassword_passwordArgIsEmpty_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withPassword(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void withEndpoint_webUrlArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withEndpoint(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withEndpoint_webUrlArgIsEmpty_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withEndpoint(""), isThrowing(IllegalArgumentException.class));;
    }

    @Test
    public void withAvailableQueueProviders_queueProviderArgIsNull_throws() {
        assertThat(() -> new OpenUpdateSinkConfig().withAvailableQueueProviders(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withQueueProvider_queueProviderArgIsEmpty_returnsNewInstance() {
        OpenUpdateSinkConfig openUpdateSinkConfig = new OpenUpdateSinkConfig().withAvailableQueueProviders(Collections.emptyList());
        assertThat(openUpdateSinkConfig.getAvailableQueueProviders(), is(Collections.emptyList()));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final OpenUpdateSinkConfig openUpdateSinkConfig = new OpenUpdateSinkConfig();
        final OpenUpdateSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(openUpdateSinkConfig), OpenUpdateSinkConfig.class);
        assertThat(unmarshalled, is(openUpdateSinkConfig));
    }

    public static OpenUpdateSinkConfig newOpenUpdateSinkConfigInstance() {
        return new OpenUpdateSinkConfig().withUserId(USER_ID).withPassword(PASSWORD).withEndpoint(ENDPOINT).withAvailableQueueProviders(AVAILABLE_QUEUE_PROVIDERS);
    }

}
