/*
 * Copyright 2010-2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.aeson;

import java.io.OutputStream;
import java.io.Writer;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import org.pageseeder.aeson.JSONState.JSONContext;
import org.pageseeder.aeson.JSONState.JSONType;
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
 * @version 23 November 2013
 */
public final class JSONSerializer extends DefaultHandler implements ContentHandler {

  /**
   * Namespace used for instructions understood by this serializer.
   */
  public static final String NS_URI = "http://pageseeder.org/JSON";

  /**
   * JSON Generator from JSON Processing API.
   */
  private final JsonGenerator json;

  /**
   * Maintains the state of the serialization.
   */
  private final JSONState state = new JSONState();

  /**
   * The buffer for property values.
   */
  private final StringBuilder buffer = new StringBuilder();

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
    this.state.pushState();
  }

  @Override
  public void endDocument() throws SAXException {
    this.state.popState();
    this.json.close();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    try {
      if (!this.state.isContext(JSONContext.NULL)) {
        if (NS_URI.equals(uri)) {
          handleJSONElement(localName, atts);
        } else {
          handleElement(localName, atts);
        }
      } else {
        this.state.pushState(JSONContext.NULL, atts, "");
        warning(new SAXParseException("Ignoring element "+qName+" in null context", this.locator));
      }
    } catch (Exception ex) {
      throw new SAXException(ex);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
      // Preserve what we need of previous context
      JSONContext wasContext = this.state.currentContext();
      String wasName = this.state.currentName();

      // Then return to parent
      this.state.popState();

      if (wasContext != JSONContext.NULL) {
        if (NS_URI.equals(uri)) {

          // One of the json elements
          if ("array".equals(localName) || "object".equals(localName)) {
            this.json.writeEnd();
          }

        } else if (wasContext == JSONContext.VALUE) {

          // A property
          String name = this.state.isContext(JSONContext.OBJECT)? wasName : null;
          String value = this.buffer.toString();
          JSONType type = this.state.getType(localName);
          writeProperty(name, value, type);
          this.buffer.setLength(0);

        } else {
          // A regular element
          this.json.writeEnd();
        }
      }
    } catch (Exception ex) {
      throw new SAXException(ex);
    }
  }

  @Override
  public void warning(SAXParseException ex) {
    // Construct a message for the warning
    StringBuilder message = new StringBuilder();
    String systemId = ex.getSystemId();
    if (systemId != null) {
      int sol = systemId.lastIndexOf('/');
      message.append('[').append(sol != -1? systemId.substring(sol+1) : systemId).append("] ");
    }
    message.append(ex.getMessage());
    if (ex.getLineNumber() != -1)
      message.append(" at line ").append(ex.getLineNumber());
    if (ex.getColumnNumber() != -1)
      message.append(" column ").append(ex.getColumnNumber());
    if (ex.getException() != null) {
      message.append("; caused by ").append(ex.getException().getClass().getSimpleName());
      message.append(": ").append(ex.getException().getMessage());
    }
    // And print on the console by default
    System.err.println(message);
  }

  @Override
  public void characters(char[] ch, int start, int len) throws SAXException {
    if (this.state.isContext(JSONContext.VALUE)) {
      this.buffer.append(ch, start, len);
    }
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  // Helper methods
  // =============================================================================================

  /**
   * Filter out namespace declarations (xmlns:*), XML attributes like (xml:*) and JSON
   * serialization attributes (json:*).
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
   * Indicates whether the specified attributes include at least one attribute
   * that should be serialized as a property.
   *
   * @param atts the attributes to loop through
   * @return <code>true</code> if at least one attribute matched;
   *         <code>false</code> otherwise.
   */
  private static boolean hasProperty(Attributes atts) {
    final int upto = atts.getLength();
    for (int i = 0; i < upto; i++) {
      if (filterNamespace(atts.getURI(i))) return true;
    }
    return false;
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
    if (name == null && this.state.isContext(JSONContext.OBJECT)) {
      warning(new SAXParseException("Attribute json:name must be used to specify array/object name", this.locator));
      name = localName;
    }
    if ("array".equals(localName)) {

      // A JavaScript array explicitly
      if (this.state.isContext(JSONContext.OBJECT))
        this.json.writeStartArray(name);
      else
        this.json.writeStartArray();

      this.state.pushState(JSONContext.ARRAY, atts, name);

    } else if ("object".equals(localName)) {

      // A JavaScript object explicitly
      if (this.state.isContext(JSONContext.OBJECT))
        this.json.writeStartObject(name);
      else
        this.json.writeStartObject();

      this.state.pushState(JSONContext.OBJECT, atts, name);

      // Serialize the attributes as value pairs
      handleValuePairs(atts);

    } else if ("null".equals(localName)) {

      // A JavaScript null explicitly
      if (this.state.isContext(JSONContext.ROOT)) {
        // Illegal in root context!
        warning(new SAXParseException("Illegal null as root, substituting for empty object", this.locator));
        this.json.writeStartObject();
        this.json.writeEnd();
      } else if (this.state.isContext(JSONContext.OBJECT))
        this.json.writeNull(name);
      else
        this.json.writeNull();

      this.state.pushState(JSONContext.NULL, atts, name);

    } else {
      this.state.pushState(JSONContext.OBJECT, atts, name);
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

    // If the element name matches of the types, it's a property
    if (this.state.getType(localName) != JSONType.DEFAULT) {
      if (hasProperty(atts)) {
        warning(new SAXParseException("Element "+localName+" is mapped to a property, also has properties!", this.locator));
      }
      if (name == null) name = localName;
      this.state.pushState(JSONContext.VALUE, atts, name);

    } else {
      // Start object
      if (this.state.isContext(JSONContext.OBJECT)) {
        if (name == null) name = localName;
        this.json.writeStartObject(name);
      } else {
        if (atts.getValue(NS_URI, "name") != null) {
          warning(new SAXParseException("Attribute json:name is ignored in array/document context", this.locator));
        }
        this.json.writeStartObject();
      }
      this.state.pushState(JSONContext.OBJECT, atts, name);

      // Serialize the attributes as value pairs
      handleValuePairs(atts);
    }
  }

  /**
   * Serialize the attributes as value pairs within the context object.
   *
   * @param atts The attributes on the current element
   */
  private void handleValuePairs(Attributes atts) {
    // Serialize the name value pairs from the attributes
    final int upto = atts.getLength();
    for (int i=0; i < upto; i++) {
      if (filterNamespace(atts.getURI(i))) {
        String name = atts.getLocalName(i);
        String value = atts.getValue(i);
        JSONType type = this.state.getType(name);
        writeProperty(name, value, type);
      }
    }
  }

  /**
   * Write the property
   *
   * @param name  The name of the property (may be <code>null</code>)
   * @param value The value of the property
   * @param type  The type of property
   */
  private void writeProperty(String name, String value, JSONType type) {
    switch (type) {
      case NUMBER:
        asNumber(name, value);
        break;
      case BOOLEAN:
        asBoolean(name, value);
        break;
      case NULL:
        asNull(name);
        break;
      default:
        asString(name, value);
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
        if (name != null)
          this.json.write(name, number);
        else
          this.json.write(number);
      } else {
        long number = Long.parseLong(value);
        if (name != null)
          this.json.write(name, number);
        else
          this.json.write(number);
      }
    } catch (NumberFormatException ex) {
      asString(name, value);
      warning(new SAXParseException("Unable to convert attribute '"+name+"' to a number", this.locator, ex));
    }
  }

  /**
   * Attempts to write the specified name/value pair as a boolean.
   *
   * <p>Will fallback on a string and report a warning if unable to convert to a boolean.
   *
   * @param name  The JSON name to write (may be <code>null</code>)
   * @param value The JSON value to write.
   */
  private void asBoolean(String name, String value) {
    if ("true".equals(value)) {
      if (name != null)
        this.json.write(name, true);
      else
        this.json.write(true);
    } else if ("false".equals(value)) {
      if (name != null)
        this.json.write(name, false);
      else
        this.json.write(false);
    } else {
      asString(name, value);
      warning(new SAXParseException("Unable to convert attribute '"+name+"' to a boolean", this.locator));
    }
  }

  /**
   * Attempts to write the specified name/value pair as a <code>null</code>.
   *
   * @param name  The JSON name to write (may be <code>null</code>)
   * @param value The JSON value to write.
   */
  private void asNull(String name) {
    if (name != null)
      this.json.write(name, JsonValue.NULL);
    else
      this.json.write(JsonValue.NULL);
  }

  /**
   * Writes the specified string property as a string.
   *
   * @param name  The JSON name to write (may be <code>null</code>)
   * @param value The JSON value to write.
   */
  private void asString(String name, String value) {
    if (name != null)
      this.json.write(name, value);
    else
      this.json.write(value);
  }

}
