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
package be.fgov.kszbcss.rhq.websphere.mbean;

import java.util.Set;

import javax.management.JMException;
import javax.management.ObjectName;

import be.fgov.kszbcss.rhq.websphere.process.WebSphereServer;

import com.ibm.websphere.management.exception.ConnectorException;

/**
 * Locates an MBean based on some criteria. Instances of this class are used as cache keys.
 * Therefore implementations must override {@link Object#equals(Object)} and
 * {@link Object#hashCode()}.
 */
public interface MBeanLocator {
    Set<ObjectName> queryNames(WebSphereServer server) throws JMException, ConnectorException, InterruptedException;
}
