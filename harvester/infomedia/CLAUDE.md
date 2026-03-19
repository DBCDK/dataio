# DataIO Infomedia harvester

## Purpose

This harvester fetches Infomedia articles from the external retriever service.
It also augments the harvested articles with creator name suggestions from the internal creator-detector service.

# Component paths
| Component                 | Path                              | Notes      |
|---------------------------|-----------------------------------|------------|
| retreiver-connector       | commons/retriever-connector       | Client lib |
| creatordetector-connector | commons/creatordetector-connector | Client lib |