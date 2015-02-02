package tsv2json;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;


public class TSV2JSON {
  private final TSVParser parser;
  private final JSONTableContentHandler jsonHandler;
  
  public TSV2JSON(String[] columnNames, String dir) {
    this(columnNames, null, dir);
  }
  
  public TSV2JSON(String[] columnNames, String[] hashColumnNames, String dir) {
    parser = new TSVParser();
    jsonHandler = new JSONTableContentHandler(columnNames, hashColumnNames);
    jsonHandler.setOutputDirectory(dir);
  }
  
  public long getJsonFileCount() {
    return jsonHandler.getJsonFileCount();
  }
  
  public static void main(String[] args) throws Exception {
    
    String[] columnNames = {
      "postedDate_dt",
      "location1_s",
      "department_s",
      "title_s",
      "",
      "salary_s",
      "start_s",
      "duration_s",
      "jobtype_s",
      "applications_s",
      "company_s",
      "contactPerson_s",
      "phoneNumber_s",
      "faxNumber_s",
      "location2_s",
      "latitude_d",
      "longitude_d",
      "firstSeenDate_dt",
      "url_s",
      "lastSeenDate_dt",
    };
    // Fields used to generate simhash to do deduplication
    String[] hashColumnNames = {
      "department_s",
      "title_s",
      "jobtype_s",
      "applications_s",
      "company_s",
      "contactPerson_s",
      "location2_s",
    };
    
    final String name = args[1]; // path of the tsv folder
    
    TSV2JSON tsv2json = new TSV2JSON(columnNames, hashColumnNames, name); // with deduplication
//    TSV2JSON tsv2json = new TSV2JSON(columnNames); // without deduplication

    
    
    File item = new File(name);
    if (item.isDirectory()) {
      for (File file : item.listFiles()) {
        if (file.isFile()) {
          tsv2json.processFile(file.getPath());
        }
      }
    }
    else {
      tsv2json.processFile(item.getPath());
    }
    
    // Print the number of generated json files
    System.out.println(tsv2json.getJsonFileCount());
  }

  public void processFile(String filepath) throws Exception {
    
    InputStream input = new FileInputStream(new File(filepath));
    
    // Use tsv file name + "-dir" as the directory to output json files
    
    Metadata metadata = new Metadata();
    metadata.add("charset", "ISO-8859-1"); // given tsv files use this encoding

    try {
      parser.parse(
              input,
              new BodyContentHandler(jsonHandler),
              metadata,
              new ParseContext()
      );
    } finally {
      input.close();
    }
  }
}
