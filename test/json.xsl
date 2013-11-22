<xsl:stylesheet version="2.0"
              xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:wo="http://weborganic.org/"
exclude-result-prefixes="wo">

<xsl:output method="xml" media-type="application/json"/>

<xsl:template match="/">
  <xsl:copy-of select="."/>
</xsl:template>

</xsl:stylesheet>