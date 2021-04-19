<?xml version="1.0" encoding="UTF-8"?>
<!-- This program is in the public domain. -->

<!--
This script transforms a Girr file, containing parameters,
to the CSV format used by IRDB.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
 xmlns:girr="http://www.harctoolbox.org/Girr">
    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>

    <!-- Nuke all text nodes not explicitly invoked -->
    <xsl:template match="text()"/>

    <xsl:template match="/">
        <!-- The silly line :-) -->
        <xsl:text>functionname,protocol,device,subdevice,function
</xsl:text>
          <xsl:apply-templates select="//girr:command"/>
    </xsl:template>

    <xsl:template match="girr:command">
        <xsl:value-of select="@name"/>
        <xsl:text>,</xsl:text>
        <xsl:apply-templates select="girr:parameters"/>
        <xsl:if test="not(girr:parameters)">
            <xsl:apply-templates select="../girr:parameters"/> <!-- Does not contain "F" -->
        </xsl:if>
        <xsl:value-of select="@F"/>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="girr:parameters">
        <xsl:value-of select="@protocol"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="girr:parameter[@name='D']/@value"/>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="girr:parameter[@name='S']/@value"/>
        <xsl:if test="not(girr:parameter[@name='S'])">
            <xsl:text>-1</xsl:text>
        </xsl:if>
        <xsl:text>,</xsl:text>
        <xsl:value-of select="girr:parameter[@name='F']/@value"/>
    </xsl:template>

</xsl:stylesheet>
