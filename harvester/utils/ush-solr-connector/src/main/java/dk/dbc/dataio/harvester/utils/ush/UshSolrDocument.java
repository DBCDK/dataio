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

package dk.dbc.dataio.harvester.utils.ush;

import org.apache.solr.client.solrj.beans.Field;

import java.nio.charset.StandardCharsets;

public class UshSolrDocument {
    @Field("id")
    public String id;

    @Field("_version_")
    public long version;

    @Field("submitter_s")
    public String submitter;

    @Field("content_format_s")
    public String contentFormat;

    @Field("frame_s")
    public String frame;

    public byte[] recordBytes() {
        return "dummy record".getBytes(StandardCharsets.UTF_8);
    }
}
