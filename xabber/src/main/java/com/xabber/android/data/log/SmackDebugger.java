package com.xabber.android.data.log;

import com.xabber.android.data.SettingsManager;
import com.xabber.android.utils.StringUtils;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.AbstractDebugger;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class SmackDebugger extends AbstractDebugger {
    public static final String LOG_TAG = "Smack";

    public SmackDebugger(XMPPConnection connection, Writer writer, Reader reader) {
        super(connection, writer, reader);
    }

    @Override
    protected void log(String logMessage) {
        LogManager.i(LOG_TAG, logMessage, replaceVcardBase64Hash(replaceMessageBody(getPrettyXmlString(logMessage))));
    }

    @Override
    protected void log(String logMessage, Throwable throwable) {
        LogManager.exception(LOG_TAG, throwable);
    }

    /**
     * Beautify XML string
     */
    private String getPrettyXmlString(String xmlData){
        try {
            int xmlMarkupStartsAtIndex = xmlData.indexOf("<");
            String data = xmlData.substring(xmlMarkupStartsAtIndex-1);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);

            Source xmlInput = new StreamSource(new StringReader(data));
            transformer.transform(xmlInput, xmlOutput);

            String result = xmlOutput.getWriter().toString();
            return xmlData.substring(0, xmlMarkupStartsAtIndex) + "\n" + result.substring(0, result.length()-1);
        } catch (Exception e) {
            if (!((e instanceof TransformerException) || (e instanceof StringIndexOutOfBoundsException))) {
                LogManager.exception(StringUtils.class.getSimpleName(), e);
            }
            return xmlData;
        }
    }

    /**
     * Replace body of message with ***.
     */
    private static String replaceMessageBody(String sourceMsg) {
        if (!SettingsManager.debugLog())
            if (sourceMsg.contains("</message>")) {
                try {
                    int s = sourceMsg.indexOf("<body>");
                    int f = sourceMsg.indexOf("</body>");
                    if (s != -1 && f != -1)
                        return sourceMsg.substring(0, s + 6) + "***" + sourceMsg.substring(f);
                    else return sourceMsg;
                } catch (Exception e) {
                    return sourceMsg;
                }
            } else return sourceMsg;
        else return sourceMsg;
    }

    private static String replaceVcardBase64Hash(String source){
        if (source.contains("</vCard>")) {
            try {
                int s = source.indexOf("<BINVAL>");
                int f = source.indexOf("</BINVAL>");
                if (s != -1 && f != -1)
                    return source.substring(0, s + 6) + "*** base 64 here ***" + source.substring(f);
                else return source;
            } catch (Exception e) {
                return source;
            }
        } else return source;
    }

}
