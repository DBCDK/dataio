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

package dk.dbc.dataio.sink.testutil;

import java.security.Principal;
import java.util.Map;
import java.util.Properties;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;

@SuppressWarnings(value = "deprecation")
public class MockedMessageDrivenContext implements MessageDrivenContext {
    private boolean rollbackOnly = false;

    @Override
    public EJBHome getEJBHome() throws IllegalStateException {
        return null;
    }

    @Override
    public EJBLocalHome getEJBLocalHome() throws IllegalStateException {
        return null;
    }

    @Override
    public Properties getEnvironment() {
        return null;
    }

    @Override
    public java.security.Identity getCallerIdentity() {
        return null;
    }

    @Override
    public Principal getCallerPrincipal() throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isCallerInRole(java.security.Identity identity) {
        return false;
    }

    @Override
    public boolean isCallerInRole(String s) throws IllegalStateException {
        return false;
    }

    @Override
    public UserTransaction getUserTransaction() throws IllegalStateException {
        return null;
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() throws IllegalStateException {
        return rollbackOnly;
    }

    @Override
    public TimerService getTimerService() throws IllegalStateException {
        return null;
    }

    @Override
    public Object lookup(String s) throws IllegalArgumentException {
        return null;
    }

    @Override
    public Map<String, Object> getContextData() {
        return null;
    }

}
