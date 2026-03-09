creatordetector-connector
=========================

A Java client library to the creator-detector service.

### usage

In your Java code

```java
import dk.dbc.dataio.commons.creatordetector.connector.CreatorDetectorConnector;
import dk.dbc.dataio.commons.creatordetector.connector.CreatorNameSuggestions;
import dk.dbc.dataio.commons.creatordetector.connector.DetectCreatorNamesRequest;
import jakarta.inject.Inject;
...

// Assumes environment variable CREATOR_DETECTOR_SERVICE_URL
// is set to the base URL of the creator-detector service.
@Inject
CreatorDetectorConnector connector;

DetectCreatorNamesRequest request = new DetectCreatorNamesRequest("Kim Skotte, journalist", "123456789");
CreatorNameSuggestions suggestions = connector.detectCreatorNames(request);
```
