package dk.dbc.dataio.commons.creatordetector.connector;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class CreatorNameSuggestionsTest {

    private final JSONBContext jsonBContext = new JSONBContext();

    @Test
    void unmarshalling() throws JSONBException {
        final String json = """
            {
              "results": {
                "kim skotte": [
                  [
                    "870979:68943574",
                    "kim skotte",
                    0.873639702796936,
                    7.805474625270857
                  ]
                ],
                "kiri kim lassen": [
                  [
                    "870979:19253007",
                    "kiri kim lassen",
                    0.873639702796936,
                    5.407171771460119
                  ]
                ]
              }
            }
        """;

        final CreatorNameSuggestions expectedCreatorNameSuggestions = new CreatorNameSuggestions();
        expectedCreatorNameSuggestions.setResults(new LinkedHashMap<>() {{
            put("kim skotte", List.of(
                    new CreatorNameSuggestion(List.of("870979:68943574", "kim skotte", 0.873639702796936, 7.805474625270857))));
            put("kiri kim lassen", List.of(
                    new CreatorNameSuggestion(List.of("870979:19253007", "kiri kim lassen", 0.873639702796936, 5.407171771460119))));
        }});

        final CreatorNameSuggestions creatorNameSuggestions = jsonBContext.unmarshall(json, CreatorNameSuggestions.class);
        assertThat(creatorNameSuggestions, is(expectedCreatorNameSuggestions));
    }
}
