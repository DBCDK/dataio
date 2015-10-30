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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

public class AddNotificationRequest {

    private String destinationEmail;
    private IncompleteTransfileNotificationContext incompleteTransfileNotificationContext;
    private JobNotification.Type notificationType;

    @JsonCreator
    public AddNotificationRequest(
            @JsonProperty("destinationEmail") String destinationEmail,
            @JsonProperty ("incompleteTransfileNotificationContext") IncompleteTransfileNotificationContext incompleteTransfileNotificationContext,
            @JsonProperty ("notificationType") JobNotification.Type notificationType) {

        InvariantUtil.checkNotNullOrThrow(destinationEmail, "destinationEmail");
        InvariantUtil.checkNotNullOrThrow(incompleteTransfileNotificationContext, "incompleteTransfileNotificationContext");
        InvariantUtil.checkNotNullOrThrow(notificationType, "notificationType");

        this.destinationEmail = destinationEmail;
        this.incompleteTransfileNotificationContext = incompleteTransfileNotificationContext;
        this.notificationType = notificationType;
    }

    public String getDestinationEmail() {
        return destinationEmail;
    }
    public IncompleteTransfileNotificationContext getIncompleteTransfileNotificationContext() {
        return incompleteTransfileNotificationContext;
    }
    public JobNotification.Type getNotificationType() {
        return notificationType;
    }
}
