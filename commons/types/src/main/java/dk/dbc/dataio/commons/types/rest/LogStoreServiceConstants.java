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

package dk.dbc.dataio.commons.types.rest;

public class LogStoreServiceConstants {
    public static final String JOB_ID_VARIABLE      = "jobId";
    public static final String CHUNK_ID_VARIABLE    = "chunkId";
    public static final String ITEM_ID_VARIABLE     = "itemId";

    public static final String ITEM_LOG_ENTRY_COLLECTION = "logentries/jobs/{jobId}/chunks/{chunkId}/items/{itemId}";
    public static final String JOB_LOG_ENTRY_COLLECTION  = "logentries/jobs/{jobId}";

    private LogStoreServiceConstants() { }
}
