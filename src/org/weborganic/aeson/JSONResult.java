package org.weborganic.aeson;

import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;

/**
 * A Result implementation automatically
 *
 * @author Christophe Lauret
 */
public class JSONResult extends SAXResult implements Result {

  /**
   *
   */
  public JSONResult() {
    super(new JSONSerializer());
  }

}
