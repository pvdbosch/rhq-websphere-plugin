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
package be.fgov.kszbcss.rhq.websphere.component.j2ee;

import java.util.regex.Pattern;

import org.w3c.dom.Document;

import be.fgov.kszbcss.rhq.websphere.config.ConfigQueryException;

import com.ibm.websphere.management.exception.ConnectorException;

public class ModuleSpecVersionMeasurementHandler extends SpecVersionMeasurementHandler {
    private static final Pattern publicIdPatterns[] = {
        Pattern.compile("-//Sun Microsystems, Inc\\.//DTD Enterprise JavaBeans ([0-9]\\.[0-9])//EN"),
        Pattern.compile("-//Sun Microsystems, Inc\\.//DTD Web Application ([0-9]\\.[0-9])//EN")
    };
    
    private final ModuleComponent moduleComponent;

    public ModuleSpecVersionMeasurementHandler(ModuleComponent moduleComponent) {
        this.moduleComponent = moduleComponent;
    }

    @Override
    protected Document getDeploymentDescriptor() throws InterruptedException, ConnectorException, ConfigQueryException {
        return moduleComponent.getModuleInfo().getDeploymentDescriptor();
    }

    @Override
    protected Pattern[] getPublicIdPatterns() {
        return publicIdPatterns;
    }
}
