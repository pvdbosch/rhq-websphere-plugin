/*
 * RHQ WebSphere Plug-in
 * Copyright (C) 2012-2013 Crossroads Bank for Social Security
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
package be.fgov.kszbcss.rhq.websphere.config;

import java.io.Serializable;

import javax.management.JMException;

import com.ibm.websphere.management.exception.ConnectorException;

import be.fgov.kszbcss.rhq.websphere.proxy.ConfigService;

/**
 * Represents an object in the WebSphere configuration. This class provides a more convenient API
 * than {@link ConfigService}.
 */
public interface ConfigObject extends Serializable {
    String getId();
    String getConfigObjectType();
    
    /**
     * Detaches this configuration object from the underlying {@link CellConfiguration} object. This
     * will recursively fetch all attributes. An invocation of this method makes the configuration
     * serializable and thread safe.
     * <p>
     * Typically this method is not called directly by application code; instead configuration
     * objects are detached by passing <code>true</code> to {@link Path#resolve(boolean)} or
     * {@link Path#resolveSingle(boolean)}.
     * 
     * @throws InterruptedException
     * @throws ConnectorException
     * @throws JMException
     */
    void detach() throws JMException, ConnectorException, InterruptedException;
}
