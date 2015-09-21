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

package dk.dbc.dataio.sink.es;

import dk.dbc.commons.es.ESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

@LocalBean
@Singleton
@Startup
@Lock(LockType.READ)
public class EsSinkConfigurationBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsSinkConfigurationBean.class);
    // Default values taken from opret_tp_addi project in svn.
    static final int DEFAULT_USER_ID = 3;
    static final String DEFAULT_PACKAGE_TYPE = ESUtil.PackageType.DATABASE_UPDATE.name();
    static final String DEFAULT_ACTION = ESUtil.Action.DELETE.name();

    @Resource(name = "esResourceName")
    String esResourceName;

    @Resource(name = "esDatabaseName")
    String esDatabaseName;

    @Resource(name = "esUserId")
    int esUserId;

    @Resource(name = "esPackageType")
    String esPackageType;

    @Resource(name = "esAction")
    String esAction;

    @PostConstruct
    public void initialize() {
        LOGGER.info("esResourceName={}", esResourceName);
        LOGGER.info("esDatabaseName={}", esDatabaseName);
        LOGGER.info("esUserId={}", getEsUserId());
        LOGGER.info("esPackageType={}", getEsPackageType());
        LOGGER.info("esAction={}", getEsAction());
    }

    public String getEsResourceName() {
        return esResourceName;
    }

    public String getEsDatabaseName() {
        return esDatabaseName;
    }

    public int getEsUserId() {
        if (esUserId == 0)
            return DEFAULT_USER_ID;
        return esUserId;
    }

    public ESUtil.PackageType getEsPackageType() {
        if (esPackageType == null || esPackageType.isEmpty())
            return ESUtil.PackageType.valueOf(DEFAULT_PACKAGE_TYPE);
        return ESUtil.PackageType.valueOf(esPackageType.toUpperCase());
    }

    public ESUtil.Action getEsAction() {
        if (esAction == null || esAction.isEmpty())
            return ESUtil.Action.valueOf(DEFAULT_ACTION);
        return ESUtil.Action.valueOf(esAction.toUpperCase());
    }
}
