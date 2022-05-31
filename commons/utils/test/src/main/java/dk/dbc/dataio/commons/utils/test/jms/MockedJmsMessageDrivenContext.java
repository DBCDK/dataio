package dk.dbc.dataio.commons.utils.test.jms;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.MessageDrivenContext;
import javax.ejb.TimerService;
import javax.transaction.UserTransaction;
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
