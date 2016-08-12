/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.ush.solr.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
public class ProgressWal {
    @Id
    private Long configId;

    private Long configVersion;

    @Temporal(TemporalType.TIMESTAMP)
    private Date harvestedFrom;

    @Temporal(TemporalType.TIMESTAMP)
    private Date harvestedUntil;

    public Long getConfigId() {
        return configId;
    }

    public ProgressWal withConfigId(Long configId) {
        this.configId = configId;
        return this;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public ProgressWal withConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
        return this;
    }

    public Date getHarvestedFrom() {
        return harvestedFrom;
    }

    public ProgressWal withHarvestedFrom(Date harvestedFrom) {
        this.harvestedFrom = harvestedFrom;
        return this;
    }

    public Date getHarvestedUntil() {
        return harvestedUntil;
    }

    public ProgressWal withHarvestedUntil(Date harvestedUntil) {
        this.harvestedUntil = harvestedUntil;
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

        ProgressWal that = (ProgressWal) o;

        if (configId != null ? !configId.equals(that.configId) : that.configId != null) {
            return false;
        }
        if (configVersion != null ? !configVersion.equals(that.configVersion) : that.configVersion != null) {
            return false;
        }
        if (harvestedFrom != null ? !harvestedFrom.equals(that.harvestedFrom) : that.harvestedFrom != null) {
            return false;
        }
        return harvestedUntil != null ? harvestedUntil.equals(that.harvestedUntil) : that.harvestedUntil == null;

    }

    @Override
    public int hashCode() {
        int result = configId != null ? configId.hashCode() : 0;
        result = 31 * result + (configVersion != null ? configVersion.hashCode() : 0);
        result = 31 * result + (harvestedFrom != null ? harvestedFrom.hashCode() : 0);
        result = 31 * result + (harvestedUntil != null ? harvestedUntil.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProgressWal{" +
                "configId=" + configId +
                ", configVersion=" + configVersion +
                ", harvestedFrom=" + harvestedFrom +
                ", harvestedUntil=" + harvestedUntil +
                '}';
    }
}
