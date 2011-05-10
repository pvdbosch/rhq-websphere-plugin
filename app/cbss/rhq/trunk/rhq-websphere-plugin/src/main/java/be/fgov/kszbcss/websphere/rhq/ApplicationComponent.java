package be.fgov.kszbcss.websphere.rhq;

import javax.management.JMException;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.w3c.dom.Document;

import com.ibm.websphere.management.exception.ConnectorException;

public class ApplicationComponent extends WebSphereServiceComponent<WebSphereServerComponent> {
    private MBean mbean;
    private DeploymentDescriptorCache deploymentDescriptorCache;
    
    @Override
    protected void start() {
        WebSphereServer server = getServer();
        ResourceContext<WebSphereServerComponent> context = getResourceContext();
        mbean = new MBean(server, Utils.createObjectName("WebSphere:type=Application,name=" + context.getResourceKey() + ",*"));
        server.registerStateChangeEventContext(mbean.getObjectNamePattern(), context.getEventContext());
        deploymentDescriptorCache = new DeploymentDescriptorCache(mbean);
    }
    
    public String getApplicationName() {
        return getResourceContext().getResourceKey();
    }
    
    public Document getDeploymentDescriptor() throws JMException, ConnectorException {
        return deploymentDescriptorCache.getContent();
    }
    
    public AvailabilityType getAvailability() {
        try {
            mbean.getAttribute("name");
            return AvailabilityType.UP;
        } catch (Exception ex) {
            return AvailabilityType.DOWN;
        }
    }

    public void stop() {
        getResourceContext().getParentResourceComponent().getServer().unregisterStateChangeEventContext(mbean.getObjectNamePattern());
    }
}
