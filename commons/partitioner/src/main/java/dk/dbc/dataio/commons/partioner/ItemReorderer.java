package dk.dbc.dataio.commons.partioner;

import java.util.Optional;

public interface ItemReorderer {
    boolean hasNext();
    Optional<DataPartitionerResult> next(DataPartitionerResult partitionerResult);
    int getNumberOfItems();
    default Boolean addCollectionWrapper() {
        return Boolean.FALSE;
    }
    int getJobId();
}
