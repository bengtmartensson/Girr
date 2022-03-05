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

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.harctoolbox.girr.AdminData.printIfNonempty;
import static org.harctoolbox.girr.Command.INITIAL_HASHMAP_CAPACITY;
import static org.harctoolbox.girr.XmlStatic.ADMINDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.APPLICATIONDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.APPLICATION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.APPPARAMETER_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.COMMANDSET_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.DEVICECLASS_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.DISPLAYNAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlStatic.MANUFACTURER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.MODEL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTENAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTE_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.VALUE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.xml.XmlUtils;
import static org.harctoolbox.xml.XmlUtils.ENGLISH;
import static org.harctoolbox.xml.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class describes a remote in Girr.
 * A Remote is essentially an abstraction of a hand-held "clicker" for controlling one device.
 * It has a name for identification, and a number of comment-like text fields. Most importantly,
 * it has a dictionary of CommandSets, indexed by their names.
 * The CommandSets contain Commands, having names that are unique within their CommandSet.
 * Note that Commands with the same name can be present in different CommandSets.
 * For example, some TVs have both an RC5 and an RC6 Command set, both containing a command "power",
 * but these are or course different.
 */
public final class Remote extends XmlExporter implements Named, Iterable<CommandSet> {

    private final static Logger logger = Logger.getLogger(Remote.class.getName());

    private final MetaData metaData;
    private final AdminData adminData;
    private String comment;
    private final Map<String, String> notes;
    private final Map<String, CommandSet> commandSets;
    private final Map<String, Map<String, String>> applicationParameters;

    /**
     * This constructor is used to read a Girr file into a Remote.
     * @param file Girr file; must have "remote" as the root element.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public Remote(String file) throws GirrException, IOException, SAXException {
        this(XmlUtils.openXmlThing(file));
    }

    /**
     * This constructor is used to read a Reader into a Remote.
     * @param reader Reader producing a Document with a top level element "remote".
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public Remote(Reader reader) throws IOException, SAXException, GirrException {
        this(XmlUtils.openXmlReader(reader, null, true, true));
    }

    /**
     * This constructor is used to import a Document.
     * @param doc W3C Document with root element of type "remote".
     * @throws org.harctoolbox.girr.GirrException
     */
    public Remote(Document doc) throws GirrException {
          this(doc.getDocumentElement(), null);
    }

    /**
     * XML import function.
     *
     * @param element Element to read from. Must be tag name "remote".
     * @param source Textual representation of the origin of the information.
     * @throws org.harctoolbox.girr.GirrException
     */
    public Remote(Element element, String source) throws GirrException {
        if (!element.getTagName().equals(REMOTE_ELEMENT_NAME))
            throw new GirrException("Element name is not " + REMOTE_ELEMENT_NAME);

        metaData = new MetaData(element.getAttribute(NAME_ATTRIBUTE_NAME),
                element.getAttribute(DISPLAYNAME_ATTRIBUTE_NAME),
                element.getAttribute(MANUFACTURER_ATTRIBUTE_NAME),
                element.getAttribute(MODEL_ATTRIBUTE_NAME),
                element.getAttribute(DEVICECLASS_ATTRIBUTE_NAME),
                element.getAttribute(REMOTENAME_ATTRIBUTE_NAME));
        NodeList nl = element.getElementsByTagName(ADMINDATA_ELEMENT_NAME);
        adminData = nl.getLength() > 0 ? new AdminData((Element) nl.item(0)) : new AdminData();
        if (source != null && !source.isEmpty())
            adminData.setSourceIfEmpty(source);
        applicationParameters = new LinkedHashMap<>(INITIAL_HASHMAP_CAPACITY);
        comment = element.getAttribute(COMMENT_ATTRIBUTE_NAME);
        notes = XmlStatic.parseElementsByLanguage(element.getElementsByTagName(NOTES_ELEMENT_NAME));
        nl = element.getElementsByTagName(APPLICATIONDATA_ELEMENT_NAME);
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            NodeList nodeList = el.getElementsByTagName(APPPARAMETER_ELEMENT_NAME);
            Map<String, String> map = new HashMap<>(32);
            for (int index = 0; index < nodeList.getLength(); index++) {
                Element par = (Element) nodeList.item(index);
                map.put(par.getAttribute(NAME_ATTRIBUTE_NAME), par.getAttribute(VALUE_ATTRIBUTE_NAME));
            }
            applicationParameters.put(el.getAttribute(APPLICATION_ATTRIBUTE_NAME), map);
        }

