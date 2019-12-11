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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import static org.harctoolbox.ircore.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.harctoolbox.irp.IrpException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class describes a remote in Girr.
 * A Remote is essentially an abstraction of a hand-held "clicker" for controlling one device.
 * It has a name for identification, and a number of comment-like text fields. Most importantly,
 * it has a dictionary of Commands, indexed by their names.
 */
public final class Remote implements Iterable<Command> {

    private final static Logger logger = Logger.getLogger(Remote.class.getName());

    private static Map<String, Command> commandToMap(Command command) {
        Map<String, Command> result = new HashMap<>(1);
        result.put(command.getName(), command);
        return result;
    }

    private MetaData metaData;
    private String comment;
    private Map<String, String> notes;
    private String protocol;
    private Map<String, Long> parameters;
    private Map<String, Command> commands;
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
        commands = new LinkedHashMap<>(32);
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
            commands.putAll(commandSet.getCommands());
        }
    }

    /**
     * Construct a Remote from its arguments, general case.
     *
     * @param metaData
     * @param comment
     * @param notes
     * @param commands
     * @param applicationParameters
     * @param protocol
     * @param parameters
     */
    public Remote(MetaData metaData, String comment, Map<String, String> notes,
            Map<String, Command> commands, Map<String, Map<String, String>> applicationParameters,
            String protocol, Map<String, Long> parameters) {
        this.metaData = metaData;
        this.comment = comment;
        this.notes = notes;
        this.commands = commands;
        this.applicationParameters = applicationParameters;
        this.protocol = protocol;
        this.parameters = parameters;
    }

    /**
     * Convenience version of the general constructor, with default values.
     *
     * @param metaData
     * @param comment
     * @param notes
     * @param commands
     * @param applicationParameters
     */
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
                null, // comment,
                null, // notes,
                commandToMap(new Command(name, comment, irSignal)),
                null);
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
            });
        }

        CommandSet commandSet = new CommandSet(null, null, commands, protocol, parameters);
        element.appendChild(commandSet.toElement(doc, fatRaw, generateRaw, generateCcf, generateParameters));

        return element;
    }

    public void sort(Comparator<Command> comparator) {
        ArrayList<Command> list = new ArrayList<>(commands.values());
        Collections.sort(list, comparator);
        commands.clear();
        list.forEach((cmd) -> {
            commands.put(cmd.getName(), cmd);
        });
    }

    /**
     * Applies the format argument to all Command's in the Remote.
     *
     * @param format
     * @param repeatCount
     */
    public void addFormat(Command.CommandTextFormat format, int repeatCount) {
        commands.values().forEach((command) -> {
            try {
                command.addFormat(format, repeatCount);
            } catch (IrCoreException | IrpException ex) {
                logger.log(Level.WARNING, null, ex);
            }
        });
    }

    /**
     * Returns true if and only if all contained commands has the protocol in the argument.
     * @param protocolName
     * @return
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public boolean hasThisProtocol(String protocolName) throws IrpException, IrCoreException {
        for (Command command : commands.values()) {
            String prtcl = command.getProtocolName();
            if (prtcl == null || !prtcl.equalsIgnoreCase(protocolName))
                return false;
        }
        return true;
    }

    /**
     * Return the Command with the selected name.
     * @param commandName
     * @return
     */
    public Command getCommand(String commandName) {
        return commands.get(commandName);
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
     * @return the commands
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String, Command> getCommands() {
        return commands;
    }

    /**
     * @return the applicationParameters
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String, Map<String, String>> getApplicationParameters() {
        return applicationParameters;
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
        for (Command command : commands.values())
            command.checkForParameters();
    }

    @Override
    public Iterator<Command> iterator() {
        return commands.values().iterator();
    }

    /**
     * This class bundles different data for a remote together.
     */
    public static final class MetaData {
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

    public static class CompareNameCaseSensitive implements Comparator<Remote>, Serializable {
        @Override
        public int compare(Remote o1, Remote o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class CompareNameCaseInsensitive implements Comparator<Remote>, Serializable {
        @Override
        public int compare(Remote o1, Remote o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
