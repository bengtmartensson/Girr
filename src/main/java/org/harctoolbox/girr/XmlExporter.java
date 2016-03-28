/*
Copyright (C) 2013 Bengt Martensson.

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * Utility class for XML export. Usage in other contexts not recommended.
 */
public class XmlExporter {
    /*public static final String flashTagName = "flash";
    public static final String gapTagName = "gap";
    public static final String decodeTagName = "decode";
    public static final String decodesTagName = "decodes";
    public static final String protocolAttributeName = "protocol";
    public static final String textTagName = "text";
    public static final String prontoTagName = "pronto";
    public static final String introTagName = "intro";
    public static final String repeatTagName = "repeat";
    public static final String endingTagName = "ending";
    public static final String parametersTagName = "parameters";
    public static final String parameterTagName = "parameter";
    public static final String parameterNameAttributeName = "name";
    public static final String parameterValueAttributeName = "value";
    public static final String irSignalTagName = "irsignal";
    public static final String nameAttributeName = "name";
    public static final String frequencyAttributeName = "frequency";
    public static final String dutyCycleAttributeName = "dutycycle";
    public static final String rawTagName = "raw";
    public static final String analyzerTagName = "analyzer";
    public static final String introBurstsLengthName = "nointrobursts";
    public static final String repeatBurstsLengthName = "norepeatbursts";
    public static final String endingBurstsLengthName = "noendingbursts";
    public static final String burstLengthAttributeName = "burstlength";
    public static final String commentAttributeName = "comment";*/

    /**
     * Name space for the XML Schemas
     */
    private static final String w3cSchemaNamespace = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * URL for schema file.
     */
    public static final String schemaLocation = "http://www.harctoolbox.org/Girr http://www.harctoolbox.org/schemas/girr_ns.xsd";

    /**
     * URL for schema file, namespace-less version.
     */
    public static final String noNamespaceSchemaLocation = "http://www.harctoolbox.org/schemas/girr.xsd";

    /**
     * Namespace URI
     */
    public static final String girrNamespace = "http://www.harctoolbox.org/Girr";

    /**
     * URL for schema file supporting name spaces.
     */
    public static final String girrSchemaLocation = "http://www.harctoolbox.org/schemas/girr_ns.xsd";

    public static final boolean useNamespaces = true;

    /**
     * Comment string pointing to Girr docu.
     */
    private static final String girrComment = "This file is in the Girr (General IR Remote) format, see http://www.harctoolbox.org/Girr.html";

    private static final String defaultCharsetName = "UTF-8";

    private final Document document;

    /**
     *
     * @param doc
     */
    public XmlExporter(Document doc) {
        this.document = doc;
    }

    public XmlExporter(Element root, String stylesheetType, String stylesheetUrl, boolean createSchemaLocation) {
        this(createDocument(root, stylesheetType, stylesheetUrl, createSchemaLocation));
    }

    public static Document createDocument(Element root, String stylesheetType, String stylesheetUrl, boolean createSchemaLocation) {
        Document document = root.getOwnerDocument();

        if (stylesheetType != null && stylesheetUrl != null && ! stylesheetUrl.isEmpty()) {
            ProcessingInstruction pi = document.createProcessingInstruction("xml-stylesheet",
                    "type=\"text/" + stylesheetType + "\" href=\"" + stylesheetUrl + "\"");
            document.appendChild(pi);
        }

        // At least in some Java versions (https://bugs.openjdk.java.net/browse/JDK-7150637)
        // there is no line feed before and after the comment.
        // This is technically correct, but looks awful to the human reader.
        // AFAIK, there is no clean way to fix this.
        // Possibly works with some Java versions?
        Comment comment = document.createComment(girrComment);
        document.appendChild(comment);
        document.appendChild(root);
        root.setAttribute("girrVersion", RemoteSet.girrVersion);
        if (createSchemaLocation) {
            root.setAttribute("xmlns:xsi", XmlExporter.w3cSchemaNamespace);
            root.setAttribute("xmlns", girrNamespace);
            //root.setAttribute("xsi:noNamespaceSchemaLocation", XmlExporter.noNamespaceSchemaLocation);
            root.setAttribute("xsi:schemaLocation", schemaLocation);
        }
        return document;
    }

    public static Document newDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false); // FIXME, but carefully...
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException e) {
        }
        return doc;
    }

    public void printDOM(OutputStream ostr, Document stylesheet, HashMap<String, String>parameters,
            boolean binary, String charsetName) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer tr;
            if (stylesheet == null) {
                tr = factory.newTransformer();

                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, charsetName);

            } else {
                if (parameters != null)
                    for (Map.Entry<String, String> kvp : parameters.entrySet()) {
                        Element e = stylesheet.createElementNS("http://www.w3.org/1999/XSL/Transform", "param");
                        e.setAttribute("name", kvp.getKey());
                        e.setAttribute("select", kvp.getValue());
                        stylesheet.getDocumentElement().insertBefore(e, stylesheet.getDocumentElement().getFirstChild());
                    }
                NodeList nodeList = stylesheet.getDocumentElement().getElementsByTagName("xsl:output");
                if (nodeList.getLength() > 0) {
                    Element e = (Element) nodeList.item(0);
                    e.setAttribute("encoding", charsetName);
                }
                //XmlUtils.printDOM(System.out, stylesheet, null, "UTF-8");
                tr = factory.newTransformer(new DOMSource(stylesheet));
            }
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            if (binary) {
                DOMResult domResult = new DOMResult();
                tr.transform(new DOMSource(document), domResult);
                Document newDoc = (Document) domResult.getNode();
                NodeList byteElements = newDoc.getDocumentElement().getElementsByTagName("byte");
                for (int i = 0; i < byteElements.getLength(); i++) {
                    int val = Integer.parseInt(((Element) byteElements.item(i)).getTextContent());
                    ostr.write(val);
                }
            } else
                tr.transform(new DOMSource(document), new StreamResult(ostr));
            if (parameters != null && stylesheet != null) {
                NodeList nl = stylesheet.getDocumentElement().getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    Node n = nl.item(i);
                    if (n.getNodeType() != Node.ELEMENT_NODE)
                        continue;
                    Element e = (Element) n;
                    if (e.getLocalName().equals("param") && parameters.containsKey(e.getAttribute("name")))
                        stylesheet.getDocumentElement().removeChild(n);
                }
            }
        } catch (TransformerConfigurationException e) {
            System.err.println(e.getMessage());
        } catch (TransformerException e) {
            System.err.println(e.getMessage());
        }
    }

    public void printDOM(OutputStream ostr, String charsetName) throws IOException {
        printDOM(ostr, null, null, false, charsetName);
    }

    public void printDOM(File file, String charsetName) throws IOException  {
        if (file == null)
            printDOM(System.out, charsetName);
        else {
            try (FileOutputStream stream = new FileOutputStream(file)) {
                printDOM(stream, charsetName);
            }
        }
    }

    public void printDOM(File file) throws FileNotFoundException, IOException {
        printDOM(file, defaultCharsetName);
    }
}
