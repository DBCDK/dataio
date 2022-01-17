/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.commons.macroexpansion;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MacroSubstitutorTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private final WeekcodeSupplier weekcodeSupplier = (catalogueCode, localDate) -> {
        switch(catalogueCode) {
            case "EMS":
                return "code4ems";
            case "LEK":
                return "LEK" + localDate.toString();
            default:
                return "unknown";
        }

    };

    @Before
    public void setTimeZone() {
        environmentVariables.set("TZ", "Europe/Copenhagen");
    }

    @Test
    public void replace_addNewVariable() {
        final ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier)
                .add("__TIME_OF_LAST_HARVEST__", timeOfLastHarvest.toInstant().toString());
        assertThat(macroSubstitutor.replace("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]"),
                is("datefield:[2019-01-14T07:00:00Z TO *]"));
    }

    @Test
    public void replace_addNewVariableFromDate() {
        final ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final java.util.Date asDate = Date.from(timeOfLastHarvest.toInstant());
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier)
                .addUTC("__TIME_OF_LAST_HARVEST__", asDate);
        assertThat(macroSubstitutor.replace("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]"),
                is("datefield:[2019-01-14T07:00:00.000000Z TO *]"));
    }

    @Test
    public void replace_now() {
        final ZonedDateTime now = Instant.parse("2019-01-14T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat(macroSubstitutor.replace("datefield:[* TO ${__NOW__}]"),
                is("datefield:[* TO 2019-01-14T07:00Z]"));
    }

    @Test
    public void replace_currentYear() {
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        final ZonedDateTime nowUTC = macroSubstitutor.convertToUtc(Instant.now());
        final String str = macroSubstitutor.replace("datefield:1977 TO ${__CURRENT_YEAR__}]");
        assertThat(str, is("datefield:1977 TO " + nowUTC.getYear() + "]"));
    }

    @Test
    public void replace_previousYear() {
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        final ZonedDateTime nowUTC = macroSubstitutor.convertToUtc(Instant.now());
        final String query = macroSubstitutor.replace("datefield:1977 TO ${__PREVIOUS_YEAR__}]");
        assertThat(query, is("datefield:1977 TO " + (nowUTC.getYear() - 1) + "]"));
    }

    @Test
    public void replace_nonMatching() {
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        assertThat(macroSubstitutor.replace("datefield:[__TIME_OF_LAST_HARVEST__ TO ${__THEN__}]"),
                is("datefield:[__TIME_OF_LAST_HARVEST__ TO ${__THEN__}]"));
    }

    @Test
    public void replace_weekcodes() {
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        assertThat(macroSubstitutor.replace("datefield:[${__WEEKCODE_EMS__} TO ${__WEEKCODE_ABC__}]"),
                is("datefield:[code4ems TO unknown]"));
    }

    @Test
    public void replace_nextweek() {
        final LocalDate localDate = Instant.now().atZone(ZoneId.of(System.getenv("TZ"))).toLocalDate();
        final LocalDate nextWeek = localDate.plusWeeks(1);
        final String expectedQuery = String.format("term.kk:DBF%s%02d",
                nextWeek.getYear(), nextWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));

        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(weekcodeSupplier);
        assertThat(macroSubstitutor.replace("term.kk:${__NEXTWEEK_DBF__}"),
                is(expectedQuery));
    }

    @Test
    public void replace_deferred_period_3_months() {
        final ZonedDateTime now = Instant.parse("2019-01-14T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat(macroSubstitutor.replace("marc.001d:${__DEFERRED_PERIOD_3_MONTHS__}"),
                is("marc.001d:[2018-10-14T00:00:00Z TO 2018-10-14T23:59:59.999999999Z]"));
    }

    @Test
    public void replace_deferred_period_yesterday() {
        final ZonedDateTime now = Instant.parse("2019-01-14T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat(macroSubstitutor.replace("marc.001d:${__DEFERRED_PERIOD_YESTERDAY__}"),
                is("marc.001d:[2019-01-13T00:00:00Z TO 2019-01-13T23:59:59.999999999Z]"));
    }

    @Test
    public void vpSearches() {
        ZonedDateTime now = Instant.parse("2021-04-01T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        MacroSubstitutor macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("1st quarter", macroSubstitutor.replace("${__VPA__}"),
                is("(term.kk:VPA2021* NOT term.kk:VPA202101)"));

        now = Instant.parse("2021-07-01T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("2nd quarter", macroSubstitutor.replace("${__VPA__}"),
                is("(term.kk:VPA2021* NOT term.kk:VPA202101)"));

        now = Instant.parse("2021-10-01T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("3rd quarter", macroSubstitutor.replace("${__VPT__}"),
                is("(term.kk:VPT2021* NOT term.kk:VPT202101)"));

        now = Instant.parse("2022-01-01T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        macroSubstitutor = new MacroSubstitutor(now.toInstant(), weekcodeSupplier);
        assertThat("4th quarter", macroSubstitutor.replace("${__VPT__}"),
                is("(term.kk:VPT2021* NOT term.kk:VPT202101) OR term.kk:VPT202201"));
    }

    @Test
    public void weekCodesPlusMinusTests() {
        ZonedDateTime now = Instant.parse("2021-04-01T07:00:00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
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
}
