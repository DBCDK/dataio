# DataIO Infomedia harvester

## Purpose
- Fetches Infomedia articles from the external retriever service.
- Augments the harvested articles with creator name suggestions from the internal creator-detector service.

# Auxiliary component paths
| Component                 | Path                              | Notes      |
|---------------------------|-----------------------------------|------------|
| creatordetector-connector | commons/creatordetector-connector | Client lib |
| harvester-framework       | harvester/framework               |            |
| retriever-connector       | commons/retriever-connector       | Client lib |
