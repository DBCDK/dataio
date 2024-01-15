package dk.dbc.dataio.commons.partioner;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.marc.writer.MarcXchangeV1Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;

/**
 * Data partitioner for streams of DanMarc2 line format records with on-the-fly re-ordering of records
 */
public class DanMarc2LineFormatReorderingDataPartitioner extends DanMarc2LineFormatDataPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanMarc2LineFormatReorderingDataPartitioner.class);
    private final JobItemReorderer reorderer;

    /**
     * Creates new instance of DanMarc2 LineFormat re-ordering DataPartitioner
     *
     * @param inputStream       stream from which data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @param reorderer         record ordering handler
     * @return new instance of data partitioner
     * @throws NullPointerException     if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     */
    public static DanMarc2LineFormatReorderingDataPartitioner newInstance(InputStream inputStream, String specifiedEncoding, JobItemReorderer reorderer)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullOrThrow(inputStream, "inputStream");
        InvariantUtil.checkNotNullNotEmptyOrThrow(specifiedEncoding, "specifiedEncoding");
        InvariantUtil.checkNotNullOrThrow(reorderer, "reorderer");
        return new DanMarc2LineFormatReorderingDataPartitioner(inputStream, specifiedEncoding, reorderer);
    }

    public JobItemReorderer getReorderer() {
        return reorderer;
    }

    protected DanMarc2LineFormatReorderingDataPartitioner(InputStream inputStream, String specifiedEncoding, JobItemReorderer reorderer) {
        super(inputStream, specifiedEncoding);
        this.reorderer = reorderer;
        this.marcWriter.setProperty(MarcXchangeV1Writer.Property.ADD_COLLECTION_WRAPPER,
                reorderer.addCollectionWrapper());
    }

    @Override
    public void drainItems(int itemsToRemove) {
        if (itemsToRemove < 0) throw new IllegalArgumentException("Unable to drain a negative number of items");
        itemsToRemove += reorderer.getNumberOfItems();
        while (--itemsToRemove >= 0) {
            try {
                // super to avoid adding items to the reordering scratchpad again
                super.nextDataPartitionerResult();
            } catch (PrematureEndOfDataException e) {
                throw e;    // to potentially trigger a retry
            } catch (Exception e) {
                // we simply swallow these as they have already been handled in chunk items
                LOGGER.trace("Swallowed exception during drain", e);
                positionInDatafile++;
            }
        }
    }

    @Override
    protected boolean hasNextDataPartitionerResult() throws PrematureEndOfDataException {
        // return true if either input stream or reorder handler instance has remaining records
        return super.hasNextDataPartitionerResult() || reorderer.hasNext();
    }

    @Override
    protected DataPartitionerResult nextDataPartitionerResult() throws InvalidDataException {
        DataPartitionerResult result;
        boolean needToExamineMoreResults;
        do {
            result = super.nextDataPartitionerResult();
            if (result.isEmpty() || result.getChunkItem().getStatus() == ChunkItem.Status.SUCCESS) {
                // Either input stream has run out of results or result contains chunk item with
                // status SUCCESS and needs to undergo potential re-ordering
                final Optional<DataPartitionerResult> reorderedResult;
                try {
                    reorderedResult = reorderer.next(result);
                    if (reorderedResult.isPresent() && reorderedResult.get().isEmpty()) {
                        // Result from reorder handler is present but empty,
                        // therefore one more result (if any exists) needs to be consumed from input stream
                        needToExamineMoreResults = true;
                    } else {
                        // Return result from reorder handler
                        // or empty result if reorder handler signalled no-more-results (isPresent() == false)
                        result = reorderedResult.orElse(DataPartitionerResult.EMPTY);
                        needToExamineMoreResults = false;
                    }
                } catch (RuntimeException e) {
                    final String message = String.format(
                            "Something unexpected happened during re-ordering of job %s", reorderer.getJobId());
                    LOGGER.error(message, e);
                    throw new InvalidDataException(e);
                }
            } else {
                needToExamineMoreResults = false;
            }

        } while (needToExamineMoreResults);

        return result;
    }
}
