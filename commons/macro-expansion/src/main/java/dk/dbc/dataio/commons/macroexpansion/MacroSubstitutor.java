package dk.dbc.dataio.commons.macroexpansion;

import org.apache.commons.text.StringSubstitutor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Long.parseLong;

/**
 * This class takes a piece of text and substitutes macro variables within it
 */
public class MacroSubstitutor {
    private static final Pattern NEXTWEEK_PATTERN = Pattern.compile("\\$\\{__NEXTWEEK_(.+?)__\\}");
    private static final Pattern WEEKCODE_PATTERN = Pattern.compile("\\$\\{__WEEKCODE_([^_]+?)__\\}");
    private static final Pattern WEEKCODE_PATTERN_MINUS = Pattern.compile("\\$\\{__WEEKCODE_(.+?)_MINUS_(.+?)__\\}");
    private static final Pattern WEEKCODE_PATTERN_PLUS = Pattern.compile("\\$\\{__WEEKCODE_(.+?)_PLUS_(.+?)__\\}");
    private static final Pattern WEEK_PATTERN_MINUS = Pattern.compile("\\$\\{__WEEK_MINUS_(.+?)__\\}");
    private static final Pattern WEEK_PATTERN_PLUS = Pattern.compile("\\$\\{__WEEK_PLUS_(.+?)__\\}");
    private static final Pattern NOW_PATTERN_MINUS = Pattern.compile("\\$\\{__NOW_MINUS_(.+?)__\\}");
    private static final Pattern NOW_PATTERN_MINUS_TO_TODAY = Pattern.compile("\\$\\{__NOW_BACK_MINUS_(.+?)__\\}");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSX");

    private final Map<String, String> substitutions;
    private final Instant now;
    private final WeekcodeSupplier weekcodeSupplier;

    private final ZoneId tz = ZoneId.of(System.getenv("TZ"));

    public MacroSubstitutor(WeekcodeSupplier weekcodeSupplier) {
        this(Instant.now(), weekcodeSupplier);
    }

    public MacroSubstitutor(Instant now, WeekcodeSupplier weekcodeSupplier) {
        substitutions = new HashMap<>();
        this.now = now;
        this.weekcodeSupplier = weekcodeSupplier;
        initialize();
    }

    private void initialize() {
        final ZonedDateTime nowUTC = convertToUtc(now);
        substitutions.put("__NOW__", nowUTC.toString());
        substitutions.put("__CURRENT_YEAR__", String.valueOf(nowUTC.getYear()));
        substitutions.put("__PREVIOUS_YEAR__", String.valueOf(nowUTC.getYear() - 1));
        substitutions.put("__DEFERRED_PERIOD_3_MONTHS__",
                getWholeDayDateRange(nowUTC.minusMonths(3)));
        substitutions.put("__DEFERRED_PERIOD_YESTERDAY__",
                getWholeDayDateRange(nowUTC.minusDays(1)));
        substitutions.put("__VPA__", getVPSearchYear("VPA", nowUTC));
        substitutions.put("__VPT__", getVPSearchYear("VPT", nowUTC));
    }

    public MacroSubstitutor add(String key, String value) {
        substitutions.put(key, value);
        return this;
    }

    public MacroSubstitutor addUTC(String key, Date value) {
        if (value != null) {
            addUTC(key, value.toInstant());
        }
        return this;
    }

    public MacroSubstitutor addUTC(String key, Instant value) {
        if (value != null) {
            substitutions.put(key, convertToUtc(value).format(TIMESTAMP_FORMATTER));
        }
        return this;
    }

