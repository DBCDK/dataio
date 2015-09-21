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

package dk.dbc.dataio.sequenceanalyser.keygenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple sequence analyser key generator ensuring sequential ordering for all
 * chunks going to the same destination
 */
public class SequenceAnalyserSinkKeyGenerator implements SequenceAnalyserKeyGenerator {
    private final long sinkId;

    /**
     * @param sinkId the ID of the sink for which a key should be generated
     */
    public SequenceAnalyserSinkKeyGenerator(long sinkId) {
        this.sinkId = sinkId;
    }

    @Override
    public Set<String> generateKeys(List<String> data) {
        final HashSet<String> keys = new HashSet<>(1);
        keys.add(Long.toString(sinkId));
        return keys;
    }
}
