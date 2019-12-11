<?xml version="1.0" encoding="UTF-8"?>
<!-- Copying and distribution of this file, with or without modification,
     are permitted in any medium without royalty provided the copyright
     notice and this notice are preserved.  This file is offered as-is,
     without any warranty.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:html="http://www.w3.org/1999/xhtml"
                xmlns:girr="http://www.harctoolbox.org/Girr">
    <xsl:output method="html"/>

    <xsl:template match="/">
        <html>
            <head>
                <title><xsl:value-of select="girr:remotes/@title"/></title>
            </head>
            <body>
                <h1><xsl:value-of select="girr:remotes/@title"/></h1>
                <xsl:apply-templates select="girr:remotes/girr:remote"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="girr:remote">
        <h2>Remote: <xsl:value-of select="@name"/></h2>
        <xsl:apply-templates select="girr:notes"/>
        <xsl:apply-templates select="html:img"/>
        <xsl:apply-templates select="girr:commandSet"/>
    </xsl:template>

    <xsl:template match="girr:notes">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="html:img">
        <img>
            <xsl:copy-of select="@*"/>
        </img>
    </xsl:template>

    <xsl:template match="girr:command">
        <h3>
            <xsl:value-of select="@name"/>
            <xsl:apply-templates select="girr:parameters"/>
            <xsl:apply-templates select="@F"/>
        </h3>
        <p>
            <xsl:value-of select="girr:ccf"/>
        </p>

    </xsl:template>

    <xsl:template match="girr:commandSet">
        <xsl:apply-templates select="girr:parameters"/>
        <xsl:apply-templates select="girr:command"/>
    </xsl:template>

        <xsl:template match="girr:commandSet/girr:parameters">
        <xsl:text>Common parameters: </xsl:text>
        <xsl:apply-templates select="@protocol"/>
        <xsl:apply-templates select="girr:parameter"/>
    </xsl:template>

    <xsl:template match="girr:command/girr:parameters">
        (<xsl:apply-templates select="@protocol"/>
        <xsl:apply-templates select="girr:parameter"/>)
    </xsl:template>

    <xsl:template match="@protocol">
        <xsl:text>Protocol=</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="@F">
        <xsl:text> F=</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="girr:parameter">
        <xsl:text> </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="@value"/>
    </xsl:template>

</xsl:stylesheet>
