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

    XLogger LOGGER = XLoggerFactory.getXLogger(NaiveDependencyGraph.class);

    private final List<Node> nodes = new ArrayList<>();

    /**
     * Inserts a new Node in the graph, representing the given Chunk.
     *
     * @param element
     */
    public void insert(CollisionDetectionElement element) {
        Node node = new Node(element.getIdentifier(), element.getKeys());
        LOGGER.info("Created node: {}", node);
        findAndUpdateDependencies(node);
        nodes.add(node);
    }

    /**
     * Method for telling whether a given ChunkIdentifier is at the top/front of
     * the graph.
     *
     * It is in this method assumed that the nodes contained in the
     * DependencyGraph spans a hierarchy where one ChunkIdentifier is at front
     * or at the top.
     *
     * @param identifier
     * @return true if the identifier matches the first element in the
     * DependencyGraph, false otherwise. If the DependencyGraph is empty, then
     * false will be returned.
     */
    public boolean isHead(CollisionDetectionElementIdentifier identifier) {
        return !nodes.isEmpty() && nodes.get(0).getIdentifier().equals(identifier);
    }

    /**
     * Remove all edges for dependent nodes, and deletes the node represented by
     * the ChunkIdentifier.
     * @param identifier
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
                return 1;
            }
        }
        return 0;
    }

    /**
     * Retrieves no more than max independent chunks which are also inactive. W
     * hen returned, the chunks will be changed to active.
     * <p>
     * An independent Chunk, is a in a Node with no outgoing edges. Incoming
     * edges are allowed since this only indicates that another node depends on
     * the current node.
     * @param max maximum number of free chunks to return
     * @return A list of independent chunks which are now flagged as active.
     */
    public List<CollisionDetectionElement> getInactiveIndependentChunksAndActivate(int maxItemsSoftLimit) {
        int inactiveNodesFound = 0;
        List<CollisionDetectionElement> result = new ArrayList<>();
        for (Node node : nodes) {
            if (inactiveNodesFound == maxItemsSoftLimit) {
                break;
            }
            if (node.isActivated()) {
                continue;
            }
            if (!doesNodeContainOutgoingEdges(node)) {
                result.add(new CollisionDetectionElement(node.getIdentifier(), node.getKeys()));
                node.activate();
                inactiveNodesFound++;
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
        for (Node headNode : nodes) {
            // find intersection between keysets (if any)
            Set<String> keyIntersection = new HashSet<>(headNode.getKeys());
            keyIntersection.retainAll(tailNode.getKeys());
            if (!keyIntersection.isEmpty()) {
                // intersection - create new edge and add it to the two nodes
                Edge edge = new Edge(headNode, tailNode);
                tailNode.getEdges().add(edge);
                headNode.getEdges().add(edge);
            }
        }
    }

    private static class Node {
        private final CollisionDetectionElementIdentifier identifier;
        private final List<Edge> edges;
        private final Set<String> keys;
        private boolean activated = false;

        public Node(CollisionDetectionElementIdentifier identifier, Set<String> keys) {
            this.identifier = identifier;
            this.edges = new ArrayList<>();
            this.keys = new HashSet<>(keys);
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
