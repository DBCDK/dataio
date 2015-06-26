package dk.dbc.dataio.sequenceanalyser;

import java.util.List;

/**
 * The sequence analyser is a mostly a data structure for finding dependencies
 * between similar elements. Whenever an element is ready for processing, it should
 * be given to the sequence analyser which will then put it into an internal data
 * structure where it will be analysed for dependencies on already inserted elements.
 * If there is a dependency, the element will not be released for further processing
 * until all its dependencies are completed. An element is inactive if it has not
 * been released for further processing.
 */
public interface SequenceAnalyser {
    /**
     * Inserts an element into the sequence analyser. During the insert operation
     * all existing elements that this new element depends on are found in
     * order to ensure that these other elements are released before this element
     * can become active.
     * <p>
     * When an element is inserted it is inactive.
     * <p>
     * If the element depends on other elements it is a &lt;it&gt;dependent element&lt;it&gt;. If
     * the element does not depend on other elements, then it is an &lt;it&gt;independent
     * element &lt;it&gt;.
     * <p>
     * @param element A CollisionDetectionElement containing element identification
     * and keys for comparison.
     */
    void add(CollisionDetectionElement element);

    /**
     * Releases all elements having a dependency on the element specified by given
     * identifier and removes the corresponding element from the internal data structure
     * @return number of consumed slots held by deleted element
     */
    int deleteAndRelease(CollisionDetectionElementIdentifier identifier);

    /**
     * Returns a list of independent and inactive elements consuming no more than
     * maxSlotsSoftLimit slots combined. When the list is returned, all the returned
     * elements are flagged as active.
     * @param maxSlots maximum number of slots consumed by all released elements combined
     * @return A list of independent elements which are now flagged as active
     */
    List<CollisionDetectionElement> getInactiveIndependent(int maxSlots);

    /**
     * Number of elements in the sequence analyser
     * @return the number of elements in the internal data structure.
     */
    int size();

    /**
     * Boolean for telling if a given identifier represents the first (or the
     * top-most) element in the SequenceAnalyser. This is mostly for
     * monitoring/testing purposes.
     * @return true if identifier represents the first (or top-most) element
     * in the SequenceAnalyser, false otherwise. Also returns false if there are no
     * elements in the SequenceAnalyser.
     */
    boolean isHead(CollisionDetectionElementIdentifier identifier);
}
