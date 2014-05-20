package dk.dbc.dataio.jobstore.types;

public class ChunkCounter {
    private long total;
    private final ItemResultCounter itemResultCounter;

    public ChunkCounter() {
        total = 0;
        itemResultCounter = new ItemResultCounter();
    }

    public long getTotal() {
        return total;
    }

    public void incrementTotal() {
        total++;
    }

    public ItemResultCounter getItemResultCounter() {
        return itemResultCounter;
    }
}
