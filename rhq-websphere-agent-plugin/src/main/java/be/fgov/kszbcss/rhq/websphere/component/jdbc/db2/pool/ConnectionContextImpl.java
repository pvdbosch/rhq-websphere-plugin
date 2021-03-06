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
package be.fgov.kszbcss.rhq.websphere.component.jdbc.db2.pool;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ibm.db2.jcc.DB2ClientRerouteServerList;
import com.ibm.db2.jcc.DB2SimpleDataSource;

final class ConnectionContextImpl {
    private static final Logger log = LoggerFactory.getLogger(ConnectionContextImpl.class);
    
    int refCounter;
    private final DB2SimpleDataSource dataSource;
    private Connection connection;
    private long lastSuccessfulQuery;
    
    ConnectionContextImpl(Map<String,Object> properties) {
        dataSource = new DB2SimpleDataSource();
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(DB2SimpleDataSource.class);
        } catch (IntrospectionException ex) {
            throw new Error(ex);
        }
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            String name = descriptor.getName();
            if (properties.containsKey(name)) {
                Object value = properties.get(name);
                Class<?> propertyType = descriptor.getPropertyType();
                if (log.isDebugEnabled()) {
                    log.debug("Setting property " + name + ": propertyType=" + propertyType.getName() + ", value=" + value + " (class=" + (value == null ? "<N/A>" : value.getClass().getName()) + ")");
                }
                if (propertyType != String.class && value instanceof String) {
                    // Need to convert value to correct type
                    if (propertyType == Integer.class || propertyType == Integer.TYPE) {
                        value = Integer.valueOf((String)value);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Converted value to " + value + " (class=" + value.getClass().getName() + ")");
                    }
                }
                try {
                    descriptor.getWriteMethod().invoke(dataSource, value);
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException("Failed to set '" + name + "' property", ex);
                } catch (IllegalAccessException ex) {
                    throw new IllegalAccessError(ex.getMessage());
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException)cause;
                    } else if (cause instanceof Error) {
                        throw (Error)cause;
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }
    
    void testConnection() throws SQLException {
        execute(new Query<Void>() {
            public Void execute(Connection connection) throws SQLException {
                Statement statement = connection.createStatement();
                try {
                    statement.execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
                } finally {
                    statement.close();
                }
                return null;
            }
        });
    }
    
    DB2ClientRerouteServerList getClientRerouteServerList() throws SQLException {
        // Need to make sure that a query has been executed recently so that the reroute information is up to date
        if (System.currentTimeMillis() - lastSuccessfulQuery > 60000) {
            testConnection();
        }
        return dataSource.getClientRerouteServerList();
    }
    
    synchronized <T> T execute(Query<T> query) throws SQLException {
        if (connection == null) {
            connection = dataSource.getConnection();
        }
        try {
            T result = query.execute(connection);
            lastSuccessfulQuery = System.currentTimeMillis();
            return result;
        } catch (SQLException ex) {
            // Discard connection; we will re-attempt next time
            try {
                connection.close();
            } catch (SQLException ex2) {
                // Ignore
            }
            connection = null;
            throw ex;
        }
    }
    
    void destroy() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                log.error("Error closing database connection", ex);
            }
        }
    }
}
