package dk.dbc.dataio.commons.retriever.connector.model;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ArticlesRequestTest {

    private static final LocalDate FROM_DATE = LocalDate.parse("2026-03-12");
    private static final LocalDate TO_DATE = LocalDate.parse("2026-03-13");

    private static final JSONBContext jsonbContext = createJSONBContext();

    @Test
    void marshallEmptyRequest() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder().build();


        assertThat(jsonbContext.marshall(articlesRequest), is("{ }"));
    }

    @Test
    void marshallWithFromDate() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
                .fromDate(FROM_DATE)
                .build();

        assertThat(jsonbContext.marshall(articlesRequest), is("""
                {
                  "fromDate" : "2026-03-12"
                }"""));
    }

    @Test
    void marshallWithToDate() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
                .toDate(TO_DATE)
                .build();

        assertThat(jsonbContext.marshall(articlesRequest), is("""
                {
                  "toDate" : "2026-03-13"
                }"""));
    }

    @Test
    void marshallWithQuery() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
                .query("\"John Doe\"")
                .build();

        assertThat(jsonbContext.marshall(articlesRequest), is("""
                {
                  "query" : "\\"John Doe\\""
                }"""));
    }

    @Test
    void marshallWithPage() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
                .page(1)
                .build();

        assertThat(jsonbContext.marshall(articlesRequest), is("""
                {
                  "page" : 1
                }"""));
    }

    @Test
    void marshallWithSize() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
                .size(10)
                .build();

        assertThat(jsonbContext.marshall(articlesRequest), is("""
                {
                  "size" : 10
                }"""));
    }

    @Test
    void marshallWithFormatFulltextHtml() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
                .formatFulltextHtml(true)
                .build();

        assertThat(jsonbContext.marshall(articlesRequest), is("""
                {
                  "formatFulltextHtml" : true
                }"""));
    }

    @Test
    void marshall() throws JSONBException {
        ArticlesRequest articlesRequest = new ArticlesRequest.Builder()
                .fromDate(FROM_DATE)
                .toDate(TO_DATE)
                .query("\"John Doe\"")
                .page(1)
                .size(10)
                .formatFulltextHtml(true)
                .build();

        assertThat(jsonbContext.marshall(articlesRequest), is("""
                {
                  "fromDate" : "2026-03-12",
                  "toDate" : "2026-03-13",
                  "query" : "\\"John Doe\\"",
                  "page" : 1,
                  "size" : 10,
                  "formatFulltextHtml" : true
                }"""));
    }

    private static JSONBContext createJSONBContext() {
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.getObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        return jsonbContext;
    }
}