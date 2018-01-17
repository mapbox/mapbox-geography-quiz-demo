package com.mapbox.tappergeochallenge;

import java.util.*;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ParserSPARQLResultHandler extends DefaultHandler {

  private HashMap<String, HashMap> _result;
  private StringBuffer _buffer;
  private int _rowCurrent;
  private String _cellCurrent;

  public ParserSPARQLResultHandler() {
    super();
  }

  @Override
  public void startDocument() throws SAXException {
    super.startDocument();
    _result = new HashMap();
  }

  @Override
  public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
    if (name.equalsIgnoreCase("sparql")) {        //<>//
      _result.put("result", new HashMap<String, ArrayList<String>>());
    } else if (name.equalsIgnoreCase("head")) {
      _result.get("result").put("variables", new ArrayList<String>());
    } else if (name.equalsIgnoreCase("variable")) {
      ((ArrayList) _result.get("result").get("variables")).add(attributes.getValue("name"));
    } else if (name.equalsIgnoreCase("results")) {
      _rowCurrent = -1;
      _result.get("result").put("rows", new ArrayList<List>());
    } else if (name.equalsIgnoreCase("result")) {
      _rowCurrent++;
      ((ArrayList) _result.get("result").get("rows")).add(new HashMap<String, Object>());
    } else if (name.equalsIgnoreCase("binding")) {
      _buffer = new StringBuffer();
      _cellCurrent = attributes.getValue("name");
    } else if (_cellCurrent != null) {
      addValueCurrentRow(_cellCurrent + " type", name);
      if (attributes.getValue("xml:lang") != null) {
        addValueCurrentRow(_cellCurrent + " lang", attributes.getValue("xml:lang"));
      }
      if (attributes.getValue("datatype") != null) {
        addValueCurrentRow(_cellCurrent + " datatype", attributes.getValue("datatype"));
      }
    }
  }

  @Override
  public void endElement(String uri, String localName, String name) throws SAXException {
    String value = "";
    if (_buffer != null) {
      value = _buffer.toString().trim();
    } //<>//

    if (name.equalsIgnoreCase("binding")) {

      if (value.length() == 0) {
        return;
      }

      if (!isKeyExistCurrentRow(_cellCurrent + " type")) {
        addValueCurrentRow(_cellCurrent + " type", null);
      }

      if (getValueCurrentRow(_cellCurrent + " type") == "uri") {
        addValueCurrentRow(_cellCurrent, value);
      } else if (getValueCurrentRow(_cellCurrent + " type") == "bnode") {
        addValueCurrentRow(_cellCurrent, value);
      } else if (getValueCurrentRow(_cellCurrent + " type") == "literal") {
        if (isKeyExistCurrentRow(_cellCurrent + " datatype")) {
          if (getValueCurrentRow(_cellCurrent + " datatype") == "http://www.w3.org/2001/XMLSchema#double" ||
            getValueCurrentRow(_cellCurrent + " datatype") == "http://www.w3.org/2001/XMLSchema#decimal"
            ) {
            addValueCurrentRow(_cellCurrent, new Float(value));
          } else if (getValueCurrentRow(_cellCurrent + " datatype") == "http://www.w3.org/2001/XMLSchema#integer") {
            addValueCurrentRow(_cellCurrent, new Integer(value));
          } else if (getValueCurrentRow(_cellCurrent + " datatype") == "http://www.w3.org/2001/XMLSchema#boolean") {
            if (value == "true") {
              addValueCurrentRow(_cellCurrent, true);
            } else {
              addValueCurrentRow(_cellCurrent, false);
            }
          } else {
            addValueCurrentRow(_cellCurrent, value);
          }
        } else {
          addValueCurrentRow(_cellCurrent, value);
        }
      } else {
        addValueCurrentRow(_cellCurrent, value);
      }
      _cellCurrent = null;
      _buffer = null;
    }
  }

  private Object getValueCurrentRow(String key) {
    return ((HashMap<String, Object>) ((ArrayList) _result.get("result").get("rows")).get(_rowCurrent)).get(key);
  }

  private void addValueCurrentRow(String key, Object value) {
    ((HashMap<String, Object>) ((ArrayList) _result.get("result").get("rows")).get(_rowCurrent)).put(key, value);
  }

  private boolean isKeyExistCurrentRow(String key) {
    return null != ((HashMap<String, Object>) ((ArrayList) _result.get("result").get("rows")).get(_rowCurrent)).get(key);
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    String lecture = new String(ch, start, length);
    if (_cellCurrent != null && _buffer != null) {
      _buffer.append(lecture);
    }
  }

  HashMap<String, HashMap> getResult() {
    return _result;
  }

}