<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:epdcx="http://purl.org/eprint/epdcx/2006-11-16/"
        version="1.0">

<!-- NOTE: This stylesheet is a work in progress, and does not
     cover all aspects of the SWAP and EPDCX specification/schema.
     It is used principally to demonstrate the SWORD ingest
     process -->

<!-- This stylesheet converts incoming DC metadata in a SWAP
     profile into the DSpace Interal Metadata format (DIM) -->

	<!-- Catch all.  This template will ensure that nothing
	     other than explicitly what we want to xwalk will be dealt
	     with -->
	<xsl:template match="text()"></xsl:template>
    
    <!-- match the top level descriptionSet element and kick off the
         template matching process -->
    <xsl:template match="/epdcx:descriptionSet">
    	<dim:dim>
    		<xsl:apply-templates/>
    	</dim:dim>
    </xsl:template>
    
    <!-- general matcher for all "statement" elements -->
    <xsl:template match="/epdcx:descriptionSet/epdcx:description/epdcx:statement">
    
    	<!-- title element: dc.title -->
    	<xsl:when test="./@propertyURI='http://purl.org/dc/elements/1.1/title'">
    		<dim:field mdschema="dc" element="title">
    			<xsl:value-of select="epdcx:valueString"/>
    		</dim:field>
    	</xsl:when>
    	
    	<!-- abstract element: dc.description.abstract -->
    	<xsl:when test="./@propertyURI='http://purl.org/dc/terms/abstract'">
    		<dim:field mdschema="dc" element="description" qualifier="abstract">
    			<xsl:value-of select="epdcx:valueString"/>
    		</dim:field>
    	</xsl:when>
    	
    	<!-- creator element: dc.contributor.author -->
    	<xsl:when test="./@propertyURI='http://purl.org/dc/elements/1.1/creator'">
    		<dim:field mdschema="dc" element="contributor" qualifier="author">
    			<xsl:value-of select="epdcx:valueString"/>
    		</dim:field>
    	</xsl:when>
    	
    	<!-- identifier element: dc.identifier.* -->
    	<xsl:when test="./@propertyURI='http://purl.org/dc/elements/1.1/identifier'">
    		<xsl:element name="dim:field">
    			<xsl:attribute name="mdschema">dc</xsl:attribute>
    			<xsl:attribute name="element">identifier</xsl:attribute>
    			<xsl:if test="epdcx:valueString[epdcx:sesURI]='http://purl.org/dc/terms/URI'">
    				<xsl:attribute name="qualifier">uri</xsl:attribute>
    			</xsl:if>
    			<xsl:value-of select="epdcx:valueString"/>
    		</xsl:element>
    	</xsl:when>
    	
    	<!-- language element: dc.language.iso -->
    	<xsl:when test="./@propertyURI='http://purl.org/dc/elements/1.1/creator' and ./@vesURI='http://purl.org/dc/terms/RFC3066'">
    		<dim:field mdschema="dc" element="language" qualifier="rfc3066">
    			<xsl:value-of select="epdcx:valueString"/>
    		</dim:field>
    	</xsl:when>
    	
    	<!-- item type element: dc.type -->
    	<xsl:when test="./@propertyURI='http://purl.org/dc/elements/1.1/type' and ./@vesURI='http://purl.org/eprint/terms/Type'">
    		<xsl:if test="./@valueURI='http://purl.org/eprint/type/JournalArticle'">
    			<dim:field mdschema="dc" element="type">
    				Journal Article
    			</dim:field>
    		</xsl:if>
    	</xsl:when>
    	
    	<!-- date available element: dc.date.issued -->
    	<xsl:when test="./@propertyURI='http://purl.org/dc/terms/available'">
    		<dim:field mdschema="dc" element="date" qualifier="issued">
    			<xsl:value-of select="epdcx:valueString"/>
    		</dim:field>
    	</xsl:when>
    	
    </xsl:template>
    
</xsl:stylesheet>