package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
class NaiveDependencyGraph {
    private final List<Node> nodes = new ArrayList<>();

    public void insertChunkIntoDependencyGraph(Chunk chunk, Sink sink) {
        Node node = new Node(new ChunkIdentifier(chunk.getJobId(), chunk.getChunkId()), sink.getId(), new ArrayList<Edge>(), new ArrayList<String>());
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

    private void findDependencies(Node fromNode) {
        for (Node toNode : nodes) {
            // find intersection between keysets (if any)
            Set<String> keyIntersection = new HashSet<>(toNode.keys);
            keyIntersection.retainAll(fromNode.keys);
            if (!keyIntersection.isEmpty() && toNode.sinkId == fromNode.sinkId) {
                // intersection - create new edge and add it to the two nodes
                Edge edge = new Edge(toNode, fromNode);
                fromNode.dependsOn.add(edge);
                toNode.dependsOn.add(edge);
            }
        }
    }

    private static class Node {

        public final ChunkIdentifier chunkIdentifier;
        public final long sinkId;
        public final List<Edge> dependsOn;
        public final Set<String> keys;

        public Node(ChunkIdentifier chunkIdentifier, long sinkId, List<Edge> dependsOn, List<String> keys) {
            this.chunkIdentifier = chunkIdentifier;
            this.sinkId = sinkId;
            this.dependsOn = new ArrayList<>(dependsOn);
            this.keys = new HashSet<>(keys);
        }
    }

    private static class Edge {

        public final Node head;
        public final Node tail;

        public Edge(Node head, Node tail) {
            this.head = head;
            this.tail = tail;
        }
    }
}
