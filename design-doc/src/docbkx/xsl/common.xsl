<?xml version="1.0" encoding="utf-8"?>
<!--
  ~ RHQ WebSphere Plug-in
  ~ Copyright (C) 2012-2013 Crossroads Bank for Social Security
  ~ All rights reserved.
  ~
  ~ This program is free software; you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License, version 2, as
  ~ published by the Free Software Foundation, and/or the GNU Lesser
  ~ General Public License, version 2.1, also as published by the Free
  ~ Software Foundation.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  ~ GNU General Public License and the GNU Lesser General Public License
  ~ for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ and the GNU Lesser General Public License along with this program;
  ~ if not, write to the Free Software Foundation, Inc.,
  ~ 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version="1.0">
    <xsl:param name="admon.graphics">true</xsl:param>
    <xsl:param name="admon.textlabel">0</xsl:param>

    <xsl:param name="section.autolabel">1</xsl:param>
    <xsl:param name="section.label.includes.component.label">1</xsl:param>    
    <xsl:param name="section.autolabel.max.depth">3</xsl:param>
    
    <xsl:param name="bibliography.numbered">1</xsl:param>
</xsl:stylesheet>