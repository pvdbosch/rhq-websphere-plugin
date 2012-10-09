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
package be.fgov.kszbcss.rhq.websphere.component.sib;

import java.util.Map;

import javax.management.JMException;

import com.ibm.websphere.management.exception.ConnectorException;

import be.fgov.kszbcss.rhq.websphere.WebSphereServer;
import be.fgov.kszbcss.rhq.websphere.mbean.DynamicMBeanObjectNamePatternLocator;

public class SIBGatewayLinkMBeanLocator extends DynamicMBeanObjectNamePatternLocator {
    private final SIBMessagingEngineComponent me;
    private final String name;

    public SIBGatewayLinkMBeanLocator(SIBMessagingEngineComponent me, String name) {
        super("WebSphere", true);
        this.me = me;
        this.name = name;
    }

    @Override
    protected void applyKeyProperties(WebSphereServer server, Map<String,String> props) throws JMException, ConnectorException, InterruptedException {
        // Note: mbeanIdentifier is present in both WAS 6.1 and 7.0, but 6.1 doesn't have targetUuid and SIBMessagingEngine
        props.put("mbeanIdentifier", me.getInfo().getGatewayLinkId(name).replace('|', '/'));
    }
}