        nl = element.getElementsByTagName(COMMANDSET_ELEMENT_NAME);
        commandSets = new LinkedHashMap<>(nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            CommandSet commandSet = new CommandSet((Element) nl.item(i));
            commandSets.put(commandSet.getName(), commandSet);
        }
    }

    /**
     * Construct a Remote from its arguments, general case.
     *
     * @param metaData Remote.MetaData meta data
     * @param source Textual representation of the origin of the information
     * @param comment Textual comment
     * @param notes Textual notes
     * @param commandSetsCollection Collection&lt;CommandSet&gt;
     * @param applicationParameters
     */
    // The silly type of the commandSetsCollection is to be avoid name clashes with another constructor.
    // Sorry for that.
    public Remote(MetaData metaData, String source, String comment, Map<String, String> notes,
            Collection<CommandSet> commandSetsCollection, Map<String, Map<String, String>> applicationParameters) {
        this.adminData = new AdminData(source);
        this.metaData = metaData;
        this.comment = comment;
        this.notes = notes != null ? notes : new HashMap<>(2);
        this.commandSets = new LinkedHashMap<>(INITIAL_HASHMAP_CAPACITY);
        if (commandSetsCollection != null)
            commandSetsCollection.forEach(cmdSet -> {
                commandSets.put(cmdSet.getName(), cmdSet);
        });
        this.applicationParameters = applicationParameters;
    }

    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            Collection<CommandSet> commandSetsCollection, Map<String, Map<String, String>> applicationParameters) {
        this(metaData, null, comment, notes, commandSetsCollection, applicationParameters);
    }

    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            CommandSet commandSet, Map<String, Map<String, String>> applicationParameters) {
        this(metaData, comment, notes, Named.toList(commandSet), applicationParameters);
    }

    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            Map<String, Command> commands, Map<String, Map<String, String>> applicationParameters, String protocolName, Map<String, Long> parameters) {
        this(metaData, comment, notes, new CommandSet("commandSet", null, commands, protocolName, parameters), applicationParameters);
    }

    public Remote(MetaData metaData, String source, String comment, Map<String, String> notes,
            Map<String, Command> commands, Map<String, Map<String, String>> applicationParameters, String protocolName, Map<String, Long> parameters) {
        this(metaData, source, comment, notes, Named.toList(new CommandSet("commandSet", null, commands, protocolName, parameters)), applicationParameters);
    }

    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            Map<String, Command> commands, Map<String, Map<String, String>> applicationParameters) {
        this(metaData, comment, notes, commands, applicationParameters, null, null);
    }

    /**
     * This constructor constructs a Remote from one single IrSignal.
     *
     * @param irSignal
     * @param name Name of command to be constructed.
     * @param comment Comment for command to be constructed.
     * @param deviceName
     */
    public Remote(IrSignal irSignal, String name, String comment, String deviceName) {
        this(new MetaData(deviceName),
                null, // comment
                null, // notes
                new CommandSet(new Command(name, comment, irSignal)),
                null /* applicationParameters */);
    }

    public Remote(CommandSet commandSet) {
        this(new MetaData("unnamed"), null, null, commandSet, null);
    }

    @Override
    public Element toElement(Document doc, boolean fatRaw, boolean generateParameters, boolean generateProntoHex, boolean generateRaw) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, REMOTE_ELEMENT_NAME);
            Element adminDataEl = adminData.toElement(doc);
            if (adminDataEl.hasChildNodes() || adminDataEl.hasAttributes())
                element.appendChild(adminDataEl);
        element.setAttribute(NAME_ATTRIBUTE_NAME, metaData.name);
        if (metaData.displayName != null && !metaData.displayName.isEmpty())
            element.setAttribute(DISPLAYNAME_ATTRIBUTE_NAME, metaData.displayName);
        if (metaData.manufacturer != null && !metaData.manufacturer.isEmpty())
            element.setAttribute(MANUFACTURER_ATTRIBUTE_NAME, metaData.manufacturer);
        if (metaData.model != null && !metaData.model.isEmpty())
            element.setAttribute(MODEL_ATTRIBUTE_NAME, metaData.model);
        if (metaData.deviceClass != null && !metaData.deviceClass.isEmpty())
            element.setAttribute(DEVICECLASS_ATTRIBUTE_NAME, metaData.deviceClass);
        if (metaData.remoteName != null && !metaData.remoteName.isEmpty())
            element.setAttribute(REMOTENAME_ATTRIBUTE_NAME, metaData.remoteName);
        if (comment != null && !comment.isEmpty())
            element.setAttribute(COMMENT_ATTRIBUTE_NAME, comment);
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

        if (applicationParameters != null) {
            applicationParameters.entrySet().forEach((kvp) -> {
                if (kvp.getValue() != null) {
                    Element appEl = doc.createElementNS(GIRR_NAMESPACE, APPLICATIONDATA_ELEMENT_NAME);
                    appEl.setAttribute(APPLICATION_ATTRIBUTE_NAME, kvp.getKey());
                    element.appendChild(appEl);
                    kvp.getValue().entrySet().stream().map((param) -> {
                        Element paramEl = doc.createElementNS(GIRR_NAMESPACE, APPPARAMETER_ELEMENT_NAME);
                        paramEl.setAttribute(NAME_ATTRIBUTE_NAME, param.getKey());
                        paramEl.setAttribute(VALUE_ATTRIBUTE_NAME, param.getValue());
                        return paramEl;
                    }).forEachOrdered((paramEl) -> {
                        appEl.appendChild(paramEl);
                    });
                }
            });
        }

        for (CommandSet commandSet : this)
            element.appendChild(commandSet.toElement(doc, fatRaw, generateParameters, generateProntoHex, generateRaw));

        return element;
    }

    /**
     * Apply the sort function to all contained CommandSets.
     * @param comparator
     */
    public void sort(Comparator<? super Named> comparator) {
        List<CommandSet> list = new ArrayList<>(commandSets.values());
        Collections.sort(list, comparator);
        Named.populateMap(commandSets, list);
    }

    public void sortCommands(Comparator<? super Named> comparator) {
        for (CommandSet commandSet : this)
            commandSet.sort(comparator);
    }

    public void sortCommands() {
        sortCommands(new Named.CompareNameCaseSensitive());
    }

    public void sortCommandsIgnoringCase() {
        sort(new Named.CompareNameCaseInsensitive());
    }

    public void sort() {
        sort(new Named.CompareNameCaseSensitive());
    }

    public void sortIgnoringCase() {
        sort(new Named.CompareNameCaseInsensitive());
    }

    /**
     * Replaces all CommandSets with a single one,
     * containing all the commands of the original CommandSets.
     * Note that only one commend with a particular name is present after this operation,
     * so information may be lost.
     */
    public void normalize() {
        if (commandSets.size() < 2)
            return;

        int numberOfOriginalCommands = getNumberOfCommands();
        Map<String, Command> commands = new LinkedHashMap<>(numberOfOriginalCommands);
        StringJoiner stringJoiner = new StringJoiner(", ", "Merge of CommandSets ", "");

        for (CommandSet cmdSet : this) {
            for (Command cmd : cmdSet)
                commands.put(cmd.getName(), cmd);
            stringJoiner.add(cmdSet.getName());
        }
        Map<String, String> notesCmdSet = new HashMap<>(1);
        String noteString = stringJoiner.toString();
        CommandSet commandSet = new CommandSet("MergedCommandSet", notesCmdSet, commands, null, null);
        commandSets.clear();
        commandSets.put(commandSet.getName(), commandSet);
        int missing = numberOfOriginalCommands - commandSet.size();
        if (missing > 0)
            noteString += "\n" + missing + " commands lost in merge";
        notesCmdSet.put(XmlUtils.ENGLISH, noteString);
    }

    /**
     * Applies the format argument to all Command's in the Remote.
     *
     * @param format
     * @param repeatCount
     */
    public void addFormat(Command.CommandTextFormat format, int repeatCount) {
        for (CommandSet commandSet : this) {
            for (Command command : commandSet) {
                try {
                    command.addFormat(format, repeatCount);
                } catch (IrCoreException | IrpException ex) {
                    logger.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    /**
     * Returns true if and only if all contained commands has the protocol in the argument.
     * @param protocolName
     * @return
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public boolean hasThisProtocol(String protocolName) throws IrpException, IrCoreException {
        for (CommandSet commandSet : this) {
            for (Command command : commandSet) {
                String prtcl = command.getProtocolName();
                if (prtcl == null || !prtcl.equalsIgnoreCase(protocolName))
                    return false;
            }
        }
        return true;
    }

    /**
     * Returns true if and only if at least one contained commands has the protocol in the argument.
     * @param protocolName
     * @return
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public boolean containsThisProtocol(String protocolName) throws IrpException, IrCoreException {
        for (CommandSet commandSet : this) {
            for (Command command : commandSet) {
                String prtcl = command.getProtocolName();
                if (prtcl != null && prtcl.equalsIgnoreCase(protocolName))
                    return true;
            }
        }
        return false;
    }

    /**
     *
     * @return the metaData
     */
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     *
     * @return Name of the Remote.
     */
    @Override
    public String getName() {
        return metaData.name;
    }

    public void setName(String name) {
        metaData.name = name;
    }

    /**
     *
     * @return displayName of the Remote.
     */
    public String getDisplayName() {
        return metaData.displayName;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @deprecated Loop the CommandSets instead.
     * @return
     */
    public int getNumberOfCommands() {
        int sum = 0;
        for (CommandSet cmdSet : this)
            sum += cmdSet.size();
        return sum;
    }

    /**
     * Returns all commands contained.
     * If there are several CommandSets, the index names will be two part: commandSet : command.
     * @return the commands
     * @deprecated Loop the CommandSets instead.
     */
    public Collection<Command> getCommands() {
        if (commandSets.isEmpty())
            return new ArrayList<>(0);
        if (commandSets.size() == 1)
            return iterator().next().getCommands();

        List<Command> allCommands = new ArrayList<>(getNumberOfCommands());
        for (CommandSet cmdSet : this) {
            //String prefix = cmdSet.getName() + ':';
            for (Command cmd : cmdSet) {
                //String newName = prefix + cmd.getName();
                allCommands.add(cmd);
            }
        }
        return allCommands;
    }

    /**
     * @deprecated Loop the CommandSets instead.
     * @param name
     * @return
     */
    public List<Command> getCommand(String name) {
        List<Command> commands = new ArrayList<>(commandSets.size());
        for (CommandSet cmdSet : this) {
            Command cmd = cmdSet.getCommand(name);
            if (cmd != null)
                commands.add(cmd);
        }
        return commands;
    }

    /**
     * @return the applicationParameters
     */
    public Map<String, Map<String, String>> getApplicationParameters() {
        return Collections.unmodifiableMap(applicationParameters);
    }

    /**
     * @return the manufacturer
     */
    public String getManufacturer() {
        return metaData.manufacturer;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return metaData.model;
    }

    /**
     * @return the deviceClass
     */
    public String getDeviceClass() {
        return metaData.deviceClass;
    }

    /**
     * @return the remoteName
     */
    public String getRemoteName() {
        return metaData.remoteName;
    }

    AdminData getAdminData() {
        return adminData;
    }

    public String getFormattedAdminData() {
        return adminData.toFormattedString();
    }

    /**
     * @return the notes
     */
    Map<String, String> getAllNotes() {
        return Collections.unmodifiableMap(notes);
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes.get(ENGLISH);
    }

    /**
     * @param lang
     * @return the notes
     */
    public String getNotes(String lang) {
        return notes.get(lang);
    }

    public void setNotes(String lang, String string) {
        this.notes.put(lang, string);
    }

    public void setNotes(String string) {
        this.notes.put(ENGLISH, string);
    }

    void setComment(String comment) {
        this.comment = comment;
    }

    public void checkForParameters() throws IrpException, IrCoreException {
        for (CommandSet commandSet : this)
            for (Command command : commandSet)
                command.checkForParameters();
    }

    @Override
    public Iterator<CommandSet> iterator() {
        return commandSets.values().iterator();
    }

    public Map<String, CommandSet> getCommandSets() {
        return Collections.unmodifiableMap(commandSets);
    }

    public CommandSet getCommandSet(String name) {
        return commandSets.get(name);
    }

    public Command getCommand(String commandSetName, String name) {
        return getCommandSet(commandSetName).getCommand(name);
    }

    /**
     * Apply the strip function to all the CommandSets.
     */
    public void strip() {
        for (CommandSet commandSet : this)
            commandSet.strip();
    }

    /**
     * This class bundles different meta data for a remote together.
     */
    public static final class MetaData implements Serializable {

        private static boolean isVoid(String s) {
            return s == null || s.isEmpty();
        }

        private String name;
        private String displayName;
        private String manufacturer;
        private String model;
        private String deviceClass;
        private String remoteName;

        /**
         * Constructor for empty MetaData.
         */
        public MetaData() {
            this.name = null;
            this.displayName = null;
            this.manufacturer = null;
            this.model = null;
            this.deviceClass = null;
            this.remoteName = null;
        }

        /**
         * Constructor with name.
         * @param name
         */
        public MetaData(String name) {
            this();
            this.name = name;
        }

        /**
         * Generic constructor.
         * @param name Name of the remote, as how users and other program is referring to it. Should be in ENglish and not containing special characters.
         * @param displayName A "nicely looking" displayable name. May contain spaces and special characters.
         * @param manufacturer Manufacturer of the device.
         * @param model Model name, to be identified by humans-
         * @param deviceClass Device class, for example "TV".
         * @param remoteName Manufacturers name of original remote.
         */
        public MetaData(String name, String displayName, String manufacturer, String model,
                String deviceClass, String remoteName) {
            this.name = name;
            this.displayName = displayName;
            this.manufacturer = manufacturer;
            this.model = model;
            this.deviceClass = deviceClass;
            this.remoteName = remoteName;
        }

        public String toFormattedString() {
            StringBuilder sb = new StringBuilder(256);
            printIfNonempty(sb, NAME_ATTRIBUTE_NAME, name);
            printIfNonempty(sb, DISPLAYNAME_ATTRIBUTE_NAME, displayName);
            printIfNonempty(sb, MANUFACTURER_ATTRIBUTE_NAME, manufacturer);
            printIfNonempty(sb, MODEL_ATTRIBUTE_NAME, model);
            printIfNonempty(sb, DEVICECLASS_ATTRIBUTE_NAME, deviceClass);
            printIfNonempty(sb, REMOTENAME_ATTRIBUTE_NAME, remoteName);

            int len = sb.length();
            if (len > 1)
                sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        /**
         * Returns true if there is no non-trivial content.
         * @return
         */
        public boolean isEmpty() {
            return isVoid(name)
                    && isVoid(displayName)
                    && isVoid(manufacturer)
                    && isVoid(model)
                    && isVoid(deviceClass)
                    && isVoid(remoteName);
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the displayName
         */
        public String getDisplayName() {
            return displayName;
        }

        /**
         * @return the manufacturer
         */
        public String getManufacturer() {
            return manufacturer;
        }

        /**
         * @return the model
         */
        public String getModel() {
            return model;
        }

        /**
         * @return the deviceClass
         */
        public String getDeviceClass() {
            return deviceClass;
        }

        /**
         * @return the remoteName
         */
        public String getRemoteName() {
            return remoteName;
        }
    }
}
