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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static org.harctoolbox.girr.Command.INITIAL_HASHMAP_CAPACITY;
import static org.harctoolbox.girr.XmlStatic.ADMINDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.CREATINGUSER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.CREATIONDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.CREATIONDATE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlStatic.NOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.SOURCE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.TOOL2VERSION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.TOOL2_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.TOOLVERSIION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.TOOL_ATTRIBUTE_NAME;
import static org.harctoolbox.xml.XmlUtils.ENGLISH;
import static org.harctoolbox.xml.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class combines a number of administrative data.
 */
final class AdminData implements Serializable {
    public static final String dateFormatString = "yyyy-MM-dd_HH:mm:ss";

    private final String creatingUser;
    private       String source;
    private final String creationDate;
    private final String tool;
    private final String toolVersion;
    private final String tool2;
    private final String tool2Version;
    private final Map<String, String> notes;

    AdminData() {
        this(null, null, null, null, null, null, null, null);
    }

    AdminData(String creatingUser, String source, String creationDate, String tool, String toolVersion, String tool2, String tool2Version, Map<String, String> notes) {
        this.creatingUser = creatingUser != null ? creatingUser : System.getProperty("user.name");
        this.source = source;
        this.creationDate = creationDate != null ? creationDate : (new SimpleDateFormat(dateFormatString)).format(new Date());
        this.tool = tool;
        this.toolVersion = toolVersion;
        this.tool2 = tool2;
        this.tool2Version = tool2Version;
        this.notes = notes != null ? notes : new HashMap<>(INITIAL_HASHMAP_CAPACITY);
    }

    AdminData(Element element) throws GirrException {
        if (!element.getTagName().equals(ADMINDATA_ELEMENT_NAME))
            throw new GirrException("Element not " + ADMINDATA_ELEMENT_NAME);

        notes = XmlStatic.parseElementsByLanguage(element.getElementsByTagName(NOTES_ELEMENT_NAME));
        NodeList nodeList = element.getElementsByTagName(CREATIONDATA_ELEMENT_NAME);
        if (nodeList.getLength() > 0) {
            Element creationdata = (Element) nodeList.item(0);
            creatingUser = creationdata.getAttribute(CREATINGUSER_ATTRIBUTE_NAME);
            source = creationdata.getAttribute(SOURCE_ATTRIBUTE_NAME);
            creationDate = creationdata.getAttribute(CREATIONDATE_ATTRIBUTE_NAME);
            tool = creationdata.getAttribute(TOOL_ATTRIBUTE_NAME);
            toolVersion = creationdata.getAttribute(TOOLVERSIION_ATTRIBUTE_NAME);
            tool2 = creationdata.getAttribute(TOOL2_ATTRIBUTE_NAME);
            tool2Version = creationdata.getAttribute(TOOL2VERSION_ATTRIBUTE_NAME);
        } else {
            creatingUser = null;
            source = null;
            creationDate = (new SimpleDateFormat(dateFormatString)).format(new Date());
            tool = null;
            toolVersion = null;
            tool2 = null;
            tool2Version = null;
        }
    }

    Element toElement(Document doc) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, ADMINDATA_ELEMENT_NAME);
        Element creationEl = doc.createElementNS(GIRR_NAMESPACE, CREATIONDATA_ELEMENT_NAME);

        if (creatingUser != null)
            creationEl.setAttribute(CREATINGUSER_ATTRIBUTE_NAME, creatingUser);
        if (source != null)
            creationEl.setAttribute(SOURCE_ATTRIBUTE_NAME, source);
        if (creationDate != null)
            creationEl.setAttribute(CREATIONDATE_ATTRIBUTE_NAME, creationDate);
        if (tool != null)
            creationEl.setAttribute(TOOL_ATTRIBUTE_NAME, tool);
        if (toolVersion != null)
            creationEl.setAttribute(TOOLVERSIION_ATTRIBUTE_NAME, toolVersion);
        if (tool2 != null)
            creationEl.setAttribute(TOOL2_ATTRIBUTE_NAME, tool2);
        if (tool2Version != null)
            creationEl.setAttribute(TOOL2VERSION_ATTRIBUTE_NAME, tool2Version);
        if (creationEl.hasChildNodes() || creationEl.hasAttributes())
            element.appendChild(creationEl);

        if (notes != null) {
            notes.entrySet().stream().map((note) -> {
                Element notesEl = doc.createElementNS(GIRR_NAMESPACE, NOTES_ELEMENT_NAME);
                notesEl.setAttribute(XML_LANG_ATTRIBUTE_NAME, note.getKey());
                notesEl.setTextContent(note.getValue());
                return notesEl;
            }).forEachOrdered((notesEl) -> {
                element.appendChild(notesEl);
            });
        }
        return element;
    }

    String getNotes() {
        return getNotes(ENGLISH);
    }

    String getNotes(String language) {
        return notes.get(language);
    }

    /**
     * @return the creatingUser
     */
    public String getCreatingUser() {
        return creatingUser;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    public void setSourceIfEmpty(String source) {
        if (this.source == null || this.source.isEmpty())
            this.source = source;
    }

    /**
     * @return the creationDate
     */
    public String getCreationDate() {
        return creationDate;
    }

    /**
     * @return the tool
     */
    public String getTool() {
        return tool;
    }

    /**
     * @return the toolVersion
     */
    public String getToolVersion() {
        return toolVersion;
    }

    /**
     * @return the tool2
     */
    public String getTool2() {
        return tool2;
    }

    /**
     * @return the tool2Version
     */
    public String getTool2Version() {
        return tool2Version;
    }

    /**
     * @return the notes
     */
    public Map<String, String> getAllNotes() {
        return Collections.unmodifiableMap(notes);
    }
}
