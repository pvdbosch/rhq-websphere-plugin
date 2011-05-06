package be.fgov.kszbcss.websphere.rhq;

import java.util.Set;

import org.mc4j.ems.connection.bean.EmsBeanName;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jmx.JMXComponent;

public class ModuleDiscoveryComponent extends WebSphereMBeanResourceDiscoveryComponent<JMXComponent> {
    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<JMXComponent> context) {
        // We need to copy the applicationName from the parent resource so that we can use it in the discovery of
        // child resources. The reason is that the moduleName is not unique.
        String applicationName = context.getParentResourceContext().getPluginConfiguration().getSimple("applicationName").getStringValue();
        Set<DiscoveredResourceDetails> result = super.discoverResources(context);
        for (DiscoveredResourceDetails details : result) {
            details.getPluginConfiguration().put(new PropertySimple("applicationName", applicationName));
        }
        return result;
    }

    @Override
    protected String getResourceKey(EmsBeanName objectName) {
        // For WebModule MBeans, the mbeanIdentifier may be "null". The J2EEName contains a
        // combination of the application name and the module name.
        return objectName.getKeyProperty("J2EEName");
    }
}