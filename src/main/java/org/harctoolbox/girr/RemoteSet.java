/*
Copyright (C) 2013, 2015, 2018 Bengt Martensson.

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import javax.xml.validation.Schema;
import static org.harctoolbox.girr.XmlExporter.ADMINDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.CREATINGUSER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.CREATIONDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.CREATIONDATE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlExporter.GIRR_SCHEMA_LOCATION_URI;
import static org.harctoolbox.girr.XmlExporter.GIRR_VERSION;
import static org.harctoolbox.girr.XmlExporter.GIRR_VERSION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.NOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.REMOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.REMOTE_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.SOURCE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.SPACE;
import static org.harctoolbox.girr.XmlExporter.TITLE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.TOOL2VERSION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.TOOL2_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.TOOLVERSIION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.TOOL_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.XmlUtils;
import static org.harctoolbox.ircore.XmlUtils.DEFAULT_CHARSETNAME;
import static org.harctoolbox.ircore.XmlUtils.HTML_NAMESPACE;
import static org.harctoolbox.ircore.XmlUtils.HTML_NAMESPACE_ATTRIBUTE_NAME;
import static org.harctoolbox.ircore.XmlUtils.SCHEMA_LOCATION_ATTRIBUTE_NAME;
import static org.harctoolbox.ircore.XmlUtils.W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME;
import static org.harctoolbox.ircore.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.harctoolbox.irp.IrpParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class models a collection of Remotes, indexed by their names.
 */
public final class RemoteSet {

    private final static Logger logger = Logger.getLogger(RemoteSet.class.getName());

    private static String dateFormatString = "yyyy-MM-dd_HH:mm:ss";

    /**
     * @param aDateFormatString the dateFormatString to set, default "yyyy-MM-dd_HH:mm:ss";
     */
    public static void setDateFormatString(String aDateFormatString) {
        dateFormatString = aDateFormatString;
    }

