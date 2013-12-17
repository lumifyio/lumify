package com.altamiracorp.lumify.storm;

import javax.management.*;
import java.lang.management.ManagementFactory;

public class JmxBeanHelper {
    private static Object registerLock = new Object();
    public static final String BOLT_PREFIX = "com.altamiracorp.lumify.storm.bolt";
    public static final String SPOUT_PREFIX = "com.altamiracorp.lumify.storm.spout";

    public static void registerJmxBean(Object me, String prefix) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        synchronized (registerLock) {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            for (int suffix = 0; ; suffix++) {
                ObjectName beanName = new ObjectName(prefix + ":type=" + me.getClass().getName() + "-" + suffix);
                if (beanServer.isRegistered(beanName)) {
                    continue;
                }
                beanServer.registerMBean(me, beanName);
                break;
            }
        }
    }
}
