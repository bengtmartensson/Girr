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
import static org.harctoolbox.girr.XmlExporter.COMMANDSET_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.COMMAND_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlExporter.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.NOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.PARAMETERS_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.PARAMETER_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.PROTOCOL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.VALUE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.IrpException;
import static org.harctoolbox.xml.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A CommandSet is a set of Commands with unique names.
 * Typically, they share the same protocol, but different parameter values.
 */
public final class CommandSet implements Named, Iterable<Command> {

    private final static Logger logger = Logger.getLogger(CommandSet.class.getName());

    private Map<String, String> notes;
    private String protocolName;
    private final String name;
    private final Map<String, Long> parameters;
    private final Map<String, Command> commands;

    /**
     * Imports a CommandSet from an Element.
     *
     * @param element
     * @throws GirrException
     */
    public CommandSet(Element element) throws GirrException {
        name = element.getAttribute(NAME_ATTRIBUTE_NAME);
        protocolName = null;
        commands = new LinkedHashMap<>(4);
        parameters = new LinkedHashMap<>(4);
        notes = XmlExporter.parseElementsByLanguage(element.getElementsByTagName(NOTES_ELEMENT_NAME));
        // Cannot use getElementsByTagName("parameters") because it will find
        // the parameters of the child commands, which is not what we want.
        NodeList nl = element.getChildNodes();
        for (int nodeNr = 0; nodeNr < nl.getLength(); nodeNr++) {
            if (nl.item(nodeNr).getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element el = (Element) nl.item(nodeNr);
            if (!el.getTagName().equals(PARAMETERS_ELEMENT_NAME))
                continue;
            String newProtocol = el.getAttribute(PROTOCOL_ATTRIBUTE_NAME);
            if (!newProtocol.isEmpty())
                protocolName = newProtocol.toLowerCase(Locale.US);
            NodeList paramList = el.getElementsByTagName(PARAMETER_ELEMENT_NAME);
            for (int i = 0; i < paramList.getLength(); i++) {
                Element e = (Element) paramList.item(i);
                try {
                    parameters.put(e.getAttribute(NAME_ATTRIBUTE_NAME), IrCoreUtils.parseLong(e.getAttribute(VALUE_ATTRIBUTE_NAME)));
                } catch (NumberFormatException ex) {
                    throw new GirrException(ex);
                }
            }
        }

        nl = element.getElementsByTagName(COMMAND_ELEMENT_NAME);
        for (int i = 0; i < nl.getLength(); i++) {
            Command irCommand;
            try {
                irCommand = new Command((Element) nl.item(i), protocolName, parameters);
                commands.put(irCommand.getName(), irCommand);
            } catch (GirrException ex) {
                // Ignore erroneous commands, continue parsing
                String cmdName = ((Element) nl.item(i)).getAttribute(NAME_ATTRIBUTE_NAME);
                logger.log(Level.WARNING, "Command {0}: {1}", new Object[]{cmdName, ex.getMessage()});
            }
        }
    }

    /**
     * Constructs a CommandSet from its argument.
     *
     * @param name
     * @param notes
     * @param commands
     * @param protocolName
     * @param parameters
     */
    public CommandSet(String name, Map<String, String> notes, Map<String, Command> commands, String protocolName, Map<String, Long> parameters) {
        this.name = name != null ? name : "commandSet";
        this.notes = notes != null ? notes : new HashMap<>(0);
        this.commands = commands;
        this.protocolName = protocolName != null ? protocolName.toLowerCase(Locale.US) : null;
        this.parameters = parameters;
    }

    /**
     * Constructs a CommandSet from a single Command.
     * @param command
     */
    public CommandSet(Command command) {
        this(command.getName(), null, Named.toMap(command), null, null);
    }

    /**
     * Returns the Commands in the CommandSet.
     * @return unmodifiable Map.
     */
    public Map<String, Command> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public Command getCommand(String commandName) {
        return commands.get(commandName);
    }

    @Override
    public String getName() {
        return name;
    }

    public int size() {
        return commands.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<Command> iterator() {
        return commands.values().iterator();
    }

    public void sort(Comparator<? super Named> comparator) {
        List<Command> list = new ArrayList<>(commands.values());
        Collections.sort(list, comparator);
        commands.clear();
        list.forEach((cmd) -> {
            commands.put(cmd.getName(), cmd);
        });
    }

    /**
     * Exports the CommandSet to a Document.
     *
     * @param doc
     * @param fatRaw
     * @param generateRaw
     * @param generateCcf
     * @param generateParameters
     * @return newly constructed element, belonging to the doc Document.
     */
    public Element toElement(Document doc, boolean fatRaw, boolean generateRaw, boolean generateCcf, boolean generateParameters) {
        Element element = doc.createElementNS(XmlExporter.GIRR_NAMESPACE, COMMANDSET_ELEMENT_NAME);
        element.setAttribute(NAME_ATTRIBUTE_NAME, name);
        notes.entrySet().stream().map((note) -> {
            Element notesEl = doc.createElementNS(GIRR_NAMESPACE, NOTES_ELEMENT_NAME);
            notesEl.setAttribute(XML_LANG_ATTRIBUTE_NAME, note.getKey());
            notesEl.setTextContent(note.getValue());
            return notesEl;
        }).forEachOrdered((notesEl) -> {
            element.appendChild(notesEl);
        });
        if (parameters != null && generateParameters) {
            Element parametersEl = doc.createElementNS(XmlExporter.GIRR_NAMESPACE, PARAMETERS_ELEMENT_NAME);
            parametersEl.setAttribute(PROTOCOL_ATTRIBUTE_NAME, protocolName);
            element.appendChild(parametersEl);
            parameters.entrySet().stream().map((parameter) -> {
                Element parameterEl = doc.createElementNS(XmlExporter.GIRR_NAMESPACE, PARAMETER_ELEMENT_NAME);
                parameterEl.setAttribute(NAME_ATTRIBUTE_NAME, parameter.getKey());
                parameterEl.setAttribute(VALUE_ATTRIBUTE_NAME, parameter.getValue().toString());
                return parameterEl;
            }).forEachOrdered((parameterEl) -> {
                parametersEl.appendChild(parameterEl);
            });
        }
        if (commands != null) {
            commands.values().forEach((command) -> {
                element.appendChild(command.toElement(doc, null, fatRaw,
                        generateRaw, generateCcf, generateParameters));
            });
        }
        return element;
    }

    /**
     * Applies the format argument to all Command's in the CommandSet.
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
}
