retriever-connector
===================

A Java client library to the retriever service public API.
                    
https://port.retriever-info.com/public-api/swagger-ui/index.html
https://retriever.dk

### usage

In your Java code

```java
import dk.dbc.dataio.commons.retriever.connector.RetrieverConnector;
import dk.dbc.dataio.commons.retriever.connector.model.ArticlesRequest;
import dk.dbc.dataio.commons.retriever.connector.model.ArticleResponse;;
import dk.dbc.dataio.commons.retriever.connector.model.ArticlesResponse;
import jakarta.inject.Inject;
...

// Assumes environment variables RETRIEVER_SERVICE_URL and RETRIEVER_SERVICE_API_KEY
// are set to the base URL and API key of the retriever service respectively.
@Inject
RetrieverConnector connector;

ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
        .query("foo bar baz")
        .fromDate(LocalDate.parse("2026-02-01"))
        .toDate(LocalDate.parse("2026-03-31"))
        .build();

ArticlesResponse articlesResponse = connector.searchArticles(articlesRequest);

articlesResponse.articles().forEach(article -> {
    System.out.println(article.get("DOC_ID", String.class));
})
```
