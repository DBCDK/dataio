CREATE INDEX chunk_timeOfCreationForUnfinishedChunks_index ON chunk(timeOfCreation) WHERE timeOfCompletion IS NULL;
