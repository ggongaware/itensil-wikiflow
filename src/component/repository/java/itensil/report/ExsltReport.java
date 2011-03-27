package itensil.report;

import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xml.utils.QName;
import org.apache.xpath.objects.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.TimeZone;
import java.util.Date;

import javax.xml.transform.TransformerException;


import itensil.security.SecurityAssociation;
import itensil.util.Check;
import itensil.util.UriHelper;

import itensil.io.xml.XMLDocument;
import itensil.repository.*;


/**
 *
 * @author  ggongaware@itensil.com
 * @version $Revision: 1.2 $
 *
 * Last updated by $Author: grant $
 */
public class ExsltReport {

    protected static final SimpleDateFormat dateShortFormat = new SimpleDateFormat("M/d/yy");
    protected static final SimpleDateFormat parseFmts[] = new  SimpleDateFormat[]{
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
        new SimpleDateFormat("yyyy-MM-dd")};
    

    
    /*
    static {
        TimeZone zulu = TimeZone.getTimeZone("Europe/Dublin");
        for (int i = 0; i < parseFmts.length; i++) parseFmts[i].setTimeZone(zulu);
    }
    */

    public static XString shortDate(String dateIn)
    {
        Date d = getDate(dateIn);
        if (d == null)  return new XString("");
        SimpleDateFormat fmt = (SimpleDateFormat)dateShortFormat.clone();
        fmt.setTimeZone(SecurityAssociation.getUser().getTimeZone());
        return new XString(fmt.format(d));
    }
  

    protected static Date getDate(String dateStr) {
        for (int i = 0; i < parseFmts.length; i++) {
            Date dVal;
            try {
                dVal = parseFmts[i].parse(dateStr);
                if (dVal != null)
                    return dVal;
            } catch (ParseException e) {
                // eat it
            }
        }
        return null;
    }
}
