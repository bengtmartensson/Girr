/*
Copyright (C) 2013, 2018 Bengt Martensson.

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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static org.harctoolbox.irp.XmlUtils.ENGLISH;
import static org.harctoolbox.irp.XmlUtils.SCHEMA_LOCATION_ATTRIBUTE_NAME;
import static org.harctoolbox.irp.XmlUtils.W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME;
import static org.harctoolbox.irp.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * Utility class for XML export. Usage in other contexts not recommended.
 */
public final class XmlExporter {

    private final static Logger logger = Logger.getLogger(XmlExporter.class.getName());

    /**
     * Namespace URI
     */
    static final String GIRR_NAMESPACE = "http://www.harctoolbox.org/Girr";

    /**
     * Homepage URL.
     */
    public static final String GIRR_HOMEPAGE = "http://www.harctoolbox.org/Girr.html";

    /**
     * URL for schema file supporting name spaces.
     */
    public static final String GIRR_SCHEMA_LOCATION_URI = "http://www.harctoolbox.org/schemas/girr_ns.xsd";

    /**
     * URL for schema file, namespace-less version.
     */
    public static final String GIRR_NONAMESPACE_SCHEMA_LOCATION_URI = "http://www.harctoolbox.org/schemas/girr.xsd";

    /**
     * Comment string pointing to Girr docu.
     */
    static final String GIRR_COMMENT = "This file is in the Girr (General IR Remote) format, see http://www.harctoolbox.org/Girr.html";

    /**
     * String of the form major.minor identifying the protocol version
     * (not to be confused with the version of an implementation).
     */
    public static final String GIRR_VERSION         = "1.0";

      // Attribute names in Girr file.
    static final String GIRR_VERSION_ATTRIBUTE_NAME = "girrVersion";
    static final String TOGGLE_ATTRIBUTE_NAME       = "T";
    static final String F_ATTRIBUTE_NAME            = "F";
    static final String VALUE_ATTRIBUTE_NAME        = "value";
    static final String NAME_ATTRIBUTE_NAME         = "name";
    static final String COMMENT_ATTRIBUTE_NAME      = "comment";
    static final String MASTER_ATTRIBUTE_NAME       = "master";
    static final String FREQUENCY_ATTRIBUTE_NAME    = "frequency";
    static final String DUTYCYCLE_ATTRIBUTE_NAME    = "dutyCycle";
    static final String TITLE_ATTRIBUTE_NAME        = "title";
    static final String PROTOCOL_ATTRIBUTE_NAME     = "protocol";
    static final String DISPLAYNAME_ATTRIBUTE_NAME  = "displayName";
    static final String MANUFACTURER_ATTRIBUTE_NAME = "manufacturer";
    static final String APPLICATION_ATTRIBUTE_NAME  = "application";
    static final String MODEL_ATTRIBUTE_NAME        = "model";
    static final String DEVICECLASS_ATTRIBUTE_NAME  = "deviceClass";
    static final String REMOTENAME_ATTRIBUTE_NAME   = "remoteName";
    static final String CREATINGUSER_ATTRIBUTE_NAME = "creatingUser";
    static final String SOURCE_ATTRIBUTE_NAME       = "source";
    static final String CREATIONDATE_ATTRIBUTE_NAME = "creationDate";
    static final String TOOL_ATTRIBUTE_NAME         = "tool";
    static final String TOOLVERSIION_ATTRIBUTE_NAME = "toolVersion";
    static final String TOOL2_ATTRIBUTE_NAME        = "tool2";
    static final String TOOL2VERSION_ATTRIBUTE_NAME = "tool2Version";

    // Element names in Girr files.
    static final String PRONTO_HEX_ELEMENT_NAME     = "ccf";
    static final String FLASH_ELEMENT_NAME          = "flash";
    static final String GAP_ELEMENT_NAME            = "gap";
    static final String PARAMETER_ELEMENT_NAME      = "parameter";
    static final String NOTES_ELEMENT_NAME          = "notes";
    static final String PARAMETERS_ELEMENT_NAME     = "parameters";
    static final String PROTOCOL_ELEMENT_NAME       = "protocol";
    static final String RAW_ELEMENT_NAME            = "raw";
    static final String INTRO_ELEMENT_NAME          = "intro";
    static final String REPEAT_ELEMENT_NAME         = "repeat";
    static final String ENDING_ELEMENT_NAME         = "ending";
    static final String FORMAT_ELEMENT_NAME         = "format";
    static final String COMMAND_ELEMENT_NAME        = "command";
    static final String COMMANDSET_ELEMENT_NAME     = "commandSet";
    static final String APPLICATIONDATA_ELEMENT_NAME = "applicationData" ;
    static final String APPPARAMETER_ELEMENT_NAME   = "appParameter";
    static final String REMOTE_ELEMENT_NAME         = "remote";
    static final String ADMINDATA_ELEMENT_NAME      = "adminData";
    static final String CREATIONDATA_ELEMENT_NAME   = "creationData";
    static final String REMOTES_ELEMENT_NAME        = "remotes";

    static final String SPACE = " ";

    /**
     * Makes a Document from an Element.
     * @param root
     * @param stylesheetType
     * @param stylesheetUrl
     * @param createSchemaLocation
     * @return
     */
    static Document createDocument(Element root, String stylesheetType, String stylesheetUrl, boolean createSchemaLocation) {
        Document document = root.getOwnerDocument();

        if (stylesheetType != null && stylesheetUrl != null && !stylesheetUrl.isEmpty()) {
            ProcessingInstruction pi = document.createProcessingInstruction("xml-stylesheet",
                    "type=\"text/" + stylesheetType + "\" href=\"" + stylesheetUrl + "\"");
            document.appendChild(pi);
        }

        // At least in some Java versions (https://bugs.openjdk.java.net/browse/JDK-7150637)
        // there is no line feed before and after the comment.
        // This is technically correct, but looks awful to the human reader.
        // AFAIK, there is no clean way to fix this.
        // Possibly works with some Java versions?
        Comment comment = document.createComment(GIRR_COMMENT);
        document.appendChild(comment);
        document.appendChild(root);
        root.setAttribute(GIRR_VERSION_ATTRIBUTE_NAME, GIRR_VERSION);
        if (createSchemaLocation) {
            root.setAttribute(W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME, W3C_XML_SCHEMA_INSTANCE_NS_URI);
            root.setAttribute(XMLNS_ATTRIBUTE, GIRR_NAMESPACE);
            root.setAttribute(SCHEMA_LOCATION_ATTRIBUTE_NAME, GIRR_NAMESPACE + " " + GIRR_SCHEMA_LOCATION_URI);
        }
        return document;
    }

    static Map<String, String> parseElementsByLanguage(NodeList nodeList) {
        Map<String, String> map = new HashMap<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element note = (Element) nodeList.item(i);
            String lang = note.getAttribute(XML_LANG_ATTRIBUTE_NAME);
            if (lang.isEmpty())
                lang = ENGLISH;
            map.put(lang, note.getTextContent());
        }
        return map;
    }

    private XmlExporter() {
    }
}