    /**
     * For testing only, not deployment.
     * @param args
     */
    public static void main(String[] args) {
        try {
            Command.setIrpMaster("../IrpTransmogrifier/src/main/resources/IrpProtocols.xml");
            RemoteSet remoteSet = parseFileOrDirectory(new File(args[0]));
            remoteSet.print(args.length > 1 ? args[1] : "-");
        }
        catch (IOException | GirrException | SAXException | IrpParseException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public static Collection<RemoteSet> parseFiles(Path path) {
        Collection<RemoteSet> coll = new ArrayList<>(10);
        System.out.println(path.toString());
        if (Files.isRegularFile(path)) {
            try {
                RemoteSet remoteSet = new RemoteSet(path.toFile());
                coll.add(remoteSet);
            }
            catch (GirrException | IOException | SAXException ex) {
                logger.log(Level.WARNING, null, ex);
            }
        } else if (Files.isDirectory(path)) {
            try {
                Stream<Path> stream = Files.list(path);
                stream.forEach((f) -> {
                    Collection<RemoteSet> c = parseFiles(f);
                    coll.addAll(c);
                });
            }
            catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return coll;
    }

    public static RemoteSet parseFileOrDirectory(File file) throws GirrException, IOException, SAXException {
        return file.isFile() ? new RemoteSet(file) : new RemoteSet(file.toPath());
    }

    private String creatingUser;
    private String source;
    private String creationDate;
    private String tool;
    private String toolVersion;
    private String tool2;
    private String tool2Version;
    private Map<String, String> notes;
    private Map<String, Remote> remotes;

    public RemoteSet(Path path) {
        this(System.getProperty("user.name"), path.toString(), parseFiles(path));
    }

    public RemoteSet(String creatingUser, String source, Collection<RemoteSet> remoteSets) {
        this(creatingUser, source, (new SimpleDateFormat(dateFormatString)).format(new Date()),
                Version.appName,
                Version.versionString,
                null, null);
        remoteSets.stream().map((remoteSet) -> remoteSet.getRemotes()).forEach((Collection<Remote> coll) -> {
            coll.forEach((Remote remote) -> {
                String originalName = remote.getName();
                String name = originalName;
                int i = 0;
                while (remotes.containsKey(name)) {
                    i++;
                    name = originalName + "_" + Integer.toString(i);
                    remote.setName(name);
                    remote.setComment("Name changed from \"" + originalName + "\" to \"" + name + "\".");
                }
                remotes.put(name, remote);
            });
        });
    }

    /**
     * This constructor is used to import a Document.
     * @param doc W3C Document
     * @throws org.harctoolbox.girr.GirrException
     */
    public RemoteSet(Document doc) throws GirrException {
        remotes = new LinkedHashMap<>(4);

        Element root = doc.getDocumentElement();
        NodeList nl = root.getElementsByTagName(ADMINDATA_ELEMENT_NAME);
        if (nl.getLength() > 0) {
            Element adminData = (Element) nl.item(0);
            notes = XmlExporter.parseElementsByLanguage(adminData.getElementsByTagName(NOTES_ELEMENT_NAME));
            NodeList nodeList = adminData.getElementsByTagName(CREATIONDATA_ELEMENT_NAME);
            if (nodeList.getLength() > 0) {
                Element creationdata = (Element) nodeList.item(0);
                creatingUser = creationdata.getAttribute(CREATINGUSER_ATTRIBUTE_NAME);
                source = creationdata.getAttribute(SOURCE_ATTRIBUTE_NAME);
                creationDate = creationdata.getAttribute(CREATIONDATE_ATTRIBUTE_NAME);
                tool = creationdata.getAttribute(TOOL_ATTRIBUTE_NAME);
                toolVersion = creationdata.getAttribute(TOOLVERSIION_ATTRIBUTE_NAME);
                tool2 = creationdata.getAttribute(TOOL2_ATTRIBUTE_NAME);
                tool2Version = creationdata.getAttribute(TOOL2VERSION_ATTRIBUTE_NAME);
            }
        } else
            notes = new HashMap<>(0);
        nl = root.getElementsByTagName(REMOTE_ELEMENT_NAME);
        for (int i = 0; i < nl.getLength(); i++) {
            Remote remote = new Remote((Element) nl.item(i));
            remotes.put(remote.getName(), remote);
        }
    }

    /**
     * This constructor is used to read a Girr file into a RemoteSet.
     * @param file
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public RemoteSet(File file) throws GirrException, IOException, SAXException {
        this(XmlUtils.openXmlFile(file, (Schema) null, false, false));
    }

    /**
     * This constructor is used to read a Reader into a RemoteSet.
     * @param reader
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public RemoteSet(Reader reader) throws IOException, SAXException, GirrException {
        this(XmlUtils.openXmlReader(reader, null, true, true));
    }

    /**
     * This constructor sets up a RemoteSet from a given Map of Remotes, so that it can later be used through
     * the xmlExport or xmlExportDocument to generate an XML export.
     * @param creatingUser Comment field for the creating user, if wanted.
     * @param source Comment field describing the origin of the data; e.g. name of human author or creating program.
     * @param creationDate Date of creation, as text string.
     * @param tool Name of creating tool.
     * @param toolVersion Version of creating tool.
     * @param tool2 Name of secondary tppl, if applicable.
     * @param tool2Version Version of secondary tool.
     * @param notes Textual notes.
     * @param remotes hMap of remotes.
     */
    public RemoteSet(String creatingUser,
            String source,
            String creationDate,
            String tool,
            String toolVersion,
            String tool2,
            String tool2Version,
            Map<String, String> notes,
            Map<String, Remote> remotes) {
        this(creatingUser, source, creationDate, tool, toolVersion, tool2, tool2Version);
        this.notes = notes;
        this.remotes = remotes;
    }

    /**
     * This constructor sets up a RemoteSet with no Remotes.
     * @param creatingUser Comment field for the creating user, if wanted.
     * @param source Comment field describing the origin of the data; e.g. name of human author or creating program.
     * @param creationDate Date of creation, as text string.
     * @param tool Name of creating tool.
     * @param toolVersion Version of creating tool.
     * @param tool2 Name of secondary tppl, if applicable.
     * @param tool2Version Version of secondary tool.
     */
    public RemoteSet(String creatingUser,
            String source,
            String creationDate,
            String tool,
            String toolVersion,
            String tool2,
            String tool2Version) {
        this.creatingUser = creatingUser;
        this.source = source;
        this.creationDate = creationDate != null ? creationDate : (new SimpleDateFormat(dateFormatString)).format(new Date());
        this.tool = tool;
        this.toolVersion = toolVersion;
        this.tool2 = tool2;
        this.tool2Version = tool2Version;
        this.notes = new HashMap<>(0);
        this.remotes = new LinkedHashMap<>(1);
    }

    /**
     * This constructor sets up a RemoteSet from one single Remote.
     *
     * @param creatingUser
     * @param source
     * @param creationDate
     * @param tool
     * @param toolVersion
     * @param tool2
     * @param tool2Version
     * @param notes
     * @param remote
     */
    public RemoteSet(String creatingUser,
            String source,
            String creationDate,
            String tool,
            String toolVersion,
            String tool2,
            String tool2Version,
            Map<String, String> notes,
            Remote remote) {
        this(creatingUser,
                source,
                creationDate,
                tool,
                toolVersion,
                tool2,
                tool2Version);
        this.notes = notes;
        remotes.put(remote.getName(), remote);
    }

    /**
     * Convenience version of the one-remote constructor.
     * @param creatingUser
     * @param source
     * @param remote
     */
    public RemoteSet(String creatingUser, String source, Remote remote) {
        this(creatingUser,
                source,
                null,
                Version.appName,
                Version.versionString,
                org.harctoolbox.irp.Version.appName,
                org.harctoolbox.irp.Version.version,
                new HashMap<String, String>(0),
                remote);
    }

    /**
     * Convenience version of the many-remote constructor.
     * @param creatingUser
     * @param source
     * @param remotes
     */
    public RemoteSet(String creatingUser, String source, Map<String, Remote> remotes) {
        this(creatingUser,
                source,
                null,
                Version.appName,
                Version.versionString,
                org.harctoolbox.irp.Version.appName,
                org.harctoolbox.irp.Version.version,
                null,
                remotes);
    }

    /**
     * This constructor creates a RemoteSet from a single IrSignal.
     *
     * @param source
     * @param creatingUser
     * @param irSignal
     * @param name
     * @param comment
     * @param deviceName
     */
    public RemoteSet(String source, String creatingUser, IrSignal irSignal, String name,
            String comment, String deviceName) {
        this(creatingUser, source, new Remote(irSignal, name, comment, deviceName));
    }

    /**
     * Copies the Remotes in the RemoteSet as argument, possibly replacing already existing.
     * @param remoteSet
     */
    public void append(RemoteSet remoteSet) {
        remotes.putAll(remoteSet.remotes);
    }

    /**
     * Generates an W3C Element from a RemoteList.
     * @param doc
     * @param title
     * @param fatRaw
     * @param createSchemaLocation
     * @param generateRaw
     * @param generateCcf
     * @param generateParameters
     * @return Element describing the RemoteSet
     */
    public Element toElement(Document doc, String title, boolean fatRaw, boolean createSchemaLocation,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, REMOTES_ELEMENT_NAME);
        if (createSchemaLocation) {
            element.setAttribute(HTML_NAMESPACE_ATTRIBUTE_NAME, HTML_NAMESPACE);
            element.setAttribute(W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME, W3C_XML_SCHEMA_INSTANCE_NS_URI);
            element.setAttribute(SCHEMA_LOCATION_ATTRIBUTE_NAME, GIRR_NAMESPACE + SPACE + GIRR_SCHEMA_LOCATION_URI);
        }
        element.setAttribute(GIRR_VERSION_ATTRIBUTE_NAME, GIRR_VERSION);

        if (title != null)
            element.setAttribute(TITLE_ATTRIBUTE_NAME, title);

        Element adminDataEl = doc.createElementNS(GIRR_NAMESPACE, ADMINDATA_ELEMENT_NAME);
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
        if (creationEl.hasChildNodes())
            adminDataEl.appendChild(creationEl);

        if (notes != null) {
            notes.entrySet().stream().map((note) -> {
                Element notesEl = doc.createElementNS(GIRR_NAMESPACE, NOTES_ELEMENT_NAME);
                notesEl.setAttribute(XML_LANG_ATTRIBUTE_NAME, note.getKey());
                notesEl.setTextContent(note.getValue());
                return notesEl;
            }).forEachOrdered((notesEl) -> {
                adminDataEl.appendChild(notesEl);
            });
        }

        if (adminDataEl.hasChildNodes())
            element.appendChild(adminDataEl);

        remotes.values().forEach((remote) -> {
            element.appendChild(remote.toElement(doc, fatRaw, generateRaw, generateCcf, generateParameters));
        });
        return element;
    }

    /**
     * Generates an XML Document from a RemoteSet.
     * @param title Textual title of document.
     * @param stylesheetType Type of stylesheet, normally "css" or "xsl".
     * @param fatRaw For the raw form, generate elements for each flash and gap, otherwise a long PCDATA text string of durations will be generated.
     * @param stylesheetUrl URL of stylesheet to be linked in a processing instruction.
     * @param createSchemaLocation if schema location attributes (for validation) should be included.
     * @param generateRaw If true, the raw form will be generated.
     * @param generateCcf If true, the CCF ("Pronto hex") form will be generated.
     * @param generateParameters If true, the protocol/parameter description will be generated.
     * @return XmlExporter
     */
    public Document toDocument(String title, String stylesheetType, String stylesheetUrl,
            boolean fatRaw, boolean createSchemaLocation,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element root = toElement(XmlUtils.newDocument(true), title, fatRaw, createSchemaLocation,
            generateRaw, generateCcf, generateParameters);
        return XmlExporter.createDocument(root, stylesheetType, stylesheetUrl, createSchemaLocation);
    }

    public void print(OutputStream ostr) {
        Document doc = toDocument("untitled", "xsl", null/*"xsimplehtml.xsl"*/, false, true, true, true, true);
        XmlUtils.printDOM(ostr, doc, DEFAULT_CHARSETNAME, null);
    }

    public void print(String file) throws IOException {
        if (file.endsWith("-"))
            print(System.out);
        else
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                print(fileOutputStream);
            }
    }

    /**
     * Applies the format argument to all Command's in the CommandSet.
     * @param format
     * @param repeatCount
     */
    public void addFormat(Command.CommandTextFormat format, int repeatCount) {
        remotes.values().forEach((remote) -> {
            remote.addFormat(format, repeatCount);
        });
    }

    /**
     * Generates a list of the commands in all contained remotes.
     * It may contain non-unique names.
     * @return ArrayList of the commands.
     */
    public ArrayList<Command> getAllCommands() {
        ArrayList<Command> allCommands = new ArrayList<>(32);
        remotes.values().forEach((remote) -> {
            allCommands.addAll(remote.getCommands().values());
        });
        return allCommands;
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
     * @param lang
     * @return the notes
     */
    public String getNotes(String lang) {
        return notes.get(lang);
    }

    /**
     * @return Collection of the contained remotes.
     */
    public Collection<Remote> getRemotes() {
        return remotes.values();
    }

    /**
     * Returns a particular remote.
     * @param name
     * @return Remote with the corresponding name, or null if not found.
     */
    public Remote getRemote(String name) {
        return remotes.get(name);
    }
}
