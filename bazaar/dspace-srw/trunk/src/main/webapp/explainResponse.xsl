<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:srw="http://www.loc.gov/zing/srw/"
    xmlns:zr="http://explain.z3950.org/dtd/2.0/">

<xsl:import href="stdiface.xsl"/>

<xsl:output method="html" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN" doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>

<xsl:variable name="title"><xsl:value-of select="/srw:explainResponse/srw:record/srw:recordData/zr:explain/zr:databaseInfo/zr:title"/>
</xsl:variable>

<xsl:template match="/">
<xsl:call-template name="stdiface">
<xsl:with-param name="title" select="$title"/>
</xsl:call-template>
</xsl:template>

<xsl:template match="srw:explainResponse">
<script>
  <xsl:text>
    function mungeForm() {
      inform = document.CQLForm;
      outform = document.SRUForm;
      max = inform.maxIndex.value;
	if(outform.resultSetTTL.value==0) {
        if(outform.sortKeys.value.indexOf(',')) {
          outform.resultSetTTL.value=300;
        }
      }
      cql = "";
      prevIdx = 0;
      // Step through elements in form to create CQL
      for (var idx = 1; idx &lt;= max; idx++) {
        term = inform["term"+idx].value;
        if (term) {
          if (prevIdx) {
            cql += " " + inform["bool" + prevIdx].value + " "
          }
          if (term.indexOf(' ')) {
            term = '"' + term + '"';
          }
          cql += inform["index" + idx].value + " " + inform["relat" + idx].value + " " + term
          prevIdx = idx
        }
      }
      if (!cql) {
        alert("At least one term is required to search.");
        return false;
      }
      outform.query.value = cql
      outform.submit();
      return false;
    }

    function mungeScanForm() {
      inform = document.ScanIndexes;
      outform = document.ScanSubmit;
      index = inform.scanIndex.value;
      term = inform.term.value;
      relat = inform.relat.value;
      outform.scanClause.value = index + " " + relat +" \"" + term + "\""
      outform.submit();
      return false;
    }
</xsl:text>
</script>

<p>
<xsl:value-of select="srw:record/srw:recordData/zr:explain/zr:databaseInfo/zr:description"/>
</p>

<xsl:apply-templates select="srw:diagnostics"/>

<table cellspacing="0" class="layout">
<tr> 
<td><h1>Search</h1></td>
<td><h1>Browse</h1></td>
</tr>
<tr> 
<td width="50%" style="padding-right: 10px;"> 
  <xsl:call-template name="SearchForm"/>
</td>

<td width="50%">
  <xsl:call-template name="BrowseForm"/>
</td>
</tr>
</table>            
</xsl:template>


<xsl:template name="BrowseForm">
<xsl:call-template name="BrowseFormPart1"/>
<xsl:call-template name="BrowseFormPart2"/>
</xsl:template>


<xsl:template name="SearchForm">
<xsl:call-template name="SearchFormPart1"/>
<xsl:call-template name="SearchFormPart2"/>
</xsl:template>


<xsl:template name="SearchFormPart1">
<form name="CQLForm" onSubmit="return mungeForm();">
<input type="submit" value="Search" onClick="return mungeForm();"/>
<input type="hidden" name="maxIndex">
  <xsl:attribute name="value">
  <xsl:value-of
select="count(srw:record/srw:recordData/zr:explain/zr:indexInfo/zr:index)"/>
  </xsl:attribute>
  </input>
<table cellspacing="0" class="formtable">
<tr>
<th>Index</th>
<th>Relation</th>
<th>Term</th>
<th>Boolean</th>
</tr>

<xsl:for-each select="srw:record/srw:recordData/zr:explain/zr:indexInfo/zr:index">
<xsl:sort select="."/>
<tr>
<td><xsl:value-of select="zr:title"/>
<input type="hidden">
  <xsl:attribute name="name">index<xsl:value-of select="position()"/></xsl:attribute>
  <xsl:attribute name="value"><xsl:value-of select="zr:map[1]/zr:name/@set"/>.<xsl:value-of select="zr:map[1]/zr:name"/></xsl:attribute>
  </input>
</td>

<td>
<select>
  <xsl:attribute name="name">relat<xsl:value-of select="position()"/></xsl:attribute>
<option value="=">=</option>
<option value="exact">exact</option>
<option value="any">any</option>
<option value="all">all</option>
<option value="&lt;">&lt;</option>
<option value="&gt;">&gt;</option>
<option value="&lt;=">&lt;=</option>
<option value="&gt;=">&gt;=</option>
<option value="&lt;&gt;">not</option>
</select>
</td>
<td>
<input type="text" value="">
  <xsl:attribute name="name">term<xsl:value-of select="position()"/></xsl:attribute>
  </input>
</td>

<td>
<select>
  <xsl:attribute name="name">bool<xsl:value-of select="position()"/></xsl:attribute>
