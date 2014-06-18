package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.ChunkCounter;

public class ChunkCounterBuilder {

    private long total = 0;
    private long success = 0;
    private long failure = 0;
    private long ignore = 0;


    public ChunkCounterBuilder setTotal(long total) {
        this.total = total;
        return this;
    }

    public ChunkCounterBuilder setSuccess(long success){
        this.success = success;
        return this;
    }

    public ChunkCounterBuilder setFailure(long failure){
        this.failure = failure;
        return this;
    }

    public ChunkCounterBuilder setIgnore(long ignore){
        this.ignore = ignore;
        return this;
    }

    private void incrementTotal(ChunkCounter chunkCounter, long numberOfIncrements){
        for(int i = 0; i < numberOfIncrements; i++){
            chunkCounter.incrementTotal();
        }
    }

    private void incrementSuccess(ChunkCounter chunkCounter, long numberOfIncrements){
        for(int i = 0; i < numberOfIncrements; i++){
            chunkCounter.getItemResultCounter().incrementSuccess();
        }
    }

    private void incrementFailure(ChunkCounter chunkCounter, long numberOfIncrements){
        for(int i = 0; i < numberOfIncrements; i++){
            chunkCounter.getItemResultCounter().incrementFailure();
        }
    }

    private void incrementIgnore(ChunkCounter chunkCounter, long numberOfIncrements){
        for(int i = 0; i < numberOfIncrements; i++){
            chunkCounter.getItemResultCounter().incrementIgnore();
        }
    }

    public ChunkCounter build() {
        ChunkCounter chunkCounter = new ChunkCounter();
        incrementTotal(chunkCounter, total);
        incrementSuccess(chunkCounter, success);
        incrementFailure(chunkCounter, failure);
        incrementIgnore(chunkCounter, ignore);
        return chunkCounter;
    }
}
