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

import java.util.HashMap;
import java.util.Map;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static org.harctoolbox.xml.XmlUtils.ENGLISH;
import static org.harctoolbox.xml.XmlUtils.HTML_NAMESPACE_ATTRIBUTE_NAME;
import static org.harctoolbox.xml.XmlUtils.HTML_NAMESPACE_URI;
import static org.harctoolbox.xml.XmlUtils.SCHEMA_LOCATION_ATTRIBUTE_NAME;
import static org.harctoolbox.xml.XmlUtils.W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME;
import static org.harctoolbox.xml.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * Static constants and helper functions for XML export. Usage in other contexts not recommended.
 */
public abstract class XmlStatic {

    /**
     * String of the form major.minor identifying the protocol version
     * (not to be confused with the version of an implementation).
     * Should be the same as the attribute girrVersion in girr_ns.xsd.
     */
    public static final String GIRR_VERSION         = "1.2";

    /**
     * Namespace URI
     */
    public static final String GIRR_NAMESPACE       = "http://www.harctoolbox.org/Girr";

    /**
     * Homepage URL.
     */
    public static final String GIRR_HOMEPAGE        = "http://www.harctoolbox.org/Girr.html";

    /**
     * URL for schema file supporting name spaces.
     */
    public static final String GIRR_SCHEMA_LOCATION_URI = "http://www.harctoolbox.org/schemas/girr_ns-"  + GIRR_VERSION + ".xsd";

    /**
     * URL for schema file, namespace-less version.
     */
    public static final String GIRR_NONAMESPACE_SCHEMA_LOCATION_URI = "http://www.harctoolbox.org/schemas/girr.xsd";

    /**
     * Comment string pointing to Girr docu.
     */
    public static final String GIRR_COMMENT                = "This file is in the Girr (General IR Remote) format, see " + GIRR_HOMEPAGE;

      // Attribute names in Girr file.
    public static final String GIRR_VERSION_ATTRIBUTE_NAME = "girrVersion";
    public static final String TOGGLE_ATTRIBUTE_NAME       = "T";
    public static final String F_ATTRIBUTE_NAME            = "F";
    public static final String VALUE_ATTRIBUTE_NAME        = "value";
    public static final String NAME_ATTRIBUTE_NAME         = "name";
    public static final String COMMENT_ATTRIBUTE_NAME      = "comment";
    public static final String MASTER_ATTRIBUTE_NAME       = "master";
    public static final String FREQUENCY_ATTRIBUTE_NAME    = "frequency";
    public static final String DUTYCYCLE_ATTRIBUTE_NAME    = "dutyCycle";
    public static final String TITLE_ATTRIBUTE_NAME        = "title";
    public static final String PROTOCOL_ATTRIBUTE_NAME     = "protocol";
    public static final String DISPLAYNAME_ATTRIBUTE_NAME  = "displayName";
    public static final String MANUFACTURER_ATTRIBUTE_NAME = "manufacturer";
    public static final String APPLICATION_ATTRIBUTE_NAME  = "application";
    public static final String MODEL_ATTRIBUTE_NAME        = "model";
    public static final String DEVICECLASS_ATTRIBUTE_NAME  = "deviceClass";
    public static final String REMOTENAME_ATTRIBUTE_NAME   = "remoteName";
    public static final String CREATINGUSER_ATTRIBUTE_NAME = "creatingUser";
    public static final String SOURCE_ATTRIBUTE_NAME       = "source";
    public static final String CREATIONDATE_ATTRIBUTE_NAME = "creationDate";
    public static final String TOOL_ATTRIBUTE_NAME         = "tool";
    public static final String TOOLVERSIION_ATTRIBUTE_NAME = "toolVersion";
    public static final String TOOL2_ATTRIBUTE_NAME        = "tool2";
    public static final String TOOL2VERSION_ATTRIBUTE_NAME = "tool2Version";

    // Element names in Girr files.
    public static final String PRONTO_HEX_ELEMENT_NAME     = "ccf";
    public static final String FLASH_ELEMENT_NAME          = "flash";
    public static final String GAP_ELEMENT_NAME            = "gap";
    public static final String PARAMETER_ELEMENT_NAME      = "parameter";
    public static final String NOTES_ELEMENT_NAME          = "notes";
    public static final String PARAMETERS_ELEMENT_NAME     = "parameters";
    public static final String PROTOCOL_ELEMENT_NAME       = "protocol";
    public static final String RAW_ELEMENT_NAME            = "raw";
    public static final String INTRO_ELEMENT_NAME          = "intro";
    public static final String REPEAT_ELEMENT_NAME         = "repeat";
    public static final String ENDING_ELEMENT_NAME         = "ending";
    public static final String FORMAT_ELEMENT_NAME         = "format";
    public static final String COMMAND_ELEMENT_NAME        = "command";
    public static final String COMMANDSET_ELEMENT_NAME     = "commandSet";
    public static final String APPLICATIONDATA_ELEMENT_NAME = "applicationData" ;
    public static final String APPPARAMETER_ELEMENT_NAME   = "appParameter";
    public static final String REMOTE_ELEMENT_NAME         = "remote";
    public static final String ADMINDATA_ELEMENT_NAME      = "adminData";
    public static final String CREATIONDATA_ELEMENT_NAME   = "creationData";
    public static final String REMOTES_ELEMENT_NAME        = "remotes";

    public static final String SPACE                       = " ";
    public static final String EQUALS                      = "=";

    /**
     * Makes a Document from an Element.
     *
     * @param title Text for title attribute
     * @param root Element to transfer
     * @param stylesheetType
     * @param stylesheetUrl
     * @return
     */
    static Document createDocument(String title, Element root, String stylesheetType, String stylesheetUrl) {
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
        root.setAttribute(W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME, W3C_XML_SCHEMA_INSTANCE_NS_URI);
        root.setAttribute(XMLNS_ATTRIBUTE, GIRR_NAMESPACE);
        root.setAttribute(HTML_NAMESPACE_ATTRIBUTE_NAME, HTML_NAMESPACE_URI);
        root.setAttribute(SCHEMA_LOCATION_ATTRIBUTE_NAME, GIRR_NAMESPACE + " " + GIRR_SCHEMA_LOCATION_URI);
        if (title != null && ! title.isEmpty())
            root.setAttribute(TITLE_ATTRIBUTE_NAME, title);
        return document;
    }

    static Map<String, String> parseElementsByLanguage(NodeList nodeList) {
        Map<String, String> map = new HashMap<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element note = (Element) nodeList.item(i);
            String lang = note.getAttribute(XML_LANG_ATTRIBUTE_NAME);
            if (lang.isEmpty())
                lang = ENGLISH;
            map.put(lang, note.getTextContent().trim());
        }
        return map;
    }

    private XmlStatic() {
    }
}
