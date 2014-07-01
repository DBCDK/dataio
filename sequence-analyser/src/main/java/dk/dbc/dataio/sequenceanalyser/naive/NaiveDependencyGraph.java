package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class NaiveDependencyGraph {
    private final List<Node> nodes = new ArrayList<>();

    public void insertChunkIntoDependencyGraph(Chunk chunk, Sink sink) {
        Node node = new Node(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId()), sink.getId(), new ArrayList<Edge>(), new ArrayList<Node>(), new ArrayList<String>());
        nodes.add(node);
    }

    public void deleteAndReleaseChunk(ChunkIdentifier identifier) {
        for (Node node : nodes) {
            if (node.chunkIdentifier.chunkId == identifier.chunkId && node.chunkIdentifier.jobId == identifier.jobId) {
                nodes.remove(node);
                return;
            }
        }
    }

    public List<ChunkIdentifier> getIndependentChunks() {
        List<ChunkIdentifier> result = new ArrayList<>();
        for (Node node : nodes) {
            if (node.dependsOn.isEmpty()) {
                result.add(node.chunkIdentifier);
            }
        }
        return result;
    }

    private void findDependencies(Node newNode) {
        for (Node node : nodes) {
            // find intersection between keysets (if any)
            Set<String> keyIntersection = new HashSet<>(node.keys);
            keyIntersection.retainAll(newNode.keys);
            if (!keyIntersection.isEmpty()) {
            }
        }
    }

    private class Node {

        public final ChunkIdentifier chunkIdentifier;
        public final long sinkId;
        public final List<Edge> dependsOn;
        public final List<Node> dependsOnMe;
        public final Set<String> keys;

        public Node(ChunkIdentifier chunkIdentifier, long sinkId, List<Edge> dependsOn, List<Node> dependsOnMe, List<String> keys) {
            this.chunkIdentifier = chunkIdentifier;
            this.sinkId = sinkId;
            this.dependsOn = new ArrayList<>(dependsOn);
            this.dependsOnMe = new ArrayList<>(dependsOnMe);
            this.keys = new HashSet<>(keys);
        }
    }

    private class Edge {

        public final Node head;
        public final Node tail;

        public Edge(Node head, Node tail) {
            this.head = head;
            this.tail = tail;
        }
    }

}
