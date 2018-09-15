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

import java.util.HashMap;
import java.util.LinkedHashMap;
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
import static org.harctoolbox.ircore.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.harctoolbox.irp.IrpException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A CommandSet is a set of Command's with the same protocol, but different parameter values.
 */
public final class CommandSet {

    private final static Logger logger = Logger.getLogger(CommandSet.class.getName());

    private Map<String, String> notes;
    private String protocol;
    private final String name;
    private final Map<String, Long> parameters;
    private final Map<String, Command> commands;

    /**
     * Imports a CommandSet from an Element.
     *
     * @param element
     * @throws ParseException
     */
    CommandSet(Element element) throws GirrException {
        name = element.getAttribute(NAME_ATTRIBUTE_NAME);
        protocol = null;
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
                protocol = newProtocol;
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
                irCommand = new Command((Element) nl.item(i), protocol, parameters);
                commands.put(irCommand.getName(), irCommand);
            } catch (GirrException ex) {
                // Ignore erroneous commands, continue parsing
                logger.log(Level.WARNING, null, ex);
            }
        }
    }

    /**
     * Constructs a CommandSet from its argument.
     *
     * @param name
     * @param notes
     * @param commands
     * @param protocol
     * @param parameters
     */
    CommandSet(String name, Map<String, String> notes, Map<String, Command> commands, String protocol, Map<String, Long> parameters) {
        this.name = name != null ? name : "commandSet";
        this.notes = notes != null ? notes : new HashMap<>(0);
        this.commands = commands;
        this.protocol = protocol;
        this.parameters = parameters;
    }

    /**
     * Returns the Commands in the CommandSet.
     * @return
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String, Command> getCommands() {
        return commands;
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
            parametersEl.setAttribute(PROTOCOL_ATTRIBUTE_NAME, protocol.toLowerCase(Locale.US));
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
