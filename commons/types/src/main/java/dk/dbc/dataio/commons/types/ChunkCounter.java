package dk.dbc.dataio.commons.types;

import java.io.Serializable;

public class ChunkCounter implements Serializable {
    private static final long serialVersionUID = 4376125910754399339L;

    private long total;
    private ItemResultCounter itemResultCounter;

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
