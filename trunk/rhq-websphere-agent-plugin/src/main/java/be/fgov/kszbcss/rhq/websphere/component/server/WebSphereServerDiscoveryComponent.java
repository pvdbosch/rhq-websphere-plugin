package be.fgov.kszbcss.rhq.websphere.component.server;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javanet.staxutils.SimpleNamespaceContext;

import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import be.fgov.kszbcss.rhq.websphere.ConfigurationBasedProcessLocator;
import be.fgov.kszbcss.rhq.websphere.connector.SecureAdminClientProvider;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.exception.ConnectorException;

public class WebSphereServerDiscoveryComponent implements ResourceDiscoveryComponent<ResourceComponent<?>>, ManualAddFacet<ResourceComponent<?>> {
    private static final Log log = LogFactory.getLog(WebSphereServerDiscoveryComponent.class);
    
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException, Exception {
        Set<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();
        for (ProcessScanResult process : context.getAutoDiscoveredProcesses()) {
            ProcessInfo processInfo = process.getProcessInfo();
            if (log.isDebugEnabled()) {
                log.debug("Examining process " + processInfo.getPid());
            }
            String[] commandLine = processInfo.getCommandLine();
            int appOptionIndex = -1; // The index of the -application option
            for (int i=0; i<commandLine.length; i++) {
                if (commandLine[i].equals("-application")) {
                    appOptionIndex = i;
                    break;
                }
            }
            if (appOptionIndex == -1) {
                log.debug("No -application option found");
                continue;
            }
            if (commandLine.length-appOptionIndex != 7) {
                log.debug("Unexpected number of arguments after -application");
                continue;
            }
            if (!commandLine[appOptionIndex+1].equals("com.ibm.ws.bootstrap.WSLauncher")) {
                log.debug("Expected com.ibm.ws.bootstrap.WSLauncher after -application");
                continue;
            }
            if (!commandLine[appOptionIndex+2].equals("com.ibm.ws.runtime.WsServer")) {
                if (log.isDebugEnabled()) {
                    log.debug("Process doesn't appear to be a WebSphere server process; main class is " + commandLine[appOptionIndex+2]);
                }
                continue;
            }
            File repository = new File(commandLine[appOptionIndex+3]);
            String cell = commandLine[appOptionIndex+4];
            String node = commandLine[appOptionIndex+5];
            String processName = commandLine[appOptionIndex+6];
            if (processName.equals("nodeagent")) {
                log.debug("Process appears to be a node agent");
                continue;
            }
            if (processName.equals("dmgr")) {
                log.debug("Process appears to be a deployment manager");
                continue;
            }
            log.info("Discovered WebSphere application server process " + cell + "/" + node + "/" + processName);
            if (!repository.exists()) {
                log.error("Configuration repository " + repository + " doesn't exist");
                continue;
            }
            if (!repository.canRead()) {
                log.error("Configuration repository " + repository + " not readable");
                continue;
            }
            
            // Parse the serverindex.xml file for the node
            File serverIndexFile = new File(repository, "cells" + File.separator + cell + File.separator + "nodes" + File.separator + node + File.separator + "serverindex.xml");
            if (log.isDebugEnabled()) {
                log.debug("Attempting to read " + serverIndexFile);
            }
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document serverIndex;
            try {
                serverIndex = builder.parse(serverIndexFile);
            } catch (Exception ex) {
                log.error("Unable to parse " + serverIndexFile, ex);
                continue;
            }
            
            // Extract the port number
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.setPrefix("serverindex", "http://www.ibm.com/websphere/appserver/schemas/5.0/serverindex.xmi");
            xpath.setNamespaceContext(nsContext);
            String portString = (String)xpath.evaluate("/serverindex:ServerIndex/serverEntries[@serverName='" + processName
                    + "']/specialEndpoints[@endPointName='ORB_LISTENER_ADDRESS']/endPoint/@port", serverIndex, XPathConstants.STRING);
            if (portString == null || portString.length() == 0) {
                log.error("Unable to extract ORB_LISTENER_ADDRESS from " + serverIndexFile);
                continue;
            }
            int port;
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException ex) {
                log.error("Found non numerical port number in " + serverIndexFile);
                continue;
            }
            
            // Check if this is a managed server by looking for WebSphere instances of type NODE_AGENT
            boolean unmanaged = ((NodeList)xpath.evaluate("/serverindex:ServerIndex/serverEntries[@serverType='NODE_AGENT']",
                    serverIndex, XPathConstants.NODESET)).getLength() == 0;
            
            Configuration conf = new Configuration();
            conf.put(new PropertySimple("host", "localhost"));
            conf.put(new PropertySimple("port", port));
            conf.put(new PropertySimple("protocol", "RMI"));
            conf.put(new PropertySimple("loggingProvider", "none")); // TODO: autodetect XM4WAS
            conf.put(new PropertySimple("childJmxServerName", "JVM"));
            conf.put(new PropertySimple("unmanaged", unmanaged));
            result.add(new DiscoveredResourceDetails(context.getResourceType(), cell + "/" + node + "/" + processName,
                    processName, null, processName + " (cell " + cell + ", node " + node + ")", conf, processInfo));
        }
        return result;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration, ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext) throws InvalidPluginConfigurationException {
        ObjectName serverBeanName;
        try {
            AdminClient adminClient = new SecureAdminClientProvider(new ConfigurationBasedProcessLocator(pluginConfiguration)).createAdminClient();
            serverBeanName = adminClient.getServerMBean();
        } catch (ConnectorException ex) {
            throw new InvalidPluginConfigurationException("Unable to connect to server", ex);
        }
        String cell = serverBeanName.getKeyProperty("cell");
        String node = serverBeanName.getKeyProperty("node");
        String process = serverBeanName.getKeyProperty("process");
        String processType = serverBeanName.getKeyProperty("processType");
        boolean unmanaged;
        if (processType.equals("ManagedProcess")) {
            unmanaged = false;
        } else if (processType.equals("UnManagedProcess")) {
            unmanaged = true;
        } else {
            throw new InvalidPluginConfigurationException("Unsupported process type " + processType);
        }
        pluginConfiguration.getSimple("unmanaged").setBooleanValue(unmanaged);
        return new DiscoveredResourceDetails(discoveryContext.getResourceType(), cell + "/" + node + "/" + process,
                process, null, process + " (cell " + cell + ", node " + node + ")", pluginConfiguration, null);
    }
}
