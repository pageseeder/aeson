package org.weborganic.aeson;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;

public class Main {


  public static void main(String[] args) throws Exception {
    TransformerFactory factory = TransformerFactory.newInstance();
    Source xslt = new StreamSource(new File("test/idtransform.xsl"));
    Templates templates = factory.newTemplates(xslt);
    templates.getOutputProperties();

    Transformer transformer = factory.newTransformer(xslt);
    Source source = new SAXSource(new InputSource(new File("test/source.xml").toURI().toString()));
    StreamResult r = new StreamResult(System.out);
    Result result = JSONResult.newInstanceIfSupported(transformer, r);

    transformer.transform(source, result);
  }

}
