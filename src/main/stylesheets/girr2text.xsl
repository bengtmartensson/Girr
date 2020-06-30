<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2020 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
-->

<!--
This script transforms a Girr file, containing either raw or Pronto Hex data,
to text format that IrpTransmogrifier can transmogrify.
Raw is preferred over Pronto hex.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
 xmlns:girr="http://www.harctoolbox.org/Girr">
    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>

    <!-- Nuke all text nodes not explicitly invoked -->
    <xsl:template match="text()"/>

    <xsl:template match="//girr:command[girr:ccf]">
        <xsl:value-of select="@name"/>
        <xsl:text>
</xsl:text>
        <xsl:value-of select="girr:ccf/text()"/>
        <xsl:text>

</xsl:text>
    </xsl:template>

    <!-- raw has higher priority than ccf -->
    <xsl:template match="//girr:command[girr:raw]">
        <xsl:value-of select="@name"/>
       <xsl:text>
</xsl:text>
        <xsl:apply-templates select="girr:raw"/>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="girr:raw">
        <xsl:apply-templates select="@frequency"/>
        <xsl:text>[</xsl:text>
        <xsl:apply-templates select="girr:intro"/>
        <xsl:text>]</xsl:text>
        <xsl:text>[</xsl:text>
        <xsl:apply-templates select="girr:repeat"/>
        <xsl:text>]</xsl:text>
        <xsl:text>[</xsl:text>
        <xsl:apply-templates select="girr:ending"/>
        <xsl:text>]
</xsl:text>
    </xsl:template>

    <xsl:template match="@frequency">
        <xsl:text>Freq=</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="girr:intro|girr:repeat|girr:ending">
        <xsl:value-of select="normalize-space(text())"/>
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="girr:flash|girr:gap">
        <xsl:value-of select="text()"/>
        <xsl:if test="position() &lt; last()">
            <xsl:text> </xsl:text>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
