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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
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
import static org.harctoolbox.girr.Command.INITIAL_HASHMAP_CAPACITY;
import static org.harctoolbox.girr.XmlStatic.ADMINDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.COMMANDSET_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.COMMAND_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlStatic.REMOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTE_ELEMENT_NAME;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class contains a map of Remotes, indexed by their names.
 *
 * Earlier versions of this library could only import and export RemoteSets.
 */
public final class RemoteSet extends XmlExporter implements Iterable<Remote> {

    private static final Logger logger = Logger.getLogger(RemoteSet.class.getName());
    private static final int INITIAL_LIST_CAPACITY = 8;

    /**
     * For testing only, not deployment.
     * @param args
     */
    public static void main(String[] args) {
        try {
            Command.setIrpDatabase(new IrpDatabase());
            RemoteSet remoteSet = parse(new File(args[0]));
            remoteSet.print(args.length > 1 ? args[1] : "-");
        }
        catch (IOException | GirrException | SAXException | IrpParseException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Give a file or directory, parses the contained file(s) into a
     * Collection of RemoteSets.
     * Can handle XML documents with root element to type remotes, remote, commandSet and command.
     *
     * @param file
     * @return
     */
    public static Collection<RemoteSet> parseAsCollection(File file) {
        Collection<RemoteSet> coll = new ArrayList<>(INITIAL_LIST_CAPACITY);
        if (file.isFile() && !ignoreByExtension(file.getName())) {
            try {
                RemoteSet remoteSet = parse(getElement(file), file.toString());
                coll.add(remoteSet);
            } catch (GirrException | IOException | SAXException ex) {
                logger.log(Level.WARNING, "Could not read file {0}", file.toString());
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                logger.log(Level.WARNING, "Could not read directory {0}", file.toString());
                return null;
            }
            for (File f : files) {
                Collection<RemoteSet> c = parseAsCollection(f);
                if (c != null)
                    coll.addAll(c);
            }
        }
        return coll;
    }

    /**
     * Give a file or directory, parses the contained file(s) into a RemoteSet.
     * Can handle XML documents with root element to type remotes, remote, commandSet and command.
     *
     * @param file
     * @return
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public static RemoteSet parse(File file) throws GirrException, IOException, SAXException {
        Collection<RemoteSet> collection = parseAsCollection(file);
        return new RemoteSet(null, file.toString(), collection);
    }

    /**
     * Give a file or directory, parses the contained file(s) into a RemoteSet.
     * Can handle XML documents with root element to type remotes, remote, commandSet and command.
     *
     * @param file
     * @return
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     * @throws org.harctoolbox.girr.GirrException
     */
    public static RemoteSet parse(String file) throws IOException, SAXException, GirrException {
        return parse(getElement(file), file);
    }

    public static RemoteSet parse(Element element, String source) throws GirrException {
        switch (element.getTagName()) {
            case REMOTES_ELEMENT_NAME:
                return new RemoteSet(element, source);
            case REMOTE_ELEMENT_NAME:
                Remote remote = new Remote(element, source);
                return new RemoteSet(remote);
            case COMMANDSET_ELEMENT_NAME:
                CommandSet commandSet = new CommandSet(element);
                return new RemoteSet(commandSet, source);
            case COMMAND_ELEMENT_NAME:
                Command command = new Command(element);
                return new RemoteSet(command, source);
            default:
                throw new GirrException("Unsupported root element type");
        }
    }

    private static boolean ignoreByExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index == -1)
            return false;
        String extension = path.substring(index + 1);
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("pdf");
    }

    /**
     * Restores a RemoteSet from a serialized stream.
     *
     * @param inputStream
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static RemoteSet pmud(InputStream inputStream) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (RemoteSet) objectInputStream.readObject();
        }
    }

    /**
     * Restores a RemoteSet from a serialized file.
     *
     * @param file
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static RemoteSet pmud(File file) throws IOException, ClassNotFoundException {
        return pmud(new FileInputStream(file));
    }

   /**
     * Restores a RemoteSet from a serialized String.
     *
     * @param thing
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static RemoteSet pmud(String thing) throws IOException, ClassNotFoundException {
        return pmud(new File(thing));
    }

    private static IrpDatabase mkIrpDatabase(Element element) {
        try {
            return new IrpDatabase(element);
        } catch (IrpParseException ex) {
            logger.log(Level.WARNING, ex.getLocalizedMessage());
            return new IrpDatabase();
        }
    }

    private final AdminData adminData;
    private final Map<String, Remote> remotes;
    private final IrpDatabase irpDatabase;

    public RemoteSet(String creatingUser, File file) {
        this(creatingUser, file.toString(), parseAsCollection(file));
    }

    public RemoteSet(String creatingUser, String source, Collection<RemoteSet> remoteSets) {
        this(creatingUser, source, null,
                Version.appName,
                Version.versionString,
                null, null);
        for (RemoteSet remoteSet : remoteSets) {
            irpDatabase.patch(remoteSet.getIrpDatabase());
            for (Remote remote : remoteSet) {
                String originalName = remote.getName();
                String name = originalName;
                int i = 0;
                while (remotes.containsKey(name)) {
                    i++;
                    name = originalName + "_" + Integer.toString(i);
                    remote.setName(name);
                    remote.setComment("Name changed from \"" + originalName + "\" to \"" + name + "\".");
                }
                remote.getAdminData().merge(remoteSet.getAdminData());
                remotes.put(name, remote);
            }
        }
    }

    /**
     * This constructor is used to import a Document.
     *
     * @param doc W3C Document with root element "remotes".
     * @throws org.harctoolbox.girr.GirrException
     */
    public RemoteSet(Document doc) throws GirrException {
        this(doc.getDocumentElement(), null);
    }

