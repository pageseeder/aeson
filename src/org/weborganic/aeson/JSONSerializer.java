/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.weborganic.aeson;

import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Christophe Lauret
 * @version 16 November 2013
 */
public class JSONSerializer extends DefaultHandler implements ContentHandler {

  public static final String NS_URI = "http://weborganic.org/JSON";

  /**
   * The type of serialization to apply to objects
   *
   * @author Christophe Lauret
   */
  private enum JSONSerialType {OBJECT, STRING, NUMBER, BOOLEAN, ARRAY};

  /**
   * The type of serialization to apply to objects
   *
   * @author Christophe Lauret
   */
  private enum JSONContext {NIL, OBJECT, ARRAY};

  /**
   * JSON Generator from JSON Processing API.
   */
  private final JsonGenerator json;

  private final Deque<JSONSerialInstruction> instructions = new ArrayDeque<JSONSerializer.JSONSerialInstruction>();

  /**
   * Keeps track of the context.
   */
  private final Deque<JSONContext> context = new ArrayDeque<JSONContext>();

  public JSONSerializer() {
    this.json = Json.createGenerator(System.out);
  }

  public JSONSerializer(Writer w) {
    this.json = Json.createGenerator(w);
  }

  @Override
  public void startDocument() throws SAXException {
  }

  @Override
  public void endDocument() throws SAXException {
    this.json.close();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    // Grab the name (may be specified using attribute)
    String name = atts.getValue(NS_URI, "name");
    if (name == null) name = localName;
    if (NS_URI.equals(uri)) {
      if ("array".equals(localName)) {
        this.json.writeStartArray(name);
        this.context.push(JSONContext.ARRAY);
      } else {
        System.err.println("Unknown JSON element:"+qName);
      }
    } else {
      // Serialize object
      if (this.context.peek() == JSONContext.OBJECT) {
        this.json.writeStartObject(name);
      } else {
        this.json.writeStartObject();
      }
      this.context.push(JSONContext.OBJECT);
      // Serialize the name value pairs from the attributes
      for (int i=0; i < atts.getLength(); i++) {
        if (filterNamespace(atts.getURI(i))) {
          String pname = atts.getLocalName(i);
          String value = atts.getValue(i);
          this.json.write(pname, value);
        }
      }
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (NS_URI.equals(uri)) {
      if ("array".equals(localName)) {
        this.context.pop();
        this.json.writeEnd();
      }
    } else {
      this.context.pop();
      this.json.writeEnd();
    }
  }

  private boolean filterNamespace(String uri) {
    return !(NS_URI.equals(uri)
         || "http://www.w3.org/2000/xmlns/".equals(uri)
         || "http://www.w3.org/XML/1998/namespace".equals(uri));
  }

  private static class JSONSerialInstruction {

    /**
     *
     * @param atts
     */
    public JSONSerialInstruction(Attributes atts) {

    }

  }
}
