/*
Copyright (C) 2013, 2015, 2018, 2021 Bengt Martensson.

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
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static org.harctoolbox.girr.Command.INITIAL_HASHMAP_CAPACITY;
import static org.harctoolbox.girr.XmlStatic.ADMINDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlStatic.GIRR_SCHEMA_LOCATION_URI;
import static org.harctoolbox.girr.XmlStatic.GIRR_VERSION;
import static org.harctoolbox.girr.XmlStatic.GIRR_VERSION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTE_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.SPACE;
import static org.harctoolbox.girr.XmlStatic.TITLE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import static org.harctoolbox.xml.XmlUtils.HTML_NAMESPACE_ATTRIBUTE_NAME;
import static org.harctoolbox.xml.XmlUtils.HTML_NAMESPACE_URI;
import static org.harctoolbox.xml.XmlUtils.SCHEMA_LOCATION_ATTRIBUTE_NAME;
import static org.harctoolbox.xml.XmlUtils.W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class contains a map of Remotes, indexed by their names.
 */
public final class RemoteSet extends XmlExporter implements Iterable<Remote> {

    private final static Logger logger = Logger.getLogger(RemoteSet.class.getName());

    /**
     * For testing only, not deployment.
     * @param args
     */
    public static void main(String[] args) {
        try {
            Command.setIrpDatabase("../IrpTransmogrifier/src/main/resources/IrpProtocols.xml");
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
        if (Files.isRegularFile(path) && !(path.toString().endsWith(".jpg") || path.toString().endsWith(".jpeg"))) {
            try {
                RemoteSet remoteSet = new RemoteSet(path.toFile());
                coll.add(remoteSet);
            }
            catch (GirrException | IOException | SAXException ex) {
                logger.log(Level.WARNING, "Could not read file {0}", path.toString());
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
                logger.log(Level.WARNING, "Could not read directory {0}", path.toString());
            }
        }
        return coll;
    }

    public static RemoteSet parseFileOrDirectory(File file) throws GirrException, IOException, SAXException {
        return file.isFile() ? new RemoteSet(file) : new RemoteSet(file.toPath());
    }

    private AdminData adminData;
    private Map<String, Remote> remotes;
    private IrpDatabase irpDatabase = new IrpDatabase();

    public RemoteSet(Path path) {
        this(System.getProperty("user.name"), path.toString(), parseFiles(path));
    }

    public RemoteSet(String creatingUser, String source, Collection<RemoteSet> remoteSets) {
        this(creatingUser, source, null,
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
     *
     * @param doc W3C Document
     * @throws org.harctoolbox.girr.GirrException
     */
    public RemoteSet(Document doc) throws GirrException {
        this(doc.getDocumentElement());
    }

    /**
     * This constructor is used to import an Element.
     *
     * @param root W3C Element of type remotes
     * @throws org.harctoolbox.girr.GirrException
     */
    public RemoteSet(Element root) throws GirrException {
        if (!root.getTagName().equals(REMOTES_ELEMENT_NAME))
            throw new GirrException("Root element not of type \"" + REMOTES_ELEMENT_NAME + "\".");

        NodeList nl = root.getElementsByTagName(ADMINDATA_ELEMENT_NAME);
        adminData = nl.getLength() > 0 ? new AdminData((Element) nl.item(0)) : new AdminData();
        remotes = new LinkedHashMap<>(INITIAL_HASHMAP_CAPACITY);
        nl = root.getElementsByTagName(REMOTE_ELEMENT_NAME);
        for (int i = 0; i < nl.getLength(); i++) {
            Remote remote = new Remote((Element) nl.item(i));
            remotes.put(remote.getName(), remote);
        }

        nl = root.getElementsByTagName(IrpDatabase.IRP_NAMESPACE_PREFIX + ":" + IrpDatabase.PROTOCOLS_NAME);
        if (nl.getLength() > 0) {
            try {
                Element protocolsElement = (Element) nl.item(0);
                irpDatabase = new IrpDatabase(protocolsElement);
            } catch (IrpParseException ex) {
                logger.log(Level.WARNING, ex.getLocalizedMessage());
            }
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
        this(getElement(file));
    }

    /**
     * This constructor is used to read a Girr file into a RemoteSet.
     * @param file
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public RemoteSet(String file) throws GirrException, IOException, SAXException {
        this(getElement(file));
    }

    /**
     * This constructor is used to read a Reader into a RemoteSet.
     * @param reader
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public RemoteSet(Reader reader) throws IOException, SAXException, GirrException {
        this(getElement(reader));
    }

    /**
     * This constructor sets up a RemoteSet from a given Map of Remotes, so that it can later be used through
     * the xmlExport or xmlExportDocument to generate an XML export.
     * @param creatingUser Comment field for the creating user, if wanted.
     * @param source Comment field describing the origin of the data; e.g. name of human author or creating program.
     * @param creationDate Date of creation, as text string.
     * @param tool Name of creating tool.
     * @param toolVersion Version of creating tool.
     * @param tool2 Name of secondary tool, if applicable.
     * @param tool2Version Version of secondary tool.
     * @param notes Textual notes.
     * @param remotes Map of remotes.
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
        this(new AdminData(creatingUser, source, creationDate, tool, toolVersion, tool2, tool2Version, notes), remotes);
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
        adminData = new AdminData(creatingUser, source, null, tool, toolVersion, tool2, tool2Version, null);
        this.remotes = new LinkedHashMap<>(1);
    }

    RemoteSet(AdminData adminData, Map<String, Remote> remotes) {
        this.adminData = adminData;
        this.remotes = remotes != null ? remotes : new LinkedHashMap<>(1);
    }

    /**
     * This constructor sets up a RemoteSet with no Remotes.
     */
    public RemoteSet() {
        this(new AdminData(), null);
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
        this(new AdminData(creatingUser, source, creationDate, tool, toolVersion, tool2, tool2Version, notes),
                null);
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

    public void sort(Comparator<? super Named> comparator) {
        List<Remote> list = new ArrayList<>(remotes.values());
        Collections.sort(list, comparator);
        remotes.clear();
        list.forEach((Remote remote) -> {
            remote.sort(comparator);
            remotes.put(remote.getName(), remote);
        });
    }

    public boolean isEmpty() {
        return remotes.isEmpty();
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
    @Override
    public Element toElement(Document doc, String title, boolean fatRaw, boolean createSchemaLocation,
            boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, REMOTES_ELEMENT_NAME);
        if (createSchemaLocation) {
            element.setAttribute(HTML_NAMESPACE_ATTRIBUTE_NAME, HTML_NAMESPACE_URI);
            element.setAttribute(W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME, W3C_XML_SCHEMA_INSTANCE_NS_URI);
            element.setAttribute(SCHEMA_LOCATION_ATTRIBUTE_NAME, GIRR_NAMESPACE + SPACE + GIRR_SCHEMA_LOCATION_URI);
        }
        element.setAttribute(GIRR_VERSION_ATTRIBUTE_NAME, GIRR_VERSION);

        if (title != null)
            element.setAttribute(TITLE_ATTRIBUTE_NAME, title);

        Element adminDataEl = adminData.toElement(doc);
        if (adminDataEl.hasChildNodes() || adminDataEl.hasAttributes())
            element.appendChild(adminDataEl);

        for (Remote remote : this)
            element.appendChild(remote.toElement(doc, null, fatRaw, false, generateRaw, generateCcf, generateParameters));

        return element;
    }

    /**
     * Applies the format argument to all Command's in the CommandSet.
     * @param format
     * @param repeatCount
     */
    public void addFormat(Command.CommandTextFormat format, int repeatCount) {
        for (Remote remote : this)
            remote.addFormat(format, repeatCount);
    }

    /**
     * @deprecated
     * Generates a list of the commands in all contained remotes.
     * It may contain non-unique names.
     * Deprecated, while meaningless: You do not want to know all commands of
     * a RemoteSet, you want to know the commands of the individual Remotes.
     * @return ArrayList of the commands.
     */
    public List<Command> getAllCommands() {
        List<Command> allCommands = new ArrayList<>(32);
        for (Remote remote : this)
            allCommands.addAll(remote.getCommands().values());

        return allCommands;
    }

    /**
     * Tries to generate the parameter version of the signal (decoding the signals),
     * unless parameters already are present.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public void checkForParameters() throws IrpException, IrCoreException {
        for (Remote remote : this)
            remote.checkForParameters();
    }

    AdminData getAdminData() {
        return adminData;
    }

    /**
     * @return the creatingUser
     */
    public String getCreatingUser() {
        return adminData.getCreatingUser();
    }

    /**
     * @return the source
     */
    public String getSource() {
        return adminData.getSource();
    }

    /**
     * @return the creationDate
     */
    public String getCreationDate() {
        return adminData.getCreationDate();
    }

    /**
     * @return the tool
     */
    public String getTool() {
        return adminData.getTool();
    }

    /**
     * @return the toolVersion
     */
    public String getToolVersion() {
        return adminData.getToolVersion();
    }

    /**
     * @return the tool2
     */
    public String getTool2() {
        return adminData.getTool2();
    }

    /**
     * @return the tool2Version
     */
    public String getTool2Version() {
        return adminData.getTool2Version();
    }

    /**
     * @param lang
     * @return the notes
     */
    public String getNotes(String lang) {
        return adminData.getNotes(lang);
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

    public Remote getFirstRemote() {
        return iterator().next();
    }

    @Override
    public Iterator<Remote> iterator() {
        return remotes.values().iterator();
    }

    /**
     * Returns the metaData of first Remote. This should not be considered to be the meta data of the RemoteSet.
     * @return metaData of first Remote.
     */
    public Remote.MetaData getFirstMetaData() {
        Remote remote = getFirstRemote();
        return remote != null ? remote.getMetaData() : new Remote.MetaData();
    }

    public IrpDatabase getIrpDatabase() {
        return irpDatabase;
    }
}