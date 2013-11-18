/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.weborganic.aeson;

import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This serializer is a content handler implementation so that it can be used directly against an
 * XML instance or wrapped inside a SAXResult implementation.
 *
 * <p>When used as part of a <code>SAXResult</code>, it is preferable to use the dedicated
 * <code>JSONResult</code> class.
 *
 * @author Christophe Lauret
 * @version 17 November 2013
 */
public class JSONSerializer extends DefaultHandler implements ContentHandler {

  /**
   * Namespace used for instructions understood by this serializer.
   */
  public static final String NS_URI = "http://weborganic.org/JSON";

  /**
   * The type of serialization to apply to objects
   *
   * @author Christophe Lauret
   */
  private enum JSONSerialType {OBJECT, STRING, NUMBER, BOOLEAN, ARRAY, AUTO};

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

  /**
   * Maintains instructions for the JSON serialization at each hierarchical level of the structure.
   */
  private final Deque<JSONSerialInstruction> instructions = new ArrayDeque<JSONSerializer.JSONSerialInstruction>();

  /**
   * Keeps track of the context.
   */
  private final Deque<JSONContext> context = new ArrayDeque<JSONContext>();

  /**
   * The document locator used when reporting warnings.
   */
  private Locator locator = null;

  // Constructors
  // =============================================================================================

  /**
   * Zero-argument default constructor.
   *
   * <p>Parsed output will go to <code>System.out</code>.
   */
  public JSONSerializer() {
    this.json = Json.createGenerator(System.out);
  }

  /**
   * Construct a JSONSerializer from a byte stream.
   *
   * @param out A valid OutputStream.
   */
  public JSONSerializer(OutputStream out) {
    this.json = Json.createGenerator(out);
  }

  /**
   * Construct a JSONSerializer from a character stream.
   *
   * @param writer A valid character stream.
   */
  public JSONSerializer(Writer w) {
    this.json = Json.createGenerator(w);
  }

  // Content Handler implementations
  // =============================================================================================

  @Override
  public void startDocument() throws SAXException {
  }

