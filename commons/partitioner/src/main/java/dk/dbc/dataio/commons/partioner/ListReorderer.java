package dk.dbc.dataio.commons.partioner;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class ListReorderer implements ItemReorderer {
    private final Iterator<DataPartitionerResult> iterator;
    private int size;

    public ListReorderer(List<DataPartitionerResult> list) {
        iterator = list.iterator();
        size = list.size();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Optional<DataPartitionerResult> next(DataPartitionerResult partitionerResult) {
        return Optional.of(iterator.next());
    }

    @Override
    public int getNumberOfItems() {
        return size;
    }

    @Override
    public int getJobId() {
        return 0;
    }
}
