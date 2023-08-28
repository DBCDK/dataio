package dk.dbc.dataio.commons.utils.test.jms;

import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBLocalHome;
import jakarta.ejb.MessageDrivenContext;
import jakarta.ejb.TimerService;
import jakarta.transaction.UserTransaction;

import java.security.Principal;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings("deprecation")
public class MockedJmsMessageDrivenContext implements MessageDrivenContext {
    private boolean rollbackOnly = false;

    @Override
    public EJBHome getEJBHome() throws IllegalStateException {
        return null;
    }

    @Override
    public EJBLocalHome getEJBLocalHome() throws IllegalStateException {
        return null;
    }

    public Properties getEnvironment() {
        return null;
    }

    @Override
    public Principal getCallerPrincipal() throws IllegalStateException {
        return null;
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
