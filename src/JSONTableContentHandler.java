package tsv2json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class JSONTableContentHandler extends DefaultHandler {
  
  private final String[] columnNames;
  private final String[] hashColumnNames;
//  private HashMap<Integer, Boolean> map;
//  private HashMap<Integer, ArrayList<String>> map;
  private HashMap<Integer, ArrayList<Long>> map;
  private String dirname;
  private String subfolder;
  private int lineNumber;
  
  private int columnIndex;
  private JSONObject obj;
  
  private long jsonFileCount = 0;

  public JSONTableContentHandler(String[] columnNames, String[] hashColumnNames) {
    this.columnNames = columnNames;
    this.hashColumnNames = hashColumnNames;
    map = new HashMap<>();
    this.dirname = "";
    this.lineNumber = 0;
  }
  public void setOutputDirectory(String dirname) {
    this.lineNumber = 0;
    if (dirname.isEmpty()) {
      return;
    }
    this.dirname = dirname.endsWith("/") ? dirname : dirname + "/";
    File dir = new File(dirname);
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }
  
  @Override
  public void startElement(String uri, String localName, String qName,
          Attributes attributes) throws SAXException {
    switch (qName) {
      case "tr":
        obj = new JSONObject();
        columnIndex = -1;
        lineNumber++;
        break;
      case "td":
        columnIndex++;
        break;
    }
  }
  
  @Override
  public void endElement(String uri, String localName, String qName)
          throws SAXException {
    if (qName.equals("tr")) {
      if (columnIndex != columnNames.length - 1) {
        while (columnIndex < columnNames.length) {
          obj.put(columnNames[columnIndex], "");
          columnIndex++;
        }
        System.out.println(this.dirname + " Line " + lineNumber + " is not in correct format");
        System.out.println(obj);
      }
      if (hashColumnNames != null) { // do deduplication
        String str = "";
        for (String col : hashColumnNames) {
          str += obj.get(col) + " ";
        }
//        long hash = SimHash.fvnHash64(str.toLowerCase());
//        if (map.containsKey(hash)) {
//          return;
//        }
//        else {
//          map.put(hash, Boolean.TRUE);
//        }
        
        long simhash = SimHash.simHash64(str.toLowerCase());
        for (int i = 0; i < 4; i++) {
          int key = (short)(simhash >> i*16);
          if (map.containsKey(key)) {
//            ArrayList<String> hashList = map.get(key);
//            for (String h : hashList) {
//              int dist = SimHash.hammingDistance(simhash, Long.parseLong(h.split(",")[0]));
//              if (dist < 4) {
//                if (dist >= 0) {
//                  System.out.println(dirname + " " + lineNumber + " is a duplicate of line " + h.split(",")[1]);
//                }
//                return;
//              }
//            }
            ArrayList<Long> hashList = map.get(key);
            for (long h : hashList) {
              // if the hamming distance < 4, I consider it as a duplicate
              // and stop processing
              if (SimHash.hammingDistance(simhash, h) < 4) {
                return;
              }
            }
          }
        }
        // not a duplicate, store it in the map
        for (int i = 0; i < 4; i++) {
          int key = (short)(simhash >> i*16);
          if (!map.containsKey(key)) {
            map.put(key, new ArrayList<Long>());
          }
//          ArrayList<String> hashList = map.get(key);
//          hashList.add(simhash + "," + dirname + " " + lineNumber);
          ArrayList<Long> hashList = map.get(key);
          hashList.add(simhash);
        }
      }
      // generate json file for this row
      try {
        if (jsonFileCount % 40000 == 0) {
          subfolder = String.format("%02d", jsonFileCount / 40000) + "json/";
          File dir = new File(dirname + subfolder);
          if (!dir.exists()) {
            dir.mkdirs();
          }
        }
        jsonFileCount++;
        FileWriter output = new FileWriter(dirname + subfolder + String.format("%07d", jsonFileCount) + ".json");
        obj.put("id", String.format("%07d", jsonFileCount));
        long first = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(obj.get("firstSeenDate_dt").toString()).getTime();
        long last = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(obj.get("lastSeenDate_dt").toString()).getTime();
        int days = (int)Math.abs((last-first)/(1000*60*60*24));
        obj.put("days_i", days);
        output.write(obj.toJSONString());
        output.close();
      } catch (IOException ex) {
        System.out.println("IO Exception when output line " + lineNumber);
        Logger.getLogger(JSONTableContentHandler.class.getName()).log(Level.SEVERE, null, ex);
      } catch (ParseException ex) {
        Logger.getLogger(JSONTableContentHandler.class.getName()).log(Level.SEVERE, null, ex);
      }
      
    }
  }

  @Override
  public void characters(char[] ch, int start, int length)
          throws SAXException {
    if (!columnNames[columnIndex].equals("")) {
      String str = new String(ch, start, length);
      // it's a date, convert it to solr date format
      if (columnNames[columnIndex].endsWith("dt")) {
        SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        try {
          str = out.format(new SimpleDateFormat("yyyy-MM-dd").parse(str));
        } catch (ParseException ex) {
          System.out.println("Date format exception " + dirname + " " + lineNumber);
          Logger.getLogger(JSONTableContentHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      obj.put(columnNames[columnIndex], str);
    }
  }
  
  public long getJsonFileCount() {
    return jsonFileCount;
  }
}
