/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NotificationTest {
    @Test
    public void jsonMarshallingUnmarshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final Notification notification = new Notification()
                .withId(42)
                .withTimeOfCreation(new Date())
                .withContext(new InvalidTransfileNotificationContext(
                        "filename", "content", "invalid"))
                .withContent("test");
        final String marshalled = jsonbContext.marshall(notification);
        final Notification unmarshalled = jsonbContext.unmarshall(marshalled, Notification.class);
        assertThat(unmarshalled, is(notification));
    }

    @Test
    public void getTypeFromValue() {
        assertThat("Value 1", Notification.Type.of((short) 1), is(Notification.Type.JOB_CREATED));
        assertThat("Value 2", Notification.Type.of((short) 2), is(Notification.Type.JOB_COMPLETED));
        assertThat("Value 4", Notification.Type.of((short) 4), is(Notification.Type.INVALID_TRANSFILE));
    }

    @Test
    public void getStatusFromValue() {
        assertThat("Value 1", Notification.Status.of((short) 1), is(Notification.Status.WAITING));
        assertThat("Value 2", Notification.Status.of((short) 2), is(Notification.Status.COMPLETED));
        assertThat("Value 3", Notification.Status.of((short) 3), is(Notification.Status.FAILED));
    }
}