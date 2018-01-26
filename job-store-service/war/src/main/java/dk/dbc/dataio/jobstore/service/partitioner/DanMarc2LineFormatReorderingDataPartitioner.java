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

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.jobstore.types.InvalidDataException;
import dk.dbc.dataio.jobstore.types.PrematureEndOfDataException;
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
     * @param inputStream stream from which data to be partitioned can be read
     * @param specifiedEncoding encoding from job specification (currently only latin 1 is supported).
     * @param reorderer record ordering handler
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

    protected DanMarc2LineFormatReorderingDataPartitioner(InputStream inputStream, String specifiedEncoding, JobItemReorderer reorderer) {
        super(inputStream, specifiedEncoding);
        this.reorderer = reorderer;
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
