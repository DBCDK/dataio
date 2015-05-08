package dk.dbc.dataio.sequenceanalyser;

import java.util.List;

/**
 * The sequence analyser is a mostly a data structure for finding dependencies
 * between chunks. Whenever a chunk is ready for processing, it should be given
 * to the sequence analyser which then put it into an internal data structure
 * where it will be analysed for dependencies on already inserted chunks. If
 * there is a dependency, the chunk will not be released for further processing
 * until all its dependencies are completed. A chunk is inactive if it has not
 * been given to further processing.
 */
public interface SequenceAnalyser {

    /**
     * Inserts a chunk in the sequence analyser. While inserting all existing
     * chunks in the sequence analyser that this chunk depends on is found, in
     * order to ensure that these other chunks are released before this chunk
     * can become active.
     * <p>
     * When a chunk is inserted it is inactive.
     * <p>
     * If the chunk depends on other chunks it is a <it>dependent chunk<it>. If
     * the chunk does not depend on other chunks, then it is an <it>independent
     * chunk<it>.
     * <p>
     *
     * @param element A CollisionDetectionElement containing chunk identification 
     * and keys for comparison.
     */
    void addChunk(CollisionDetectionElement element);

    /**
     * Releases all chunks the depends on this ChunkIdentifier, and deletes
     * internal representation of the ChunkIdentifier.
     *
     * @param identifier
     */
    void deleteAndReleaseChunk(ChunkIdentifier identifier);

    /**
     * Returns a list of no more than max independent chunks, which are not already
     * flagged as active. When the list is returned, all the returned ChunkIdentifiers
     * are flagged as active. Only ChunkIdentifiers that - previous to the call
     * - was independent and inactive are returned.
     * @param max maximum number of free chunks to return
     * @return A list of independent ChunkIdentifiers which are now flagged as
     * active.
     */
    List<ChunkIdentifier> getInactiveIndependentChunks(int max);

    /**
     * Number of elements in internal data-structure.
     *
     * @return the number of elements in the internal data structure.
     */
    int size();

    /**
     * Boolean for telling if a given ChunkIdentifier is the first or the
     * top-most ChunkIdentifier in the SequenceAnalyser. This i mostly for
     * monitoring/testing purposes.
     *
     * @param chunkIdentifier
     * @return true if ChunkIdentifier is the first or top-most ChunkIdentifier
     * in the SequenceAnalyser, false otherwise, and false if there are no
     * ChunkIdentifiers in the SequenceAnalyser.
     */
    boolean isHead(ChunkIdentifier chunkIdentifier);
}
