/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.apache.commons.text.StringSubstitutor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class takes a piece of text and substitutes variables within it
 */
public class QuerySubstitutor {
    private final static Pattern WEEKCODE_PATTERN = Pattern.compile("\\$\\{__WEEKCODE_(.+?)__\\}");

    private final Instant now = Instant.now();
    private final ZoneId tz = ZoneId.of(System.getenv("TZ"));

    /**
     * Replaces variables within the given query string with values
     * from the given config
     * <p>
     * Known variables are:
     * </p>
     * <p>
     *      ${__TIME_OF_LAST_HARVEST__} := time of last harvest as Coordinated
     *                                     Universal Time (UTC) string.
     * </p>
     * <p>
     *      ${__CURRENT_YEAR__}         := year based on the instantiation time
     *                                     for this object as Coordinated Universal
     *                                     Time (UTC)
     * </p>
     * <p>
     *      ${__PREVIOUS_YEAR__}        := previous year based on the instantiation time
     *                                     for this object as Coordinated Universal
     *                                     Time (UTC)
     * </p>
     * <p>
     *      ${__NOW__}                  := time of instantiation for this object
     *                                     as Coordinated Universal Time (UTC)
     *                                     string.
     * </p>
     * <p>
     *      ${__WEEKCODE_[CATALOGUE]__} := weekcode as string for the given CATALOGUE
     *                                     in relation to the current local date,
     *                                     e.g. ${__WEEKCODE_EMS__}
     * </p>
     * @param query query string on which to do variable substitution
     * @param config config supplying values for substitutions
     * @param weekcodeSupplier Supplier of week codes
     * @return result of the replace operation with all occurrences of known variables
     *         replaced
     */
    public String replace(String query, PeriodicJobsHarvesterConfig config, WeekcodeSupplier weekcodeSupplier) {
        final Map<String, String> substitutions = new HashMap<>();
        final ZonedDateTime nowUTC = convertToUtc(now);
        substitutions.put("__NOW__", nowUTC.toString());
        substitutions.put("__CURRENT_YEAR__", String.valueOf(nowUTC.getYear()));
        substitutions.put("__PREVIOUS_YEAR__", String.valueOf(nowUTC.getYear() - 1));
        substitutions.put("__TIME_OF_LAST_HARVEST__", config.getContent().getTimeOfLastHarvest() != null
                ? convertToUtc(config.getContent().getTimeOfLastHarvest().toInstant()).toString()
                : convertToUtc(Instant.EPOCH).toString());

        if (weekcodeSupplier != null) {
            final LocalDate localDate = now.atZone(tz).toLocalDate();
            getCatalogueCodesToResolve(query).forEach(catalogueCode -> substitutions.put(
                    String.format("__WEEKCODE_%s__", catalogueCode), weekcodeSupplier.get(catalogueCode, localDate)));
        }

        return new StringSubstitutor(substitutions).replace(query);
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

    private List<String> getCatalogueCodesToResolve(String query) {
        final List<String> catalogueCodes = new ArrayList<>();
        final Matcher matcher = WEEKCODE_PATTERN.matcher(query);
        while (matcher.find()) {
            catalogueCodes.add(matcher.group(1));
        }
        return catalogueCodes;
    }
}
