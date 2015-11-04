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

public class IncompleteTransfileNotificationContext implements NotificationContext {
    private String transfileName;
    private String transfileContent;

    @JsonCreator
    public IncompleteTransfileNotificationContext(
            @JsonProperty("transfileName") String transfileName,
            @JsonProperty("transfileContent") String transfileContent) {

        InvariantUtil.checkNotNullOrThrow(transfileName, "transfileName");
        InvariantUtil.checkNotNullOrThrow(transfileContent, "transfileContent");

        this.transfileName = transfileName;
        this.transfileContent = transfileContent;
    }
    public String getTransfileName() {
        return transfileName;
    }

    public String getTransfileContent() {
        return transfileContent;
    }
}