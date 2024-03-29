package dk.dbc.dataio.commons.macroexpansion;

import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

import static dk.dbc.dataio.commons.macroexpansion.MacroSubstitutor.TZ;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MacroSubstitutorTest {
    private final WeekcodeSupplier weekcodeSupplier = (catalogueCode, localDate) -> {
        switch (catalogueCode) {
            case "EMS":
                return "code4ems";
            case "LEK":
                return "LEK" + localDate.toString();
            default:
                return "unknown";
        }

    };

    @Test
    public void replace_addNewVariable() {
        ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00Z").atZone(TZ);
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier).add("__TIME_OF_LAST_HARVEST__", timeOfLastHarvest.toInstant().toString());
        assertThat(macroSubstitutor.replace("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]"), is("datefield:[2019-01-14T07:00:00Z TO *]"));
    }

    @Test
    public void replace_addNewVariableFromDate() {
        ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00Z").atZone(TZ);
        java.util.Date asDate = Date.from(timeOfLastHarvest.toInstant());
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier).addUTC("__TIME_OF_LAST_HARVEST__", asDate);
        assertThat(macroSubstitutor.replace("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]"), is("datefield:[2019-01-14T07:00:00.000000Z TO *]"));
    }

    @Test
    public void replace_now() {
        ZonedDateTime now = Instant.parse("2019-01-14T07:00:00Z").atZone(TZ);
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat(macroSubstitutor.replace("datefield:[* TO ${__NOW__}]"), is("datefield:[* TO 2019-01-14T07:00Z]"));
    }

    @Test
    public void replace_currentYear() {
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        ZonedDateTime nowUTC = macroSubstitutor.convertToUtc(Instant.now());
        String str = macroSubstitutor.replace("datefield:1977 TO ${__CURRENT_YEAR__}]");
        assertThat(str, is("datefield:1977 TO " + nowUTC.getYear() + "]"));
    }

    @Test
    public void replace_previousYear() {
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        ZonedDateTime nowUTC = macroSubstitutor.convertToUtc(Instant.now());
        String query = macroSubstitutor.replace("datefield:1977 TO ${__PREVIOUS_YEAR__}]");
        assertThat(query, is("datefield:1977 TO " + (nowUTC.getYear() - 1) + "]"));
    }

    @Test
    public void replace_nonMatching() {
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        assertThat(macroSubstitutor.replace("datefield:[__TIME_OF_LAST_HARVEST__ TO ${__THEN__}]"),
                is("datefield:[__TIME_OF_LAST_HARVEST__ TO ${__THEN__}]"));
    }

    @Test
    public void replace_weekcodes() {
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        assertThat(macroSubstitutor.replace("datefield:[${__WEEKCODE_EMS__} TO ${__WEEKCODE_ABC__}]"),
                is("datefield:[code4ems TO unknown]"));
    }

    @Test
    public void replace_nextweek() {
        LocalDate localDate = Instant.now().atZone(TZ).toLocalDate();
        LocalDate nextWeek = localDate.plusWeeks(1);
        String expectedQuery = String.format("term.kk:DBF%s%02d",
                nextWeek.getYear(), nextWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));

        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        assertThat(macroSubstitutor.replace("term.kk:${__NEXTWEEK_DBF__}"),
                is(expectedQuery));
    }

    @Test
    public void replace_deferred_period_3_months() {
        ZonedDateTime now = Instant.parse("2019-01-14T07:00:00Z").atZone(TZ);
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat(macroSubstitutor.replace("marc.001d:${__DEFERRED_PERIOD_3_MONTHS__}"),
                is("marc.001d:[2018-10-14T00:00:00Z TO 2018-10-14T23:59:59.999999999Z]"));
    }

    @Test
    public void replace_deferred_period_yesterday() {
        ZonedDateTime now = Instant.parse("2019-01-14T07:00:00Z").atZone(TZ);
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat(macroSubstitutor.replace("marc.001d:${__DEFERRED_PERIOD_YESTERDAY__}"),
                is("marc.001d:[2019-01-13T00:00:00Z TO 2019-01-13T23:59:59.999999999Z]"));
    }

    @Test
    public void vpSearches() {
        ZonedDateTime now = Instant.parse("2021-04-01T07:00:00Z").atZone(TZ);
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("1st quarter", macroSubstitutor.replace("${__VPA__}"),
                is("(term.kk:VPA2021* NOT term.kk:VPA202101)"));

        now = Instant.parse("2021-07-01T07:00:00Z").atZone(TZ);
        macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("2nd quarter", macroSubstitutor.replace("${__VPA__}"),
                is("(term.kk:VPA2021* NOT term.kk:VPA202101)"));

        now = Instant.parse("2021-10-01T07:00:00Z").atZone(TZ);
        macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("3rd quarter", macroSubstitutor.replace("${__VPT__}"),
                is("(term.kk:VPT2021* NOT term.kk:VPT202101)"));

        now = Instant.parse("2022-01-01T07:00:00Z").atZone(TZ);
        macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("4th quarter", macroSubstitutor.replace("${__VPT__}"),
                is("(term.kk:VPT2021* NOT term.kk:VPT202101) OR term.kk:VPT202201"));
    }

    @Test
    public void weekCodesPlusMinusTests() {
        ZonedDateTime now = Instant.parse("2021-04-01T07:00:00Z").atZone(TZ);
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("Minus 10 weeks", macroSubstitutor.replace("term.kk:${__WEEKCODE_LEK_MINUS_10__}"),
                is("term.kk:LEK2021-01-21"));
        assertThat("Plus 10 weeks", macroSubstitutor.replace("term.kk:${__WEEKCODE_LEK_PLUS_10__}"),
                is("term.kk:LEK2021-06-10"));

        // Weird edge cases - this probably shouldn't work, but it does.
        assertThat("Plus 0 weeks", macroSubstitutor.replace("term.kk:${__WEEKCODE_LEK_PLUS_0__}"),
                is("term.kk:LEK2021-04-01"));
        assertThat("Plus -10 weeks", macroSubstitutor.replace("term.kk:${__WEEKCODE_LEK_PLUS_-10__}"),
                is("term.kk:LEK2021-01-21"));
    }

    @Test
    public void daysPlusMinusTests() {
        ZonedDateTime now = Instant.parse("2021-04-01T07:00:00Z").atZone(TZ);
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("Minus 10 days", macroSubstitutor.replace("${__NOW_MINUS_10__}"),
                is("2021-03-22T00:00:00Z"));
        assertThat("Minus 10 days interval", macroSubstitutor.replace("term.aj:${__NOW_BACK_MINUS_10__}"),
                is("term.aj:[2021-03-22T00:00:00Z TO 2021-04-01T23:59:59.999999999Z]"));

        // Weird edge cases - this probably shouldn't work, but it does.
        assertThat("Minus 0 days", macroSubstitutor.replace("${__NOW_MINUS_0__}"),
                is("2021-04-01T00:00:00Z"));
        assertThat("Minus 0 days interval", macroSubstitutor.replace("term.aj:${__NOW_BACK_MINUS_0__}"),
                is("term.aj:[2021-04-01T00:00:00Z TO 2021-04-01T23:59:59.999999999Z]"));
        assertThat("Minus -10 days", macroSubstitutor.replace("${__NOW_MINUS_-10__}"),
                is("2021-04-11T00:00:00Z"));
        // This will probably make solr pretty angry - I don't think it likes reverse date intervals
        assertThat("Minus -10 days interval", macroSubstitutor.replace("term.aj:${__NOW_BACK_MINUS_-10__}"),
                is("term.aj:[2021-04-11T00:00:00Z TO 2021-04-01T23:59:59.999999999Z]"));
    }

}