    /**
     * Replaces variables within the given string
     * <p>
     * Known variables are:
     * </p>
     * <p>
     * ${__CURRENT_YEAR__}
     * := year based on the instantiation time for this
     * object as Coordinated Universal Time (UTC).
     * </p>
     * <p>
     * ${__PREVIOUS_YEAR__}
     * := previous year based on the instantiation time
     * for this object as Coordinated Universal Time (UTC).
     * </p>
     * <p>
     * ${__NOW__}
     * := time of instantiation for this object as
     * Coordinated Universal Time (UTC) string.
     * </p>
     * <p>
     * ${__WEEKCODE_[CATALOGUE]__}
     * := weekcode as string for the given CATALOGUE
     * in relation to the current local date,
     * e.g. ${__WEEKCODE_EMS__}.
     * </p>
     * <p>
     * ${__WEEKCODE_[CATALOGUE]_MINUS_[WEEKS]__}
     * := weekcode as string for the given CATALOGUE
     * in relations to the current local date minus WEEKS,
     * e.g ${__WEEKCODE_EMS_MINUS_10__}.
     * </p>
     * <p>
     * ${__WEEKCODE_[CATALOGUE]_PLUS_[WEEKS]__}
     * := weekcode as string for the given CATALOGUE
     * in relations to the current local date plus WEEKS,
     * e.g ${__WEEKCODE_EMS_PLUS_10__}.
     * </p>
     * <p>
     * ${__NEXTWEEK_[CATALOGUE]__}
     * := weekcode for next week as string for the given CATALOGUE
     * in relation to the current local date,
     * e.g. ${__NEXTWEEK_DBF__}
     * </p>
     * <p>
     * ${__DEFERRED_PERIOD_3_MONTHS__}
     * := datetime range matching the date of a deferred period
     * of three months back in time relative to the instantiation
     * time for this object.
     * </p>
     * <p>
     * ${__DEFERRED_PERIOD_YESTERDAY__}
     * := datetime range matching the date of yesterday
     * relative to the instantiation time for this object.
     * </p>
     * <p>
     * ${__NOW_MINUS_[DAYS]__}
     * := date as string for today minus the specified number of days
     * </p>
     * <p>
     * ${__NOW_BACK_MINUS_[DAYS]__}
     * := date interval as string for today to today minus the specified number of days
     * </p>
     * <p>
     * ${__VPA__}
     * := VPA query
     * </p>
     * <p>
     * ${__VPT__}
     * := VPT query
     * </p>
     *
     * @param str string on which to do variable substitution
     * @return result of the replace operation with all occurrences of known variables replaced
     */
    public String replace(String str) {
        setVariablesForCatalogueCodes(str);
        setVariablesForModifiedDateStamps(str);
        setVariablesForWeeks(str);
        return new StringSubstitutor(substitutions).replace(str);
    }

    /**
     * @return value used for the __NOW__ variable
     */
    public Date getNow() {
        return Date.from(now);
    }

    ZonedDateTime convertToUtc(Instant instant) {
        final ZonedDateTime zonedDateTime = instant.atZone(tz);
        return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
    }

    private void setVariablesForModifiedDateStamps(String str) {
        final ZonedDateTime nowUTC = convertToUtc(now);
        getDateStampsToResolve(str, NOW_PATTERN_MINUS).forEach(dateStamp -> substitutions.computeIfAbsent(
                String.format("__NOW_MINUS_%s__", dateStamp),
                key -> String.format("%s", getStartOfDate(nowUTC.minusDays(parseLong(dateStamp))))));
        getDateStampsToResolve(str, NOW_PATTERN_MINUS_TO_TODAY).forEach(dateStamp -> substitutions.computeIfAbsent(
                String.format("__NOW_BACK_MINUS_%s__", dateStamp),
                key -> String.format("%s", getDateInterval(nowUTC.minusDays(parseLong(dateStamp)), nowUTC))));
    }

    private void setVariablesForWeeks(String str) {
        final ZonedDateTime nowUTC = convertToUtc(now);
        substitutions.put("__WEEK__", getYearAndWeek(nowUTC));
        getDateStampsToResolve(str, WEEK_PATTERN_MINUS).forEach(dateStamp -> substitutions.computeIfAbsent(
                String.format("__WEEK_MINUS_%s__", dateStamp),
                key -> String.format("%s", getYearAndWeek(nowUTC.minusWeeks(parseLong(dateStamp))))));
        getDateStampsToResolve(str, WEEK_PATTERN_PLUS).forEach(dateStamp -> substitutions.computeIfAbsent(
                String.format("__WEEK_PLUS_%s__", dateStamp),
                key -> String.format("%s", getYearAndWeek(nowUTC.minusWeeks(parseLong(dateStamp))))));
    }