    /**
     * This constructor is used to import an Element.
     *
     * @param root W3C Element of type "remotes".
     * @param source
     * @throws org.harctoolbox.girr.GirrException
     */
    public RemoteSet(Element root, String source) throws GirrException {
        if (!root.getTagName().equals(REMOTES_ELEMENT_NAME))
            throw new GirrException("Root element not of type \"" + REMOTES_ELEMENT_NAME + "\".");

        NodeList nl = root.getElementsByTagName(ADMINDATA_ELEMENT_NAME);
        adminData = nl.getLength() > 0 ? new AdminData((Element) nl.item(0)) : new AdminData();
        adminData.setSourceIfEmpty(source);
        remotes = new LinkedHashMap<>(INITIAL_HASHMAP_CAPACITY);
        nl = root.getElementsByTagName(REMOTE_ELEMENT_NAME);
        for (int i = 0; i < nl.getLength(); i++) {
            Remote remote = new Remote((Element) nl.item(i), source);
            remotes.put(remote.getName(), remote);
        }

        nl = root.getElementsByTagName(IrpDatabase.IRP_NAMESPACE_PREFIX + ":" + IrpDatabase.PROTOCOLS_NAME);
        if (nl.getLength() > 0) {
            Element protocolsElement = (Element) nl.item(0);
            irpDatabase = mkIrpDatabase(protocolsElement);
        } else
            irpDatabase = new IrpDatabase();
    }

    /**
     * This constructor is used to read a Girr file into a RemoteSet.
     * @param file XML file with root element of type remotes.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public RemoteSet(File file) throws GirrException, IOException, SAXException {
        this(getElement(file), file.toString());
    }

    /**
     * This constructor is used to read a Girr file into a RemoteSet.
     * @param file XML file with root element of type remotes.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public RemoteSet(String file) throws GirrException, IOException, SAXException {
        this(getElement(file), file);
    }

    /**
     * This constructor is used to read a Reader into a RemoteSet.
     * @param reader
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public RemoteSet(Reader reader) throws IOException, SAXException, GirrException {
        this(getElement(reader), null);
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
        this(new AdminData(creatingUser, source, null, tool, toolVersion, tool2, tool2Version, null), null);
    }

    RemoteSet(AdminData adminData, Map<String, Remote> remotes) {
        this.adminData = adminData;
        this.remotes = remotes != null ? remotes : new LinkedHashMap<>(1);
        this.irpDatabase = new IrpDatabase();
    }

    /**
     * This constructor sets up an empty RemoteSet.
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

    /**
     * Create a RemoteSet from a single Remote, given as argument.
     * @param remote
     */
    public RemoteSet(Remote remote) {
        this(remote.getAdminData(), Named.toMap(remote));
    }

    private RemoteSet(CommandSet commandSet, String source) {
        this(new AdminData(source), Named.toMap(new Remote(commandSet)));
    }

    private RemoteSet(Command command, String source) {
        this(new CommandSet(command), source);
    }

    public void setCreationDate(String date) {
        adminData.setCreationDate(date);
    }

    public void setCreationDate() {
        adminData.setCreationDate();
    }

    public void sort(Comparator<? super Named> comparator, boolean recurse) {
        List<Remote> list = new ArrayList<>(remotes.values());
        Collections.sort(list, comparator);
        remotes.clear();
        list.forEach((Remote remote) -> {
            if (recurse)
                remote.sort(comparator);
            remotes.put(remote.getName(), remote);
        });
    }

    public void sort(boolean recurse) {
        sort(new Named.CompareNameCaseInsensitive(), recurse);
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

    @Override
    public Element toElement(Document doc, boolean fatRaw,
            boolean generateParameters, boolean generateProntoHex, boolean generateRaw) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, REMOTES_ELEMENT_NAME);
        Element adminDataEl = adminData.toElement(doc);
        if (adminDataEl.hasChildNodes() || adminDataEl.hasAttributes())
            element.appendChild(adminDataEl);

        if (!irpDatabase.isEmpty())
            element.appendChild(irpDatabase.toElement(doc));

        for (Remote remote : this)
            element.appendChild(remote.toElement(doc, fatRaw, generateParameters , generateProntoHex, generateRaw));

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
        return Collections.unmodifiableCollection(remotes.values());
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

    /**
     * Return the number of contained Remotes.
     * @return
     */
    public int size() {
        return remotes.size();
    }

    /**
     * Applies the strip function to the contained Ramotes.
     */
    public void strip() {
        for (Remote remote : this)
            remote.strip();
    }

    /**
     * Serializes the oject and writes it to a stream.
     * @param outputStream
     * @throws IOException
     */
    public void dump(OutputStream outputStream) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(this);
        }
    }

    /**
     * Serializes the oject and writes it to a file.
     * @param file
     * @throws IOException
     */
    public void dump(File file) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            dump(fileOutputStream);
        }
    }
}
