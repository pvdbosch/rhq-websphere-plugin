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

import java.util.Collection;

import javax.management.JMException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.websphere.management.exception.ConnectorException;

public abstract class Path<T extends ConfigObject> {
    private static final Log log = LogFactory.getLog(Path.class);
    
    abstract Class<T> getType();
    abstract <S extends ConfigObject> Collection<S> resolveRelative(String relativePath, Class<S> type) throws JMException, ConnectorException, InterruptedException;
    
    public final <S extends ConfigObject> Path<S> path(Class<S> type, String name) {
        return new RelativePath<S>(this, type, name);
    }
    
    public final <S extends ConfigObject> Path<S> path(Class<S> type) {
        return path(type, "");
    }
    
    public Collection<T> resolve() throws JMException, ConnectorException, InterruptedException {
        Collection<T> configObjects = resolveRelative(null, getType());
        if (log.isDebugEnabled()) {
            if (configObjects.isEmpty()) {
                log.debug("No configuration data found");
            } else {
                StringBuilder buffer = new StringBuilder("Configuration data found:");
                for (ConfigObject configObject : configObjects) {
                    buffer.append("\n * ");
                    buffer.append(configObject.getId());
                }
                log.debug(buffer.toString());
            }
        }
        return configObjects;
    }
    
    public T resolveSingle() throws JMException, ConnectorException, InterruptedException, ConfigQueryException {
        Collection<T> configObjects = resolve();
        if (configObjects.size() == 1) {
            return configObjects.iterator().next();
        } else if (configObjects.isEmpty()) {
            throw new ConfigObjectNotFoundException("Configuration object not found");
        } else {
            // TODO: proper exception type
            throw new RuntimeException("More than one configuration object found");
        }
    }
}