    private List<String> getDateStampsToResolve(String str, Pattern pattern) {
        final List<String> dateStamps = new ArrayList<>();
        final Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            dateStamps.add(matcher.group(1));
        }
        return dateStamps;
    }

    private void setVariablesForCatalogueCodes(String str) {
        final LocalDate localDate = now.atZone(tz).toLocalDate();
        final LocalDate nextWeek = localDate.plusWeeks(1);
        getCatalogueCodesToResolve(str, NEXTWEEK_PATTERN).forEach(catalogueCode -> substitutions.computeIfAbsent(
                String.format("__NEXTWEEK_%s__", catalogueCode), key -> String.format("%s%s%02d",
                        catalogueCode.toUpperCase(), nextWeek.getYear(), nextWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))));
        if (weekcodeSupplier != null) {
            getCatalogueCodesToResolve(str, WEEKCODE_PATTERN).forEach(catalogueCode -> substitutions.computeIfAbsent(
                    String.format("__WEEKCODE_%s__", catalogueCode),
                    key -> weekcodeSupplier.get(catalogueCode, localDate)));

            getCatalogueCodesAndOffsetToResolve(str, WEEKCODE_PATTERN_MINUS).forEach(pair -> substitutions.computeIfAbsent(
                    String.format("__WEEKCODE_%s_MINUS_%s__", pair.getWeekCode(), pair.getOffset()),
                    key -> weekcodeSupplier.get(pair.getWeekCode(), localDate.minusWeeks(pair.getOffset()))
            ));

            getCatalogueCodesAndOffsetToResolve(str, WEEKCODE_PATTERN_PLUS).forEach(pair -> substitutions.computeIfAbsent(
                    String.format("__WEEKCODE_%s_PLUS_%s__", pair.getWeekCode(), pair.getOffset()),
                    key -> weekcodeSupplier.get(pair.getWeekCode(), localDate.plusWeeks(pair.getOffset()))
            ));
        }
    }

    private static class WeekCodeOffSetPair {
        private final String weekCode;
        private final int offset;

        public WeekCodeOffSetPair(String weekCode, int offset) {
            this.weekCode = weekCode;
            this.offset = offset;
        }

        public String getWeekCode() {
            return weekCode;
        }

        public int getOffset() {
            return offset;
        }
    }

    private List<WeekCodeOffSetPair> getCatalogueCodesAndOffsetToResolve(String str, Pattern pattern) {
        final List<WeekCodeOffSetPair> catalogueCodes = new ArrayList<>();
        final Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            final String weekCode = matcher.group(1);
            final int offset = Integer.parseInt(matcher.group(2));
            catalogueCodes.add(new WeekCodeOffSetPair(weekCode, offset));
        }
        return catalogueCodes;
    }

    private List<String> getCatalogueCodesToResolve(String str, Pattern pattern) {
        final List<String> catalogueCodes = new ArrayList<>();
        final Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            catalogueCodes.add(matcher.group(1));
        }
        return catalogueCodes;
    }

    private String getStartOfDate(ZonedDateTime date) {
        final String dateString = String.format("%d-%02d-%02d",
                date.getYear(), date.getMonth().getValue(), date.getDayOfMonth());
        return String.format("%sT00:00:00Z", dateString);
    }
    private String getYearAndWeek(ZonedDateTime date) {
        return String.format("%d%02d",
                date.getYear(), date.get(ChronoField.ALIGNED_WEEK_OF_YEAR));
    }

    private String getDateInterval(ZonedDateTime date, ZonedDateTime toDate) {
        final String dateString = String.format("%d-%02d-%02d",
                date.getYear(), date.getMonth().getValue(), date.getDayOfMonth());
        final String toDateString = String.format("%d-%02d-%02d",
                toDate.getYear(), toDate.getMonth().getValue(), toDate.getDayOfMonth());
        return String.format("[%sT00:00:00Z TO %sT23:59:59.999999999Z]", dateString, toDateString);
    }

    private String getWholeDayDateRange(ZonedDateTime date) {
        final String dateString = String.format("%d-%02d-%02d",
                date.getYear(), date.getMonth().getValue(), date.getDayOfMonth());
        return String.format("[%sT00:00:00Z TO %sT23:59:59.999999999Z]", dateString, dateString);
    }

    private String getVPSearchYear(String code, ZonedDateTime date) {
        final int year = date.getYear();
        final Instant firstDayOfYear = Instant.parse(String.format("%d-01-01T07:00:00Z", year));
        final long daysIntoYear = Math.abs(ChronoUnit.DAYS.between(firstDayOfYear, date.toInstant()));
        // If we have only just entered the new year, VP searches still
        // need to look at the previous year.
        final int queryYear = daysIntoYear > 30 ? year : year - 1;
        String query = String.format("(term.kk:%s%d* NOT term.kk:%s%d01)", code, queryYear, code, queryYear);
        if (queryYear != year) {
            query += String.format(" OR term.kk:%s%d01", code, year);
        }
        return query;
    }
}
