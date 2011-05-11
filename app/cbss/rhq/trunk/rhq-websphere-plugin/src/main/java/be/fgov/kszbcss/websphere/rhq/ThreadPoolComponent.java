package be.fgov.kszbcss.websphere.rhq;

import javax.management.JMException;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;

import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.pmi.stat.MBeanStatDescriptor;
import com.ibm.websphere.pmi.stat.WSRangeStatistic;

public class ThreadPoolComponent extends PMIComponent<WebSphereServerComponent> {
    private MBean mbean;
    
    @Override
    protected void start() throws InvalidPluginConfigurationException, Exception {
        ResourceContext<WebSphereServerComponent> context = getResourceContext();
        mbean = new MBean(getServer(), Utils.createObjectName("WebSphere:type=ThreadPool,name=" + context.getResourceKey() + ",*"));
    }

    @Override
    protected MBeanStatDescriptor getMBeanStatDescriptor() throws JMException, ConnectorException {
        return new MBeanStatDescriptor(mbean.getObjectName());
    }
    
    public AvailabilityType getAvailability() {
        // TODO Auto-generated method stub
        return AvailabilityType.UP;
    }

    public void stop() {
    }

    @Override
    protected double getValue(String name, WSRangeStatistic statistic) {
        if (name.equals("PercentMaxed")) {
            return ((double)statistic.getCurrent())/100;
        } else {
            return super.getValue(name, statistic);
        }
    }
}