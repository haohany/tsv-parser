package tsv2json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Set;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class TSVParser extends AbstractParser {

  private static final Set<MediaType> SUPPORTED_TYPES = Collections.singleton(MediaType.text("tab-separated-values"));
  public static final String TSV_MIME_TYPE = "text/tab-separated-values";

  @Override
  public Set<MediaType> getSupportedTypes(ParseContext context) {
    return SUPPORTED_TYPES;
  }

  @Override
  public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
    
    metadata.set(Metadata.CONTENT_TYPE, TSV_MIME_TYPE);

    XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
    xhtml.startDocument();
    xhtml.startElement("table");
    
    BufferedReader reader;
    if (metadata.get("charset") != null) {
      reader = new BufferedReader(new InputStreamReader(stream, metadata.get("charset")));
    }
    else {
      reader = new BufferedReader(new InputStreamReader(stream));
    }
    
    String line;
    while ((line = reader.readLine()) != null) {
      xhtml.startElement("tr");
      String values[] = line.split("\t");
      for (String val: values) {
        xhtml.startElement("td");
        xhtml.characters(val);
        xhtml.endElement("td");
      }
      xhtml.endElement("tr");
    }
    xhtml.endElement("table");
    xhtml.endDocument();
  }
}
