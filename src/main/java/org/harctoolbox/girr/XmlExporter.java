/*
Copyright (C) 2021 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.girr;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.xml.XmlUtils;
import static org.harctoolbox.xml.XmlUtils.DEFAULT_CHARSETNAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Abstract base class for Girr classes exporting XML.
 */
public abstract class XmlExporter {

    private final static Logger logger = Logger.getLogger(XmlExporter.class.getName());

    protected static Element getElement(File file) throws IOException, SAXException {
        return XmlUtils.openXmlFile(file).getDocumentElement();
    }

    protected static Element getElement(String file) throws IOException, SAXException {
        return XmlUtils.openXmlThing(file).getDocumentElement();
    }

    protected static Element getElement(Reader reader) throws IOException, SAXException {
        return XmlUtils.openXmlReader(reader, null, true, true).getDocumentElement();
    }

    protected static Element getElement(Document document) {
        return document.getDocumentElement();
    }

    XmlExporter() {
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     * @param ostr
     */
    public final void print(OutputStream ostr) {
        Document doc = toDocument("untitled", null, null, false, true, true, true, true);
        try {
            XmlUtils.printDOM(ostr, doc, DEFAULT_CHARSETNAME, null);
        } catch (UnsupportedEncodingException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     *
     * @param file
     * @throws java.io.IOException
     */
    public final void print(String file) throws IOException {
        print(IrCoreUtils.getPrintStream(file, DEFAULT_CHARSETNAME));
    }

    /**
     * Generates an XML Document from a RemoteSet.
     *
     * @param title Textual title of document.
     * @param stylesheetType Type of stylesheet, normally "css" or "xsl".
     * @param fatRaw For the raw form, generate elements for each flash and gap,
     * otherwise a long PCDATA text string of durations will be generated.
     * @param stylesheetUrl URL of stylesheet to be linked in a processing
     * instruction.
     * @param createSchemaLocation if schema location attributes (for
     * validation) should be included.
     * @param generateRaw If true, the raw form will be generated.
     * @param generateCcf If true, the CCF ("Pronto hex") form will be
     * generated.
     * @param generateParameters If true, the protocol/parameter description
     * will be generated.
     * @return XmlExporter
     */
    public final Document toDocument(String title, String stylesheetType, String stylesheetUrl,
            boolean fatRaw, boolean createSchemaLocation,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element root = toElement(XmlUtils.newDocument(true), title, fatRaw, createSchemaLocation,
                generateRaw, generateCcf, generateParameters);
        return XmlStatic.createDocument(root, stylesheetType, stylesheetUrl, createSchemaLocation);
    }

    abstract Element toElement(Document doc, String title, boolean fatRaw, boolean createSchemaLocation,
            boolean generateRaw, boolean generateCcf, boolean generateParameters);
}