<option value="and">and</option>
<option value="or">or</option>
<option value="not">not</option>
</select>
</td>
</tr>
</xsl:for-each>
</table>
</form>
</xsl:template>


<xsl:template name="SearchFormPart2">
<form method="GET" name="SRUForm" onSubmit="mungeForm()">
  <input type="hidden" name="query" value=""/>
  <input type="hidden" name="version" value="1.1"/>
  <input type="hidden" name="operation" value="searchRetrieve"/>
  <table cellspacing="0" class="formtable">
    <tr>
      <td>Record Schema:</td>
      <td>
        <select name="recordSchema">
          <xsl:for-each select="srw:record/srw:recordData/zr:explain/zr:schemaInfo/zr:schema">
            <option>
              <xsl:attribute name="value">
                <xsl:value-of select="@identifier"/>
                </xsl:attribute>
              <xsl:value-of select="zr:title"/>
              </option>
            </xsl:for-each>
          </select>
        </td>
      </tr>
    <tr>
      <td>Number of Records:</td>
      <td>
        <input type="text" name="maximumRecords">
          <xsl:attribute name="value">
            <xsl:choose>
              <xsl:when test='srw:record/srw:recordData/zr:explain/zr:configInfo/zr:default[@type="numberOfRecords"]'>
                <xsl:value-of select='srw:record/srw:recordData/zr:explain/zr:configInfo/zr:default[@type="numberOfRecords"]'/>
                </xsl:when>
              <xsl:otherwise>
                <xsl:text>1</xsl:text>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:attribute>
          </input>
        </td>
      </tr>

    <tr>
      <td>Record Position:</td>
      <td><input type="text" name="startRecord" value="1"/></td>
      </tr>
    <tr>
      <td>Result Set TTL:</td>
      <td><input type="text" name="resultSetTTL" value="0"/></td>
      </tr>
    <tr>
      <td>Record Packing:</td>
      <td>
        <select name="recordPacking">
          <option value="xml">XML</option>
          <option value="string">String</option>
          </select>
        </td>
      </tr>
    <tr>
	<td>Record XPath:</td>
	<td><input type="text" name="recordXPath" value = ""/></td>
	</tr>	
    <tr>
	<td>Sort Keys:</td>
	<td><input type="text" name="sortKeys" value = ""/></td>
	</tr>	
    <tr>
	<td>Restrictor Summaries:</td>
	<td><input type="checkbox" name="x-info-5-restrictorSummary"/></td>
	</tr>	
    </table>
  <input type="submit" value="Search" onClick="return mungeForm();"/>
  </form>
</xsl:template>

<xsl:template name="BrowseFormPart1">
     <form name="ScanIndexes" onSubmit="return mungeScanForm();">
         <input type="submit" value="Browse" onClick="return mungeScanForm();"/>
<table cellspacing="0" class="formtable">
<tr>
<th>Index</th>
<th>Relation</th>
<th>Term</th>
</tr>
<tr>
       <td><select name="scanIndex">
         <xsl:for-each select="srw:record/srw:recordData/zr:explain/zr:indexInfo/zr:index">
           <xsl:sort select="."/>
           <option>
             <xsl:attribute name="value"><xsl:value-of select="zr:map[1]/zr:name/@set"/>.<xsl:value-of select="zr:map[1]/zr:name"/></xsl:attribute>
                <xsl:value-of select="zr:title"/>
            </option>
         </xsl:for-each>
         </select>
      </td>
      <td><select name="relat">
          <option value="=">=</option>
          <option value="exact">exact</option>
          <option value="any">any</option>
          <option value="all">all</option>
          <option value="&lt;">&lt;</option>
          <option value="&gt;">&gt;</option>
          <option value="&lt;=">&lt;=</option>
          <option value="&gt;=">&gt;=</option>
          <option value="&lt;&gt;">not</option>
        </select>
      </td>
      <td><input name="term" type="text" value = ""/>
      </td>
</tr>
</table>
    </form>
</xsl:template>

<xsl:template name="BrowseFormPart2">
    <form name="ScanSubmit" method="GET"  onSubmit="mungeScanForm()">
    <input type="hidden" name="operation" value="scan"/>
    <input type="hidden" name="scanClause" value=""/>
    <input type="hidden" name="version" value="1.1"/>
<table cellspacing="0" class="formtable">
    <tr>
      <td>Response Position:</td>
      <td>
        <input type="text" name="responsePosition" value="10" size="25"/>
      </td>
    </tr>
    <tr>
      <td>Maximum Terms:</td>
      <td>
        <input type="text" name="maximumTerms" value="20" size="5"/>
      </td>
    </tr>
</table>
         <input type="submit" value="Browse" onClick="return mungeScanForm();"/>
    </form>
</xsl:template>

</xsl:stylesheet>
