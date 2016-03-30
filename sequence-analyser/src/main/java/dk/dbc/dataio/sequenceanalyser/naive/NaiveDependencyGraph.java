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
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class NaiveDependencyGraph {
    private static final XLogger LOGGER = XLoggerFactory.getXLogger(NaiveDependencyGraph.class);

    private final List<Node> nodes = new ArrayList<>();

    /**
     * Inserts a new Node in the graph, representing the given collision detection element.
     */
    public void insert(CollisionDetectionElement element) {
        Node node = new Node(element.getIdentifier(), element.getKeys(), element.getSlotsConsumed());
        LOGGER.info("Created node: {}", node);
        findAndUpdateDependencies(node);
        nodes.add(node);
    }

    /**
     * Method for telling whether a given element is at the top/front of
     * the graph.
     *
     * It is in this method assumed that the nodes contained in the
     * DependencyGraph spans a hierarchy where one identifier is at front
     * or at the top.
     *
     * @return true if the identifier matches the first element in the
     * DependencyGraph, false otherwise. If the DependencyGraph is empty, then
     * false will be returned.
     */
    public boolean isHead(CollisionDetectionElementIdentifier identifier) {
        return !nodes.isEmpty() && nodes.get(0).getIdentifier().equals(identifier);
    }

    /**
     * Removes all edges for dependent nodes and deletes the node represented by
     * the identifier.
     * @return number of consumed slots held by deleted element
     */
    public int deleteAndRelease(CollisionDetectionElementIdentifier identifier) {
        for (Node node : nodes) {
            if (node.getIdentifier().equals(identifier)) {
                for (Edge edge : node.getEdges()) {
                    if (edge.getHead() == node) {
                        edge.getTail().getEdges().remove(edge);
                    } else {
                        edge.getHead().getEdges().remove(edge);
                    }
                }
                nodes.remove(node);
                return node.slotsConsumed;
            }
        }
        return 0;
    }

    /**
     * Returns a list of independent and inactive elements consuming no more than
     * maxSlotsSoftLimit slots combined. On returning all the returned
     * elements are flagged as active.
     * <p>
     * An independent element is a Node with no outgoing edges. Incoming
     * edges are allowed since this only indicates that another node depends on
     * the current node.
     * @param maxSlotsSoftLimit maximum number of slots consumed by all released elements combined.
     *                          Note that this is a soft limit since no guarantees are made
     *                          that it will not be violated for shorter periods of time
     * @return A list of independent elements which are now flagged as active.
     */
    public List<CollisionDetectionElement> getInactiveIndependentElementsAndActivate(int maxSlotsSoftLimit) {
        int numberOfSlotsConsumed = 0;
        List<CollisionDetectionElement> result = new ArrayList<>();
        for (Node node : nodes) {
            if (numberOfSlotsConsumed >= maxSlotsSoftLimit) {
                break;
            }
            if (node.isActivated()) {
                continue;
            }
            if (!doesNodeContainOutgoingEdges(node)) {
                result.add(new CollisionDetectionElement(node.getIdentifier(), node.getKeys(), node.getSlotsConsumed()));
                node.activate();
                numberOfSlotsConsumed += node.getSlotsConsumed();
            }
        }
        return result;
    }

    /**
     * @return the number of nodes in the dependency graph.
     */
    int size() {
        return nodes.size();
    }

    private boolean doesNodeContainOutgoingEdges(Node node) {
        boolean outgoingEdge = false;
        for (Edge edge : node.getEdges()) {
            if (edge.getTail() == node) {
                outgoingEdge = true;
            }
        }
        return outgoingEdge;
    }

    private void findAndUpdateDependencies(Node tailNode) {
        final List<CollisionDetectionElementIdentifier> blockedBy = new ArrayList<>();
        for (Node headNode : nodes) {
            // find intersection between keysets (if any)
            Set<String> keyIntersection = new HashSet<>(headNode.getKeys());
            keyIntersection.retainAll(tailNode.getKeys());
            if (!keyIntersection.isEmpty()) {
                // intersection - create new edge and add it to the two nodes
                Edge edge = new Edge(headNode, tailNode);
                tailNode.getEdges().add(edge);
                headNode.getEdges().add(edge);
                blockedBy.add(headNode.getIdentifier());
            }
        }
        if (!blockedBy.isEmpty()) {
            LOGGER.info("node {} is blocked by {}", tailNode.getIdentifier(), blockedBy);
        }
    }

    private static class Node {
        private final CollisionDetectionElementIdentifier identifier;
        private final List<Edge> edges;
        private final Set<String> keys;
        private final int slotsConsumed;
        private boolean activated = false;

        public Node(CollisionDetectionElementIdentifier identifier, Set<String> keys, int slotsConsumed) {
            this.identifier = identifier;
            this.edges = new ArrayList<>();
            this.keys = new HashSet<>(keys);
            this.slotsConsumed = slotsConsumed;
        }

        /**
         * @return the identifier
         */
        public CollisionDetectionElementIdentifier getIdentifier() {
            return identifier;
        }

        /**
         * @return the edges
         */
        public List<Edge> getEdges() {
            return edges;
        }

        /**
         * @return the keys
         */
        public Set<String> getKeys() {
            return keys;
        }

        public int getSlotsConsumed() {
            return slotsConsumed;
        }

        /**
         * @return the activated
         */
        public boolean isActivated() {
            return activated;
        }

        public void activate() {
            activated = true;
        }

        @Override
        public String toString() {
            return "[" + getIdentifier() + ", " + Arrays.asList(getKeys()) + "]";
        }
    }

    private static class Edge {
        private final Node head;
        private final Node tail;

        public Edge(Node head, Node tail) {
            this.head = head;
            this.tail = tail;
        }

        /**
         * @return the head
         */
        public Node getHead() {
            return head;
        }

        /**
         * @return the tail
         */
        public Node getTail() {
            return tail;
        }

        @Override
        public String toString() {
            return "[" + getHead() + ", " + getTail() + "]";
        }
    }
}
