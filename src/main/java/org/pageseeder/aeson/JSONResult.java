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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

/**
 * A Result implementation automatically writing out JSON.
 *
 *
 * @see <a href="http://tools.ietf.org/html/rfc4627">The application/json Media Type for
 *  JavaScript Object Notation (JSON)</a>
 *
 * @author Christophe Lauret
 * @version 17 November 2013
 */
public class JSONResult extends SAXResult implements Result {

  /**
   * Zero-argument default constructor.
   *
   * <p>transformation results will go to <code>System.out</code>.
   */
  public JSONResult() {
    super(new JSONSerializer());
  }

  /**
   * Construct a JSONResult from a File.
   *
   * @param f Must a non-null File reference.
   */
//  public JSONResult(File f) {
//    super(new JSONSerializer(new FileWriter(f)));
//  }

  /**
   * Construct a JSONResult from a byte stream.
   *
   * @param out A valid OutputStream.
   */
  public JSONResult(OutputStream out) {
    super(new JSONSerializer(out));
  }

  /**
   * Construct a JSONResult from a URL.
   *
   * @param systemId Must conforms to the URI syntax.
   */
//  public JSONResult(String systemId) {
//
//  }

  /**
   * Construct a JSONResult from a character stream.
   *
   * <p>It is generally preferable to use a byte stream so that the encoding can controlled by the xsl:output
   * declaration; but can be convenient when using StringWriter
   *
   * @param writer A valid character stream.
   */
  public JSONResult(Writer writer) {
    super(new JSONSerializer(writer));
  }

  // Static helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns a new instance of the
   *
   * @param t
   *
   *
   * @return
   */
  public static Result newInstanceIfSupported(Transformer t, StreamResult result) {
    return supports(t)? newInstance(result) : result;
  }

  /**
   * Returns a new instance from the specified stream result.
   *
   * @param result a non-null stream result instance.
   *
   * @return a new <code>JSONResult</code> instance using the same properties as the stream result.
   */
  public static JSONResult newInstance(StreamResult result) {
    // try to set the JSON result using the byte stream from the stream result
    OutputStream out = result.getOutputStream();
    JSONResult json = null;
    if (out != null) {
      json = new JSONResult(out);
    } else {
      // try to set the JSON result using the character stream from the stream result
      Writer writer = result.getWriter();
      if (writer != null) {
        json = new JSONResult(writer);
      } else {
        String systemId = result.getSystemId();
        if (systemId != null) {
          try {
            File f = new File(URI.create(systemId));
            FileOutputStream o = new FileOutputStream(f);
            json = new JSONResult(o);
          } catch (IOException ex) {
            // TODO: Handle this proper
            ex.printStackTrace();
          }
        } else {
          json = new JSONResult();
        }
      }
    }
    json.setSystemId(result.getSystemId());
    return json;
  }

  /**
   * Indicates whether the specified transformer based on its output properties.
   *
   * <p>the transformer is considered to support this Result type if it uses the "xml" method and
   * specifies the media type as "application/json".
   *
   * @param t the XSLT transformer implementation
   *
   * @return <code>true</code> if it matches the conditions above;
   *         <code>false</code> otherwise.
   */
  public static boolean supports(Transformer t) {
    String method = t.getOutputProperty("method");
    String media = t.getOutputProperty("media-type");
    return "xml".equals(method) && "application/json".equals(media);
  }

}
