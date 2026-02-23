package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SubmitterFilterTest {

    private static final Long SUBMITTER_111111 = 111111L;
    private static final Long SUBMITTER_222222 = 222222L;
    private static final Long SUBMITTER_333333 = 333333L;

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    @Test
    void constructor_typeCannotBeNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new SubmitterFilter(null, Collections.singletonList(SUBMITTER_111111)));

        assertThat(e.getMessage(), is("type cannot be null"));
    }

    @Test
    void constructor_submitterNumbersCannotBeNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new SubmitterFilter(SubmitterFilter.Type.ACCEPT_ALL_EXCEPT, null));

        assertThat(e.getMessage(), is("submitterNumbers cannot be null"));
    }

    @Test
    void constructor_submitterNumbersCannotBeEmpty() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new SubmitterFilter(SubmitterFilter.Type.ACCEPT_ALL_EXCEPT, Collections.emptyList()));

        assertThat(e.getMessage(), is("submitterNumbers cannot be empty"));
    }

    // -------------------------------------------------------------------------
    // Getters & immutability
    // -------------------------------------------------------------------------

    @Test
    void getType_returnsConstructorType() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.SKIP_ALL_EXCEPT,
                Collections.singletonList(SUBMITTER_111111)
        );

        assertThat(filter.getType(), is(SubmitterFilter.Type.SKIP_ALL_EXCEPT));
    }

    @Test
    void getSubmitterNumbers_isUnmodifiable() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.ACCEPT_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        Set<Long> numbers = filter.getSubmitterNumbers();
        assertThat(numbers, containsInAnyOrder(SUBMITTER_111111, SUBMITTER_222222));

        assertThrows(UnsupportedOperationException.class, () -> numbers.add(100L));
    }

    @Test
    void constructor_copiesInputList_defensiveCopy() {
        ArrayList<Long> input = new ArrayList<>(Arrays.asList(SUBMITTER_111111, SUBMITTER_222222));
        SubmitterFilter filter = new SubmitterFilter(SubmitterFilter.Type.ACCEPT_ALL_EXCEPT, input);

        input.clear();

        assertThat(filter.getSubmitterNumbers(), is(new HashSet<>(Arrays.asList(SUBMITTER_111111, SUBMITTER_222222))));
    }

    // -------------------------------------------------------------------------
    // shouldSkip / shouldAccept semantics
    // -------------------------------------------------------------------------

    @Test
    void acceptAllExcept_shouldSkip_isTrueOnlyForNumbersInList() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.ACCEPT_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        assertThat(filter.shouldSkip(SUBMITTER_111111), is(true));
        assertThat(filter.shouldSkip(SUBMITTER_222222), is(true));
        assertThat(filter.shouldSkip(SUBMITTER_333333), is(false));
    }

    @Test
    void acceptAllExcept_shouldAccept_isFalseOnlyForNumbersInList() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.ACCEPT_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        assertThat(filter.shouldAccept(SUBMITTER_111111), is(false));
        assertThat(filter.shouldAccept(SUBMITTER_222222), is(false));
        assertThat(filter.shouldAccept(SUBMITTER_333333), is(true));
    }

    @Test
    void skipAllExcept_shouldSkip_isFalseOnlyForNumbersInList() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.SKIP_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        assertThat(filter.shouldSkip(SUBMITTER_111111), is(false));
        assertThat(filter.shouldSkip(SUBMITTER_222222), is(false));
        assertThat(filter.shouldSkip(SUBMITTER_333333), is(true));
    }

    @Test
    void skipAllExcept_shouldAccept_isTrueOnlyForNumbersInList() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.SKIP_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        assertThat(filter.shouldAccept(SUBMITTER_111111), is(true));
        assertThat(filter.shouldAccept(SUBMITTER_222222), is(true));
        assertThat(filter.shouldAccept(SUBMITTER_333333), is(false));
    }

    // -------------------------------------------------------------------------
    // Predicate views (acceptPredicate / skipPredicate)
    // -------------------------------------------------------------------------

    @Test
    void acceptPredicate_delegatesToShouldAccept() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.ACCEPT_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        Predicate<Long> p = filter.acceptPredicate();

        assertThat(p.test(SUBMITTER_111111), is(filter.shouldAccept(SUBMITTER_111111)));
        assertThat(p.test(SUBMITTER_222222), is(filter.shouldAccept(SUBMITTER_222222)));
        assertThat(p.test(SUBMITTER_333333), is(filter.shouldAccept(SUBMITTER_333333)));
    }

    @Test
    void skipPredicate_delegatesToShouldSkip() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.SKIP_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        Predicate<Long> p = filter.skipPredicate();

        assertThat(p.test(SUBMITTER_111111), is(filter.shouldSkip(SUBMITTER_111111)));
        assertThat(p.test(SUBMITTER_222222), is(filter.shouldSkip(SUBMITTER_222222)));
        assertThat(p.test(SUBMITTER_333333), is(filter.shouldSkip(SUBMITTER_333333)));
    }

    @Test
    void acceptPredicate_canBeUsedWithStreams() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.SKIP_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        assertThat(
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222, SUBMITTER_333333).stream()
                        .filter(filter.acceptPredicate())
                        .toList(),
                containsInAnyOrder(SUBMITTER_111111, SUBMITTER_222222)
        );
    }

    @Test
    void skipPredicate_canBeUsedWithStreams() {
        SubmitterFilter filter = new SubmitterFilter(
                SubmitterFilter.Type.ACCEPT_ALL_EXCEPT,
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222)
        );

        assertThat(
                Arrays.asList(SUBMITTER_111111, SUBMITTER_222222, SUBMITTER_333333).stream()
                        .filter(filter.skipPredicate())
                        .toList(),
                containsInAnyOrder(SUBMITTER_111111, SUBMITTER_222222)
        );
    }

    // -------------------------------------------------------------------------
    // Jackson mapping (verifies @JsonCreator / @JsonProperty wiring)
    // -------------------------------------------------------------------------

    @Test
    void jacksonDeserialize() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{ \"type\": \"ACCEPT_ALL_EXCEPT\", \"submitterNumber\": [424242, 434343] }";

        SubmitterFilter filter = mapper.readValue(json, SubmitterFilter.class);

        assertThat(filter.getType(), is(SubmitterFilter.Type.ACCEPT_ALL_EXCEPT));
        assertThat(filter.getSubmitterNumbers(), containsInAnyOrder(424242L, 434343L));
        assertThat(filter.shouldSkip(424242L), is(true));
        assertThat(filter.shouldSkip(999999L), is(false));
    }
}
