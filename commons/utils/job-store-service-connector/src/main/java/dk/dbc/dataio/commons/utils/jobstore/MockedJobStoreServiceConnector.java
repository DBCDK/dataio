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

package dk.dbc.dataio.commons.utils.jobstore;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;

import java.util.LinkedList;
import java.util.Queue;

/**
* Mocked JobStoreServiceConnector implementation able to intercept
* calls to addJob() capturing job input streams in local jobInputStreams
* field and returning values from local jobInfoSnapshots field.
*/
public class MockedJobStoreServiceConnector extends JobStoreServiceConnector {
    public Queue<JobInputStream> jobInputStreams;
    public Queue<JobInfoSnapshot> jobInfoSnapshots;
    public Queue<Chunk> chunks;

    public MockedJobStoreServiceConnector() throws NullPointerException, IllegalArgumentException {
        super(HttpClient.newClient(), "baseurl");
        jobInputStreams = new LinkedList<>();
        jobInfoSnapshots = new LinkedList<>();
        chunks = new LinkedList<>();
    }

    @Override
    public JobInfoSnapshot addJob(JobInputStream jobInputStream) {
        jobInputStreams.add(jobInputStream);
        return jobInfoSnapshots.remove();
    }

    @Override
    public JobInfoSnapshot addChunk(Chunk chunk, long jobId, long chunkId) {
        chunks.add(chunk);
        return jobInfoSnapshots.remove();
    }
}
