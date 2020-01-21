/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.types;

import java.util.Objects;

/**
 * This {@link Pickup} type represents downloads from filestore
 */
public class HttpPickup implements Pickup {
    /**
     * Receiving agency to be added to filestore metadata
     */
    private String receivingAgency;

    public String getReceivingAgency() {
        return receivingAgency;
    }

    public HttpPickup withReceivingAgency(String receivingAgency) {
        this.receivingAgency = receivingAgency;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HttpPickup that = (HttpPickup) o;

        return Objects.equals(receivingAgency, that.receivingAgency);
    }

    @Override
    public int hashCode() {
        return receivingAgency != null ? receivingAgency.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HttpPickup{" +
                "receivingAgency='" + receivingAgency + '\'' +
                '}';
    }
}
