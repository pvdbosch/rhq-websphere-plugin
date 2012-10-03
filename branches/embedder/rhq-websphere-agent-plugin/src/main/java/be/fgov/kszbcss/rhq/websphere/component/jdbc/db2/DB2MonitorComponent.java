/*
 * RHQ WebSphere Plug-in
 * Copyright (C) 2012 Crossroads Bank for Social Security
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package be.fgov.kszbcss.rhq.websphere.component.jdbc.db2;

import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

import be.fgov.kszbcss.rhq.websphere.component.WebSphereServiceComponent;
import be.fgov.kszbcss.rhq.websphere.component.jdbc.DataSourceComponent;
import be.fgov.kszbcss.rhq.websphere.proxy.AdminOperations;
import be.fgov.kszbcss.rhq.websphere.support.measurement.MeasurementFacetSupport;

public class DB2MonitorComponent extends WebSphereServiceComponent<DataSourceComponent> implements MeasurementFacet {
    private static final Log log = LogFactory.getLog(DB2MonitorComponent.class);
    
    private DataSourceComponent dataSourceComponent;
    private AdminOperations adminOperations;
    private String principal;
    private String credentials;
    private DB2MonitorContext context;
    private MeasurementFacetSupport measurementFacetSupport;
    
    protected void start() throws InvalidPluginConfigurationException, Exception {
        ResourceContext<DataSourceComponent> context = getResourceContext();
        dataSourceComponent = context.getParentResourceComponent();
        adminOperations = dataSourceComponent.getServer().getMBeanClient("WebSphere:type=AdminOperations,*").getProxy(AdminOperations.class);
        Configuration config = context.getPluginConfiguration();
        principal = config.getSimpleValue("principal", null);
        credentials = config.getSimpleValue("credentials", null);
        try {
            Class.forName("com.ibm.db2.jcc.DB2SimpleDataSource");
        } catch (ClassNotFoundException ex) {
            log.error("DB2 monitor unavailable: JDBC driver not present in the class path");
            throw ex;
        }
        if (principal != null) {
            measurementFacetSupport = new MeasurementFacetSupport(this);
            measurementFacetSupport.setDefaultHandler(new SnapshotMeasurementGroupHandler(this, adminOperations));
            measurementFacetSupport.addHandler("acr", new ACRMeasurementGroupHandler(this));
        }
    }

    @Override
    protected AvailabilityType doGetAvailability() {
        try {
            getContext().testConnection();
            return AvailabilityType.UP;
        } catch (Exception ex) {
            if (log.isDebugEnabled()) {
                log.debug("Test connection failed ==> availability = DOWN", ex);
            }
            return AvailabilityType.DOWN;
        }
    }

    @Override
    protected boolean isConfigured(boolean immediate) throws Exception {
        return true;
    }

    public DB2MonitorContext getContext() throws Exception {
        Map<String,Object> dataSourceProperties = dataSourceComponent.getConnectionFactoryInfo(false).getProperties();
        if (context == null || !context.getDataSourceProperties().equals(dataSourceProperties)) {
            if (context != null) {
                context.destroy();
            }
            context = new DB2MonitorContext(dataSourceProperties, principal, credentials);
        }
        return context;
    }
    
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        if (measurementFacetSupport == null) {
            log.warn("No monitoring user defined for data source "
                    + getResourceContext().getParentResourceComponent().getResourceContext().getResourceKey()
                    + "; unable to collect measurements");
        } else {
            measurementFacetSupport.getValues(report, requests);
        }
    }

    public void stop() {
        if (context != null) {
            context.destroy();
            context = null;
        }
    }
}
