package dk.dbc.dataio.commons.creatordetector.connector;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CreatorNameSuggestionsTest {

    private final JSONBContext jsonBContext = new JSONBContext();

    @Test
    void unmarshalling() throws JSONBException {
        final String json = """
            {
              "results": [
                {
                  "authority_id": "870979:134881968",
                  "detected_ner_name": "emil jensen",
                  "authority_name_normalized": "emil jensen",
                  "match_score": 0.873639702796936,
                  "rerank_score": 0.6931471805599453
                },
                {
                  "authority_id": "870979:141352326",
                  "detected_ner_name": "rene juul",
                  "authority_name_normalized": "rené juul",
                  "match_score": 0.06517253071069717,
                  "rerank_score": 0
                },
                {
                  "authority_id": null,
                  "detected_ner_name": "heltogtotaltukendt rasmussen",
                  "authority_name_normalized": null,
                  "match_score": 0.873639702796936,
                  "rerank_score": 0
                }
              ]
            }
        """;

        final CreatorNameSuggestions expectedCreatorNameSuggestions = new CreatorNameSuggestions();
        final CreatorNameSuggestion expectedCreatorNameSuggestion1 = new CreatorNameSuggestion(
                "emil jensen", "870979:134881968", "emil jensen", 0.873639702796936, 0.6931471805599453);
        final CreatorNameSuggestion expectedCreatorNameSuggestion2 = new CreatorNameSuggestion(
                "rene juul", "870979:141352326", "rené juul", 0.06517253071069717, 0);
        final CreatorNameSuggestion expectedCreatorNameSuggestion3 = new CreatorNameSuggestion(
                "heltogtotaltukendt rasmussen", null, null, 0.873639702796936, 0);
        expectedCreatorNameSuggestions.setResults(List.of(
                expectedCreatorNameSuggestion1,
                expectedCreatorNameSuggestion2,
                expectedCreatorNameSuggestion3));

        final CreatorNameSuggestions creatorNameSuggestions = jsonBContext.unmarshall(json, CreatorNameSuggestions.class);
        assertThat(creatorNameSuggestions, is(expectedCreatorNameSuggestions));

        System.out.println(jsonBContext.marshall(creatorNameSuggestions));
    }
}
