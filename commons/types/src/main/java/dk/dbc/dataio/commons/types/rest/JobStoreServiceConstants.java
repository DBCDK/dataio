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

public class JobStoreServiceConstants {
    public static final String JOB_ID_VARIABLE = "jobId";
    public static final String CHUNK_ID_VARIABLE = "chunkId";
    public static final String ITEM_ID_VARIABLE = "itemId";

    public static final String JOB_COLLECTION                   = "jobs";
    public static final String JOB_COLLECTION_SEARCHES          = "jobs/searches";
    public static final String JOB_COLLECTION_SEARCHES_COUNT    = "jobs/searches/count";
    public static final String ITEM_COLLECTION_SEARCHES         = "jobs/chunks/items/searches";
    public static final String ITEM_COLLECTION_SEARCHES_COUNT   = "jobs/chunks/items/searches/count";
    public static final String JOB_CHUNK_PROCESSED              = "jobs/{jobId}/chunks/{chunkId}/processed";
    public static final String JOB_CHUNK_DELIVERED              = "jobs/{jobId}/chunks/{chunkId}/delivered";
    public static final String JOB_RESOURCEBUNDLE               = "jobs/{jobId}/resourcebundle";
    public static final String CHUNK_ITEM_PARTITIONED           = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/partitioned";
    public static final String CHUNK_ITEM_PROCESSED             = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/processed/current";
    public static final String CHUNK_ITEM_PROCESSED_NEXT        = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/processed/next";
    public static final String CHUNK_ITEM_DELIVERED             = "jobs/{jobId}/chunks/{chunkId}/items/{itemId}/delivered";
    public static final String JOB_NOTIFICATIONS                = "jobs/{jobId}/notifications";

    private JobStoreServiceConstants() { }
}
