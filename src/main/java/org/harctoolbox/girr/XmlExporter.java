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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
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
public abstract class XmlExporter implements Serializable {

    private final static Logger logger = Logger.getLogger(XmlExporter.class.getName());

    /**
     * Returns the root element of the first argument, which is supposed to be a valid XML document.
     * @param file
     * @return
     * @throws IOException
     * @throws SAXException
     */
    protected static Element getElement(File file) throws IOException, SAXException {
        return XmlUtils.openXmlFile(file).getDocumentElement();
    }

    /**
     * Returns the root element of the first argument, which is supposed to be a valid XML document.
     * @param file
     * @return
     * @throws IOException
     * @throws SAXException
     */
    protected static Element getElement(String file) throws IOException, SAXException {
        return XmlUtils.openXmlThing(file).getDocumentElement();
    }

    /**
     * Returns the root element of the first argument, which is supposed to read to a valid XML document.
     * @param reader
     * @return
     * @throws IOException
     * @throws SAXException
     */
    protected static Element getElement(Reader reader) throws IOException, SAXException {
        return XmlUtils.openXmlReader(reader, null, true, true).getDocumentElement();
    }

    /**
     * Returns the root element of the first argument, which is supposed to be a valid XML Document.
     * @param document
     * @return
     */
    protected static Element getElement(Document document) {
        return document.getDocumentElement();
    }

    protected XmlExporter() {
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     * @param ostr
     * @param generateProtocol
     * @param generateProntoHex
     * @param generateRaw
     */
    public final void print(OutputStream ostr, boolean generateProtocol, boolean generateProntoHex, boolean generateRaw) {
        Document doc = toDocument(null, null, null, false, generateProtocol, generateProntoHex, generateRaw);
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
     * @param generateProtocol
     * @param generateProntoHex
     * @param generateRaw
     * @throws java.io.IOException
     */
    public final void print(String file, boolean generateProtocol, boolean generateProntoHex, boolean generateRaw) throws IOException {
        print(IrCoreUtils.getPrintStream(file, DEFAULT_CHARSETNAME), generateProtocol, generateProntoHex, generateRaw);
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     *
     * @param file
     * @throws java.io.IOException
     */
    public final void print(String file) throws IOException {
        print(file, true, true, true);
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     *
     * @param file
     * @param generateProtocol
     * @param generateProntoHex
     * @param generateRaw
     * @throws java.io.IOException
     */
    public final void print(File file, boolean generateProtocol, boolean generateProntoHex, boolean generateRaw) throws IOException {
        print(new FileOutputStream(file), generateProtocol, generateProntoHex, generateRaw);
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     *
     * @param file
     * @throws java.io.IOException
     */
    public final void print(File file) throws IOException {
        print(file, true, true, true);
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
     * @param generateParameters If true, the protocol/parameter description
     * will be generated.
     * @param generateProntoHex If true, the Pronto hex form will be
     * generated.
     * @param generateRaw If true, the raw form will be generated.
     * @return W3C Document
     */
    public final Document toDocument(String title, String stylesheetType, String stylesheetUrl,
            boolean fatRaw,
            boolean generateParameters, boolean generateProntoHex, boolean generateRaw)  {
        Element root = toElement(XmlUtils.newDocument(true), fatRaw,
                generateParameters, generateProntoHex, generateRaw);
        return XmlStatic.createDocument(title, root, stylesheetType, stylesheetUrl);
    }

    /**
     * @deprecated createSchemaLocation has been effectively removed, use version without it instead.
     *
     * @param title
     * @param stylesheetType
     * @param stylesheetUrl
     * @param fatRaw
     * @param createSchemaLocation
     * @param generateParameters
     * @param generateProntoHex
     * @param generateRaw
     * @return
     */
    public final Document toDocument(String title, String stylesheetType, String stylesheetUrl,
            boolean fatRaw, boolean createSchemaLocation,
            boolean generateParameters, boolean generateProntoHex, boolean generateRaw) {
        return toDocument(title, stylesheetType, stylesheetUrl,
                fatRaw, generateParameters, generateProntoHex, generateRaw);
    }

    /**
     * Exports the Object to an Element.
     *
     * @param doc Owner Document.
     * @param fatRaw If generating the raw form, generate it in the so-called fat form, with one element per duration.
     * @param generateParameters If true, generate the parameter form.
     * @param generateProntoHex If true, generate the Pronto Hex form.
     * @param generateRaw If true, generate the raw form.
     * @return newly constructed element, belonging to the doc Document.
     */
    abstract Element toElement(Document doc, boolean fatRaw,
            boolean generateParameters, boolean generateProntoHex, boolean generateRaw);
}
