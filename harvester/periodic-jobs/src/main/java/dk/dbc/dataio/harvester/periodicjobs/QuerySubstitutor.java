/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.apache.commons.text.StringSubstitutor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class takes a piece of text and substitutes variables within it
 */
public class QuerySubstitutor {
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
     *      ${__NOW__}                  := time of instantiation for this object
     *                                     as Coordinated Universal Time (UTC)
     *                                     string.
     * </p>
     * @param query query string on which to do variable substitution
     * @param config config supplying values for substitutions
     * @return result of the replace operation with all occurrences of known variables
     *         replaced
     */
    public String replace(String query, PeriodicJobsHarvesterConfig config) {
        final Map<String, String> substitutions = new HashMap<>();
        substitutions.put("__NOW__", convertToUtc(now).toString());
        substitutions.put("__TIME_OF_LAST_HARVEST__", config.getContent().getTimeOfLastHarvest() != null
                ? convertToUtc(config.getContent().getTimeOfLastHarvest().toInstant()).toString()
                : convertToUtc(Instant.EPOCH).toString());
        return new StringSubstitutor(substitutions).replace(query);
    }

    /**
     * @return value used for the __NOW__ variable
     */
    public Date getNow() {
        return Date.from(now);
    }

    private ZonedDateTime convertToUtc(Instant instant) {
        final ZonedDateTime zonedDateTime = instant.atZone(tz);
        return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
    }
}
