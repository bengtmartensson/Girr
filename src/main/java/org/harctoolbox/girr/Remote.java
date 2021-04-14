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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.harctoolbox.girr.XmlExporter.APPLICATIONDATA_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.APPLICATION_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.APPPARAMETER_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.COMMANDSET_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.DEVICECLASS_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.DISPLAYNAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlExporter.MANUFACTURER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.MODEL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.NOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.REMOTENAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.REMOTE_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.VALUE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpException;
import static org.harctoolbox.xml.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class describes a remote in Girr.
 * A Remote is essentially an abstraction of a hand-held "clicker" for controlling one device.
 * It has a name for identification, and a number of comment-like text fields. Most importantly,
 * it has a dictionary of CommandSets, indexed by their names.
 */
public final class Remote implements Named, Iterable<CommandSet> {

    private final static Logger logger = Logger.getLogger(Remote.class.getName());

    private MetaData metaData;
    private String comment;
    private Map<String, String> notes;
    private Map<String, CommandSet> commandSets;
    private Map<String, Map<String, String>> applicationParameters;

    /**
     * XML import function.
     *
     * @param element Element to read from.
     * @throws org.harctoolbox.girr.GirrException
     */
    public Remote(Element element) throws GirrException {
        metaData = new MetaData(element.getAttribute(NAME_ATTRIBUTE_NAME),
                element.getAttribute(DISPLAYNAME_ATTRIBUTE_NAME),
                element.getAttribute(MANUFACTURER_ATTRIBUTE_NAME),
                element.getAttribute(MODEL_ATTRIBUTE_NAME),
                element.getAttribute(DEVICECLASS_ATTRIBUTE_NAME),
                element.getAttribute(REMOTENAME_ATTRIBUTE_NAME));
        commandSets = new LinkedHashMap<>(4);
        applicationParameters = new LinkedHashMap<>(4);
        comment = element.getAttribute(COMMENT_ATTRIBUTE_NAME);
        notes = XmlExporter.parseElementsByLanguage(element.getElementsByTagName(NOTES_ELEMENT_NAME));
        NodeList nl = element.getElementsByTagName(APPLICATIONDATA_ELEMENT_NAME);
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
        for (int i = 0; i < nl.getLength(); i++) {
            CommandSet commandSet = new CommandSet((Element) nl.item(i));
            commandSets.put(commandSet.getName(), commandSet);
        }
    }

    /**
     * Construct a Remote from its arguments, general case.
     *
     * @param metaData
     * @param comment
     * @param notes
     * @param commandSetsCollection
     * @param applicationParameters
     */
    // The silly type of the commandSetsCollection is to be avoid name clashes with another constructor.
    // Sorry for that.
    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            Collection<CommandSet> commandSetsCollection, Map<String, Map<String, String>> applicationParameters) {
        this.metaData = metaData;
        this.comment = comment;
        this.notes = notes;
        this.commandSets = new LinkedHashMap<>(4);
        for (CommandSet cmdSet : commandSetsCollection)
            commandSets.put(cmdSet.getName(), cmdSet);
        this.applicationParameters = applicationParameters;
    }

    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            CommandSet commandSet, Map<String, Map<String, String>> applicationParameters) {
        this(metaData, comment, notes, Named.toList(commandSet), applicationParameters);
    }

    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            Map<String, Command> commands, Map<String, Map<String, String>> applicationParameters, String protocolName, Map<String, Long> parameters) {
        this(metaData, comment, notes, new CommandSet("commandSet", null, commands, protocolName, parameters), applicationParameters);
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

    /**
     * XML export function.
     *
     * @param doc
     * @param fatRaw
     * @param generateRaw
     * @param generateCcf
     * @param generateParameters
     * @return XML Element of gid "remote",
     */
    public Element toElement(Document doc, boolean fatRaw, boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, REMOTE_ELEMENT_NAME);
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
            element.appendChild(commandSet.toElement(doc, fatRaw, generateRaw, generateCcf, generateParameters));

        return element;
    }

    public void sort(Comparator<? super Named> comparator) {
        List<CommandSet> list = new ArrayList<>(commandSets.values());
        Collections.sort(list, comparator);
        commandSets.clear();
        list.forEach((CommandSet commandSet) -> {
            commandSet.sort(comparator);
            commandSets.put(commandSet.getName(), commandSet);
        });
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
        String protName = protocolName.toLowerCase(Locale.US);
        for (CommandSet commandSet : this) {
            for (Command command : commandSet) {
                String prtcl = command.getProtocolName();
                if (prtcl == null || !prtcl.equals(protName))
                    return false;
            }
        }
        return true;
    }

    /**
     * Return List of Commands with the selected name, possibly more than one.
     * @param commandName
     * @return List of Commands, possibly empty.
     */
    public List<Command> getAllCommands(String commandName) {
        List<Command> list = new ArrayList<>(4);
        for (CommandSet commandSet : this) {
            Command cmd = commandSet.getCommand(commandName);
            list.add(cmd);
        }
        return list;
    }

    /**
     * Returns a Command with the given name, or null if not found.
     * If the given name is found in several ComandSets, the first found is returned.
     * @param commandName
     * @return Command, or null.
     */
    public Command getCommand(String commandName) {
        for (CommandSet commandSet : this) {
            Command cmd = commandSet.getCommand(commandName);
            if (cmd != null)
                return cmd;
        }
        return null;
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

    public int numberAllCommands() {
        int sum = 0;
        for (CommandSet cmdSet : this)
            sum += cmdSet.size();
        return sum;
    }

    public int numberCommands() {
        return getCommands().size();
    }

    /**
     * Returns all commands contained.
     * If a command name is present in several CommandSets,
     * only one is returned; which one is undefined.
     * @return the commands
     */
    public Map<String, Command> getCommands() {
        Map<String, Command> allCommands = new LinkedHashMap<>(numberCommands());
        for (CommandSet cmdSet : this)
            allCommands.putAll(cmdSet.getCommands());
        return allCommands;
    }

    /**
     * Returns a list of all commands, possibly with duplicate names.
     * @return
     */
    public List<Command> getAllCommands() {
        List<Command> list = new ArrayList<>(numberCommands());
        for (CommandSet commandSet : this)
            list.addAll(commandSet.getCommands().values());
        return list;
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

    /**
     * @param lang
     * @return the notes
     */
    public String getNotes(String lang) {
        return notes.get(lang);
    }

    public void setNotes(String lang, String notes) {
        this.notes.put(lang, notes);
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

    /**
     * This class bundles different meta data for a remote together.
     */
    public static final class MetaData {

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
         * @param name
         * @param displayName
         * @param manufacturer
         * @param model
         * @param deviceClass
         * @param remoteName
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
