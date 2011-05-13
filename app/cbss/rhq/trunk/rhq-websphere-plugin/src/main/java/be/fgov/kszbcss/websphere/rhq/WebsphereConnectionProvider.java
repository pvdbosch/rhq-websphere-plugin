package be.fgov.kszbcss.websphere.rhq;

import javax.management.MBeanServer;

import org.mc4j.ems.impl.jmx.connection.support.providers.AbstractConnectionProvider;

import be.fgov.kszbcss.websphere.rhq.connector.AdminClientMBeanServer;
import be.fgov.kszbcss.websphere.rhq.connector.MBeanServerStatsProxy;

import com.ibm.websphere.management.AdminClient;

public class WebsphereConnectionProvider extends AbstractConnectionProvider {
    private final AdminClient adminClient;
    private MBeanServerStatsProxy proxy;
    private MBeanServer mbeanServer;

    public WebsphereConnectionProvider(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    protected void doConnect() throws Exception {
        proxy = new MBeanServerStatsProxy(new AdminClientMBeanServer(adminClient));
        mbeanServer = proxy.buildServerProxy();
    }

    public MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    public long getRoundTrips() {
        return proxy.getRoundTrips();
    }

    public long getFailures() {
        return proxy.getFailures();
    }
}
