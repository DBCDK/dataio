package dk.dbc.dataio.commons.types.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Stopwatch
@Interceptor
public class StopwatchInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(StopwatchInterceptor.class);

    @AroundInvoke
    public Object time(InvocationContext invocationContext) throws Exception {
        final String systemClassName = invocationContext.getMethod().getDeclaringClass().getCanonicalName();
        final String systemMethodName = invocationContext.getMethod().getName();

        final long startTime = System.currentTimeMillis();
        final Object businessCall;
        try {
            businessCall = invocationContext.proceed();
        } finally {
            LOGGER.info("calling method {}.{} took {} milliseconds",
                    systemClassName, systemMethodName, System.currentTimeMillis() - startTime);
        }
        return businessCall;
    }
}
