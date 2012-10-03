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
package be.fgov.kszbcss.rhq.websphere;

import javax.management.JMException;

import org.rhq.core.domain.configuration.Configuration;

import be.fgov.kszbcss.rhq.websphere.config.ConfigQueryService;
import be.fgov.kszbcss.rhq.websphere.config.ConfigQueryServiceFactory;

import com.ibm.websphere.management.exception.ConnectorException;

public class UnmanagedServer extends ApplicationServer {
    public UnmanagedServer(String cell, String node, String server, Configuration config) {
        super(cell, node, server, "UnManagedProcess", new ConfigurationBasedProcessLocator(config));
    }

    @Override
    protected ConfigQueryService createConfigQueryService() throws ConnectorException {
        return ConfigQueryServiceFactory.getInstance().getConfigQueryService(this);
    }

    @Override
    public String getClusterName() throws InterruptedException, JMException, ConnectorException {
        // An unmanaged server cannot be a member of a cluster
        return null;
    }
}
