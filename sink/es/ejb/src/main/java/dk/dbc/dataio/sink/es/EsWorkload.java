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

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity;

import java.util.List;

public class EsWorkload {
    final Chunk deliveredChunk;
    final List<AddiRecord> addiRecords;
    final int userId;
    final TaskSpecificUpdateEntity.UpdateAction action;

    public EsWorkload(Chunk deliveredChunk, List<AddiRecord> addiRecords,
                      int userId, TaskSpecificUpdateEntity.UpdateAction action) {
        this.deliveredChunk = InvariantUtil.checkNotNullOrThrow(deliveredChunk, "deliveredChunk");
        this.addiRecords = InvariantUtil.checkNotNullOrThrow(addiRecords, "addiRecords");
        this.userId = userId;
        this.action = InvariantUtil.checkNotNullOrThrow(action, "action");
    }

    public List<AddiRecord> getAddiRecords() {
        return addiRecords;
    }

    public Chunk getDeliveredChunk() {
        return deliveredChunk;
    }

    public int getUserId() {
        return userId;
    }

    public TaskSpecificUpdateEntity.UpdateAction getAction() {
        return action;
    }
}
