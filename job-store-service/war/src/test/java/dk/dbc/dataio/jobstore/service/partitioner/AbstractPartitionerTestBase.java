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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPartitionerTestBase {
    private static final String DATACONTAINERS_WITH_TRACKING_ID_XML = "datacontainers-with-tracking-id.xml";
    private static final InputStream EMPTY_INPUT_STREAM = StringUtil.asInputStream("");
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    protected static String getDataContainerXmlWithMarcExchangeAndTrackingIds() {
        return ResourceReader.getResourceAsString(
                AbstractPartitionerTestBase.class, DATACONTAINERS_WITH_TRACKING_ID_XML);
    }

    protected static InputStream getEmptyInputStream() {
        return EMPTY_INPUT_STREAM;
    }

    protected InputStream asInputStream(String xml) {
        return asInputStream(xml, UTF_8);
    }

    protected InputStream asInputStream(String xml, Charset encoding) {
        return new ByteArrayInputStream(xml.getBytes(encoding));
    }

    protected List<DataPartitionerResult> getResults(DataPartitioner partitioner) {
        final List<DataPartitionerResult> results = new ArrayList<>();
        for (DataPartitionerResult result : partitioner) {
            if (!result.isEmpty()) {
                results.add(result);
            }
        }
        return results;
    }

    static InputStream getResourceAsStream(String resourceName) {
        return ResourceReader.getResourceAsStream(AbstractPartitionerTestBase.class, resourceName);
    }

    static byte[] getResourceAsByteArray(String resourceName) {
        return ResourceReader.getResourceAsByteArray(AbstractPartitionerTestBase.class, resourceName);
    }
}