  @Override
  public void endDocument() throws SAXException {
    this.json.close();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if (NS_URI.equals(uri)) {
      handleJSONElement(localName, atts);
    } else {
      handleElement(localName, atts);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (NS_URI.equals(uri)) {
      if ("array".equals(localName) || "object".equals(localName)) {
        this.context.pop();
        this.json.writeEnd();
      }
    } else {
      this.context.pop();
      this.json.writeEnd();
    }
  }

  @Override
  public void warning(SAXParseException ex) {
    StringBuffer err = new StringBuffer();
    err.append(ex.getMessage());
    if (ex.getLineNumber() != -1)
      err.append(" at line ").append(ex.getLineNumber());
    if (ex.getColumnNumber() != -1)
      err.append(" column ").append(ex.getColumnNumber());
    if (ex.getException() != null) {
      err.append("; caused by ").append(ex.getException().getClass().getSimpleName());
      err.append(": ").append(ex.getException().getMessage());
    }
    System.err.println(err);
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  // Helper methods
  // =============================================================================================

  /**
   * Filter out attributes which are namespace declarations (xmlns:*), XML attributes like (xml:*) and
   * JSON serialization attributes (json:*).
   *
   * @param uri the namespace URI
   * @return whether the attribute belonging to that namespace should be considered.
   */
  private static boolean filterNamespace(String uri) {
    return !(NS_URI.equals(uri)
         || "http://www.w3.org/2000/xmlns/".equals(uri)
         || "http://www.w3.org/XML/1998/namespace".equals(uri));
  }

  /**
   * Handles <code>json:*</code> elements and indicates whether the handler should continue.
   *
   * @param localName
   * @param atts
   *
   * @throws SAXException
   */
  private void handleJSONElement(String localName, Attributes atts) {
    String name = atts.getValue(NS_URI, "name");
    if (name == null && this.context.peek() == JSONContext.OBJECT) {
      warning(new SAXParseException("Attribute json:name must be used to specify array/object name", this.locator));
      name = localName;
    }
    if ("array".equals(localName)) {

      // A JavaScript array explicitly
      if (this.context.peek() == JSONContext.OBJECT)
        this.json.writeStartArray(name);
      else
        this.json.writeStartArray();

      this.context.push(JSONContext.ARRAY);

    } else if ("object".equals(localName)) {

      // A JavaScript object explicitly
      if (this.context.peek() == JSONContext.OBJECT)
        this.json.writeStartObject(name);
      else
        this.json.writeStartObject();

      this.context.push(JSONContext.OBJECT);
      // Serialize the attributes as value pairs
      handleValuePairs(atts);

    } else {
      // An element we don't understand
      warning(new SAXParseException("Unknown JSON element:"+localName, this.locator));
    }
  }

  /**
   * Handles <code>json:*</code> elements and indicates whether the handler should continue.
   *
   * @param localName
   * @param atts
   *
   * @throws SAXException
   */
  private void handleElement(String localName, Attributes atts) {
    String name = atts.getValue(NS_URI, "name");

    // Start object
    if (this.context.peek() == JSONContext.OBJECT) {
      if (name == null) name = localName;
      this.json.writeStartObject(name);
    } else {
      if (atts.getValue(NS_URI, "name") != null) {
        warning(new SAXParseException("Attribute json:name is ignored in array/document context", this.locator));
      }
      this.json.writeStartObject();
    }
    this.context.push(JSONContext.OBJECT);

    // Serialize the attributes as value pairs
    handleValuePairs(atts);
  }

  /**
   * Serialize the attributes as value pairs within the context object.
   *
   * @param atts The attributes on the current element
   * @throws SAXException
   */
  private void handleValuePairs(Attributes atts) {
    JSONSerialInstruction instruction  = new JSONSerialInstruction(atts);
    // Serialize the name value pairs from the attributes
    for (int i=0; i < atts.getLength(); i++) {
      if (filterNamespace(atts.getURI(i))) {
        String name = atts.getLocalName(i);
        String value = atts.getValue(i);
        JSONSerialType type = instruction.getSerialType(name);
        switch (type) {
          case NUMBER:
            asNumber(name, value);
            break;
          case BOOLEAN:
            asBoolean(name, value);
            break;
          default:
            this.json.write(name, value);
        }
      }
    }
  }

  /**
   * Attempts to write the specified name/value pair as a number.
   *
   * <p>Will fallback on a string and report a warning if unable to convert to a number.
   *
   * @param name  The JSON name to write.
   * @param value The JSON value to write.
   */
  private void asNumber(String name, String value) {
    try {
      if (value.indexOf('.') != -1) {
        double number = Double.parseDouble(value);
        this.json.write(name, number);
      } else {
        long number = Long.parseLong(value);
        this.json.write(name, number);
      }
    } catch (NumberFormatException ex) {
      this.json.write(name, value);
      warning(new SAXParseException("Unable to convert attribute '"+name+"' to a number", this.locator, ex));
    }
  }

  /**
   * Attempts to write the specified name/value pair as a boolean.
   *
   * <p>Will fallback on a string and report a warning if unable to convert to a boolean.
   *
   * @param name  The JSON name to write.
   * @param value The JSON value to write.
   */
  private void asBoolean(String name, String value) {
    if ("true".equals(value)) {
      this.json.write(name, true);
    } else if ("false".equals(value)) {
      this.json.write(name, false);
    } else {
      this.json.write(name, value);
      warning(new SAXParseException("Unable to convert attribute '"+name+"' to a boolean", this.locator));
    }
  }

  // Helper inner classes
  // =============================================================================================

  private static class JSONSerialInstruction {

    private static final String[] NIL = new String[]{};

    /**
     * Names of elements to be converted to JavaScript numbers.
     */
    private final String[] numbers;

    /**
     * Names of elements to be converted to JavaScript booleans.
     */
    private final String[] booleans;

    /**
     * Names of elements to be converted to JavaScript strings.
     */
    private final String[] strings;

    /**
     *
     * @param atts the attributes on the current element
     */
    public JSONSerialInstruction(Attributes atts) {
      String toBoolean = atts.getValue(NS_URI, "boolean");
      this.booleans = toBoolean != null ? toBoolean.split(" ") : NIL;
      String toNumber = atts.getValue(NS_URI, "number");
      this.numbers = toNumber != null?  toNumber.split(" ") : NIL;
      String toString = atts.getValue(NS_URI, "string");
      this.strings = toString != null? toString.split(" ") : NIL;
    }

    public boolean isString(String name) {
      for (String s : this.strings)
        if (s.equals(name)) return true;
      return false;
    }

    public boolean isNumber(String name) {
      for (String n : this.numbers)
        if (n.equals(name)) return true;
      return false;
    }

    public boolean isBoolean(String name) {
      for (String b : this.booleans)
        if (b.equals(name)) return true;
      return false;
    }

    /**
     *
     * @param name
     * @return
     */
    public JSONSerialType getSerialType(String name) {
      if (isBoolean(name)) return JSONSerialType.BOOLEAN;
      if (isNumber(name)) return JSONSerialType.NUMBER;
      if (isString(name)) return JSONSerialType.STRING;
      return JSONSerialType.AUTO;
    }

  }
}
