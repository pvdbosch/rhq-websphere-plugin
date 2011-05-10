package be.fgov.kszbcss.websphere.rhq;

import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

public class ApplicationDiscoveryComponent implements ResourceDiscoveryComponent<WebSphereServerComponent> {
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<WebSphereServerComponent> context) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();
        for (ObjectName objectName : context.getParentResourceComponent().getServer().getAdminClient().queryNames(Utils.createObjectName("WebSphere:type=Application,*"), null)) {
            String applicationName = objectName.getKeyProperty("name");
            result.add(new DiscoveredResourceDetails(context.getResourceType(), applicationName, applicationName, null, "An enterprise application.", null, null));
        }
        return result;
    }
}
