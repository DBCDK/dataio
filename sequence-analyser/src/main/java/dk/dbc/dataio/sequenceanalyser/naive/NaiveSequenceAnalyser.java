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

package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElementIdentifier;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;

import java.util.List;

public class NaiveSequenceAnalyser implements SequenceAnalyser {

    private NaiveDependencyGraph dependencyGraph = new NaiveDependencyGraph();

    @Override
    public void add(CollisionDetectionElement element) {
        dependencyGraph.insert(element);
    }

    @Override
    public int deleteAndRelease(CollisionDetectionElementIdentifier identifier) {
        return dependencyGraph.deleteAndRelease(identifier);
    }

    @Override
    public List<CollisionDetectionElement> getInactiveIndependent(int maxSlotsSoftLimit) {
        return dependencyGraph.getInactiveIndependentElementsAndActivate(maxSlotsSoftLimit);
    }

    // Number of elements in internal data-structure.
    @Override
    public int size() {
        return dependencyGraph.size();
    }

    @Override
    public boolean isHead(CollisionDetectionElementIdentifier identifier) {
        return dependencyGraph.isHead(identifier);
    }

}
