<xsl:stylesheet version="2.0"
              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:wo="http://weborganic.org/"
exclude-result-prefixes="wo">

<!-- <xsl:output method="wo:org.weborganic.aeson.JSONSerializer" media-type="application/json"/> -->

<!-- <xsl:output method="wo:net.sf.saxon.serialize.HTMLEmitter" media-type="text/html"/> -->

<xsl:template match="/">
  <xsl:copy-of select="."/>
</xsl:template>

</xsl:stylesheet>