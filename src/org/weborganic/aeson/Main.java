package org.weborganic.aeson;

import java.io.File;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

public class Main {


  public static void main(String[] args) throws Exception {
    TransformerFactory factory = TransformerFactory.newInstance();
    Source xslt = new StreamSource(new File("test/idtransform.xsl"));
    Transformer transformer = factory.newTransformer(xslt);
    transformer.setOutputProperty("method", "xml");
    Source source = new SAXSource(new InputSource(new File("test/source.xml").toURI().toString()));
    JSONResult result = new JSONResult();

    transformer.transform(source, result);
  }

}
