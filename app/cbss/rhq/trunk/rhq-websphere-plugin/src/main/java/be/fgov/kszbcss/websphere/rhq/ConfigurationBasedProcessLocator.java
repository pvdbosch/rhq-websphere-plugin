package be.fgov.kszbcss.websphere.rhq;

import java.util.Properties;

import org.rhq.core.domain.configuration.Configuration;

import com.ibm.websphere.management.AdminClient;

/**
 * {@link ProcessLocator} implementation that builds the admin client
 * configuration from a plugin configuration.
 */
public class ConfigurationBasedProcessLocator implements ProcessLocator {
    private final Configuration config;

    public ConfigurationBasedProcessLocator(Configuration config) {
        this.config = config;
    }

    public void getAdminClientProperties(Properties properties) {
        properties.put(AdminClient.CONNECTOR_TYPE, AdminClient.CONNECTOR_TYPE_RMI);
        properties.setProperty(AdminClient.CONNECTOR_HOST, config.getSimpleValue("host", null));
        properties.setProperty(AdminClient.CONNECTOR_PORT, config.getSimpleValue("port", null));
        
        String principal = config.getSimpleValue("principal", null); 
        if (principal != null && principal.length() > 0) { 
            properties.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "true"); 
            properties.setProperty(AdminClient.USERNAME, principal); 
            properties.setProperty(AdminClient.PASSWORD, config.getSimpleValue("credentials", null)); 
        } else { 
            properties.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "false");
        }
    }
}