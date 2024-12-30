/*
 Copyright (C) 2013, 2014, 2015, 2018, 2021 Bengt Martensson.

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.harctoolbox.girr.XmlStatic.COMMAND_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.DISPLAYNAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.DUTYCYCLE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.ENDING_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.EQUALS;
import static org.harctoolbox.girr.XmlStatic.FLASH_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.FORMAT_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.FREQUENCY_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.F_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.GAP_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlStatic.INTRO_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.MASTER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.PARAMETERS_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.PARAMETER_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.PRONTO_HEX_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.PROTOCOL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.RAW_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.REPEAT_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.SPACE;
import static org.harctoolbox.girr.XmlStatic.TOGGLE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.VALUE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irp.Assignment;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.DomainViolationException;
import org.harctoolbox.irp.ElementaryDecode;
import org.harctoolbox.irp.Expression;
import org.harctoolbox.irp.InvalidNameException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpInvalidArgumentException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.NameEngine;
import org.harctoolbox.irp.NameUnassignedException;
import org.harctoolbox.irp.ParameterSpec;
import org.harctoolbox.irp.Protocol;
import org.harctoolbox.irp.ShortPronto;
import static org.harctoolbox.xml.XmlUtils.ENGLISH;
import static org.harctoolbox.xml.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class models the command in Girr. A command is essentially an IR signal,
 * given either by protocol/parameters or timing data, and a name.
 * <p>
 * Some protocols have toggles, a persistent variable that changes between invocations.
 * If a such a protocol is used, there are two cases
 * <ol>
 * <li>It the toggle parameter is explicitly specified, the signal is treated no different from other signals,
 * and no particular treatment of the toggle parameter occurs.
 * <li>If the toggle parameter is not explicitly specified, the Pronto Hex and the raw versions are computed for
 * all values of the toggle. They can be accessed by the version of the functions getProntoHex, toIrSignal, getIntro, getRepeat, getEnding
 * taking an argument, corresponding to the value of the toggle parameter.
 * </ol>
 *
 * <p>
 * The member functions of class may throw IrpExceptions when they encounter
 * erroneous data. The other classes in the package may not; they should just
 * ignore individual unparseable commands.
 */

@SuppressWarnings("serial")
public final class Command extends XmlExporter implements Named {

    private final static Logger logger = Logger.getLogger(Command.class.getName());

    static final int INITIAL_HASHMAP_CAPACITY = 4;
    private static final int INITIAL_STRINGBUILDER_CAPACITY = 64;

    /**
     * Name of the parameter containing the toggle in the IRP protocol.
     */
    public static final String TOGGLE_PARAMETER_NAME = "T";

    /**
     * Name of the function name parameter.
     */
    public static final String F_PARAMETER_NAME = "F";

    /**
     * Name of the parameter denoting device number.
     */
    public static final String D_PARAMETER_NAME = "D";

    /**
     * Name of the subparameter parameter.
     */
    public static final String S_PARAMETER_NAME = "S";

    private static final String DUMMY_COMMAND_NAME    = "dummy-command";

    private static IrpDatabase irpDatabase = null;
    private static Decoder decoder = null;
    private static Decoder.DecoderParameters decoderParameters = new Decoder.DecoderParameters();

    private static boolean useInheritanceForXml = true;

    /**
     * If true, accept commands without content, that is, only with a name.
     */
    private static boolean acceptEmptyCommands = false;

    static {
        try {
            IrpDatabase database = new IrpDatabase((String) null);
            decoder = new Decoder(database);
            irpDatabase = database;
        } catch (IOException | IrpParseException | SAXException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    /**
     * If set to true, tries to use protocol/parameter inheritance when generating
     * XML code for Commands.
     * @param val
     */
    public static void setUseInheritanceForXml(boolean val) {
        useInheritanceForXml = val;
    }

    public static boolean isUseInheritanceForXml() {
        return useInheritanceForXml;
    }

    /**
     * If called with argument true, commands without a content, only a name, will be accepted.
     * @param acceptEmpties
     */
    public static void setAcceptEmptyCommands(boolean acceptEmpties) {
        acceptEmptyCommands = acceptEmpties;
    }

    public static boolean isAcceptEmptyCommands() {
        return acceptEmptyCommands;
    }

    /**
     * Sets an global IrpDatabase instance, which will be used in subsequent transformations from parameter format,
     * and for decodes.
     * @param newIrpDatabase IrpDatabase instance
     * @throws org.harctoolbox.irp.IrpParseException
     */
    public static void setIrpDatabase(IrpDatabase newIrpDatabase) throws IrpParseException {
        irpDatabase = newIrpDatabase;
        decoder = new Decoder(irpDatabase);
    }

    /**
     * Sets an global Decoder.DecoderParameters instance, which will be used in subsequent transformations from parameter format,
     * and for decodes.
     * @param newDecoderParameters
     */
    public static void setDecoderParameters(Decoder.DecoderParameters newDecoderParameters) {
        decoderParameters = newDecoderParameters;
    }

    /**
     * Creates and sets an global IrpDatabase instance, which will be used in subsequent transformations from parameter format,
     * and for decodes.
     * @param irpProtocolsIniPath Filename of IrpProtocols.xml
     * @throws java.io.IOException
     * @throws org.harctoolbox.irp.IrpParseException
     * @throws org.xml.sax.SAXException
     */
    public static void setIrpDatabase(String irpProtocolsIniPath) throws IOException, IrpParseException, SAXException {
        setIrpDatabase(new IrpDatabase(irpProtocolsIniPath));
    }

    private static String toPrintString(Map<String, Long> map) {
        if (map == null || map.isEmpty())
            return "";
        StringBuilder str = new StringBuilder(INITIAL_STRINGBUILDER_CAPACITY);
        map.entrySet().forEach((kvp) -> {
            str.append(kvp.getKey()).append(EQUALS).append(Long.toString(kvp.getValue())).append(SPACE);
        });
        return str.substring(0, str.length() - 1);
    }

    private static void processRaw(Document doc, Element element, String sequence, String tagName, boolean fatRaw) {
        Element el = toElement(doc, sequence, tagName, fatRaw);
        if (el != null)
            element.appendChild(el);
    }

    private static Element toElement(Document doc, String sequence, String tagName, boolean fatRaw) {
        if (sequence == null || sequence.isEmpty())
            return null;

        Element el = doc.createElementNS(GIRR_NAMESPACE, tagName);
        if (fatRaw)
            insertFatElements(doc, el, sequence);
        else
            el.setTextContent(sequence);

        return el;
    }

    private static void insertFatElements(Document doc, Element el, String sequence) {
        String[] durations = sequence.split(SPACE);
        for (int i = 0; i < durations.length; i++) {
            String duration = durations[i].replaceAll("[\\+-]", "");
            Element e = doc.createElementNS(GIRR_NAMESPACE, i % 2 == 0 ? FLASH_ELEMENT_NAME : GAP_ELEMENT_NAME);
            e.setTextContent(duration);
            el.appendChild(e);
        }
    }

    private static String parseSequence(Element element) {
        if (element.getElementsByTagName(FLASH_ELEMENT_NAME).getLength() > 0) {
            StringBuilder str = new StringBuilder(INITIAL_STRINGBUILDER_CAPACITY);
            NodeList nl = element.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() != Node.ELEMENT_NODE)
                    continue;
                Element el = (Element) nl.item(i);
                switch (el.getTagName()) {
                    case FLASH_ELEMENT_NAME:
                        str.append(" +").append(el.getTextContent());
                        break;
                    case GAP_ELEMENT_NAME:
                        str.append(" -").append(el.getTextContent());
                        break;
                    default:
                        logger.log(Level.SEVERE, "Invalid tag name: {0}", el.getTagName());
                        throw new ThisCannotHappenException("Invalid tag name");
                }
            }
            return str.substring(1);
        } else
            return element.getTextContent();
    }

    /**
     * Concatenates the Commands in the argument using IrSignal.toModulatedIrSequence.
     * @param commands Collection of Commands to be concatenated.
     * @return ModulatedIrSequence
     *
     * @throws IrCoreException
     * @throws IrpException
     */
    public static ModulatedIrSequence concatenateAsSequence(Collection<Command> commands) throws IrpException, IrCoreException {
        Double frequency = null;
        Double dutyCycle = null;
        IrSequence seq = new IrSequence();
        for (Command c : commands) {
            if (frequency == null) // take the first sensible frequency
                frequency = c.getFrequency();
            if (dutyCycle == null)
                dutyCycle = c.getDutyCycle();
            seq = seq.append(c.toIrSignal().toModulatedIrSequence(1));
        }
        return new ModulatedIrSequence(seq, frequency, dutyCycle);
    }

    public static boolean isKnownProtocol(String protocolName) {
        return (protocolName != null) && ! protocolName.isEmpty() && irpDatabase.isKnownExpandAlias(protocolName);
    }

    private static Map<String, Long> mkMap(Long device, Long subdevice) {
        Map<String, Long>params = new LinkedHashMap<>(INITIAL_HASHMAP_CAPACITY);
        if (device != null)
            params.put(D_PARAMETER_NAME, device);
        if (subdevice != null)
            params.put(S_PARAMETER_NAME, subdevice);
        return params;
    }

    private static List<Assignment> parseTransformations(String str) {
        List<Assignment> list = new ArrayList<>(8);
        String payload = str.trim().replaceFirst("^\\{", "").replaceFirst("\\}$", "").replaceAll("\\s*=\\s*", "=");
        String[] array = payload.split(",");
        for (String s : array)
            list.add(new Assignment(s));

        return list;
    }

    private Protocol protocol;
    private MasterType masterType;
    private final Map<String, String> notes;
    private final String name;
    private final String displayName;
    private String protocolName; // should always be lowercase
    private Map<String, Long> parameters;
    private Integer frequency;
    private Double dutyCycle;
    private String[] intro;
    private String[] repeat;
    private String[] ending;
    private String[] prontoHex;
    private final String comment;
    private Map<String, String> otherFormats;

    /**
     * This constructor is for importing from the Element as first argument, taking the inherited protocol name and parameters, given as parameters, into account.
     * @param element of type "command".
     * @param inheritProtocol
     * @param inheritParameters
     * @throws org.harctoolbox.girr.GirrException
     */
    public Command(Element element, String inheritProtocol, Map<String, Long> inheritParameters) throws GirrException {
        this(MasterType.safeValueOf(element.getAttribute(MASTER_ATTRIBUTE_NAME)), element.getAttribute(NAME_ATTRIBUTE_NAME),
                element.getAttribute(COMMENT_ATTRIBUTE_NAME), element.getAttribute(DISPLAYNAME_ATTRIBUTE_NAME),
                XmlStatic.parseElementsByLanguage(element.getElementsByTagName(NOTES_ELEMENT_NAME)));
        if (!element.getTagName().equals(COMMAND_ELEMENT_NAME))
            throw new GirrException("Element is not of type " + COMMAND_ELEMENT_NAME);

        protocolName = inheritProtocol != null ? irpDatabase.expandAlias(inheritProtocol) : null;
        parameters = new LinkedHashMap<>(INITIAL_HASHMAP_CAPACITY);
        if (inheritParameters != null)
            parameters.putAll(inheritParameters);
        otherFormats = new HashMap<>(0);

        try {
            NodeList nl;
            NodeList paramsNodeList = element.getElementsByTagName(PARAMETERS_ELEMENT_NAME);
            if (paramsNodeList.getLength() > 0) {
                Element params = (Element) paramsNodeList.item(0);
                String proto = params.getAttribute(PROTOCOL_ATTRIBUTE_NAME);
                if (!proto.isEmpty())
                    protocolName = irpDatabase.expandAlias(proto);
                nl = params.getElementsByTagName(PARAMETER_ELEMENT_NAME);
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    parameters.put(el.getAttribute(NAME_ATTRIBUTE_NAME), IrCoreUtils.parseLong(el.getAttribute(VALUE_ATTRIBUTE_NAME)));
                }
            }
            String Fstring = element.getAttribute(F_ATTRIBUTE_NAME);
            if (!Fstring.isEmpty())
                parameters.put(F_PARAMETER_NAME, IrCoreUtils.parseLong(Fstring));
            nl = element.getElementsByTagName(RAW_ELEMENT_NAME);
            if (nl.getLength() > 0) {
                intro = new String[nl.getLength()];
                repeat = new String[nl.getLength()];
                ending = new String[nl.getLength()];
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    int T;
                    try {
                        T = Integer.parseInt(el.getAttribute(TOGGLE_ATTRIBUTE_NAME));
                    } catch (NumberFormatException ex) {
                        T = 0;
                    }
                    barfIfInvalidToggle(T, nl.getLength());
                    String freq = el.getAttribute(FREQUENCY_ATTRIBUTE_NAME);
                    if (!freq.isEmpty())
                        frequency = Integer.valueOf(freq);
                    String dc = el.getAttribute(DUTYCYCLE_ATTRIBUTE_NAME);
                    if (!dc.isEmpty()) {
                        dutyCycle = Double.valueOf(dc);
                        if (!ModulatedIrSequence.isValidDutyCycle(dutyCycle))
                            throw new GirrException("Invalid dutyCycle: " + dutyCycle + "; must be between 0 and 1.");
                    }
                    NodeList nodeList = el.getElementsByTagName(INTRO_ELEMENT_NAME);
                    if (nodeList.getLength() > 0)
                        intro[T] = parseSequence((Element) nodeList.item(0));
                    nodeList = el.getElementsByTagName(REPEAT_ELEMENT_NAME);
                    if (nodeList.getLength() > 0)
                        repeat[T] = parseSequence((Element) nodeList.item(0));
                    nodeList = el.getElementsByTagName(ENDING_ELEMENT_NAME);
                    if (nodeList.getLength() > 0)
                        ending[T] = parseSequence((Element) nodeList.item(0));
                }
            }
            nl = element.getElementsByTagName(PRONTO_HEX_ELEMENT_NAME);
            if (nl.getLength() > 0) {
                prontoHex = new String[nl.getLength()];
                for (int i = 0; i < nl.getLength(); i++) {
                    Element el = (Element) nl.item(i);
                    int T;
                    try {
                        T = Integer.parseInt(el.getAttribute(TOGGLE_ATTRIBUTE_NAME));
                    } catch (NumberFormatException ex) {
                        T = 0;
                    }
                    barfIfInvalidToggle(T, nl.getLength());
                    prontoHex[T] = el.getTextContent();
                }
            }
            nl = element.getElementsByTagName(FORMAT_ELEMENT_NAME);
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(0);
                otherFormats.put(el.getAttribute(NAME_ATTRIBUTE_NAME), el.getTextContent());
            }
        } catch (IllegalArgumentException ex) { // contains NumberFormatException
            throw new GirrException(ex);
        }
        sanityCheck();
    }

    /**
     * This constructor is for importing from the Element as first argument.
     * @param element of type "command".
     * @throws org.harctoolbox.girr.GirrException
     */
    public Command(Element element) throws GirrException {
        this(element, null, null);
    }

    /**
     * This constructor is used to read a Girr file into a Command.
     * @param file
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public Command(File file) throws GirrException, IOException, SAXException {
        this(getElement(file));
    }

    /**
     * This constructor is used to read a Reader into a Command.
     * @param reader
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    public Command(Reader reader) throws IOException, SAXException, GirrException {
        this(getElement(reader));
    }

    /**
     * Construct a Command from an IrSignal, i.e.&nbsp;timing data.
     *
     * @param name Name of command
     * @param comment textual comment
     * @param irSignal IrSignal
     */
    public Command(String name, String comment, IrSignal irSignal) {
        this(MasterType.raw, name, comment);
        generateRaw(irSignal);
    }

    /**
     * Construct a Command from protocolName and parameters.
     *
     * @param name
     * @param comment
     * @param protocolName
     * @param parameters
     * @param check If true, throw GirrException if the projectName cannot be made a protocol.
     * @throws org.harctoolbox.girr.GirrException
     */
    public Command(String name, String comment, String protocolName, Map<String, Long> parameters, boolean check) throws GirrException {
        this(name, comment, null, null, protocolName, parameters, check);
    }

    /**
     * Construct a Command from protocolName and parameters.
     *
     * @param name
     * @param comment
     * @param displayName
     * @param notes
     * @param protocolName
     * @param parameters
     * @param check If true, throw GirrException if the projectName cannot be made a protocol.
     * @throws org.harctoolbox.girr.GirrException
     */
    public Command(String name, String comment, String displayName, Map<String, String> notes, String protocolName, Map<String, Long> parameters, boolean check) throws GirrException {
        this(MasterType.parameters, name, comment, displayName, notes);
        if (protocolName == null)
            throw new GirrException("No protocol name");
        this.parameters = new LinkedHashMap<>(parameters);
        String expandedProtocolName = irpDatabase.expandAlias(protocolName);
        try {
            protocol = irpDatabase.getProtocol(expandedProtocolName);
        } catch (IrpException ex) {
            if (check)
                throw new GirrException(ex);
            protocol = null;
        }
        this.protocolName = expandedProtocolName;
        sanityCheck();
    }

    /**
     * Construct a Command from protocolName and parameters.
     *
     * @param name
     * @param comment
     * @param protocolName
     * @param parameters
     * @throws org.harctoolbox.girr.GirrException if the projectName cannot be made a protocol.
     */
    public Command(String name, String comment, String protocolName, Map<String, Long> parameters) throws GirrException {
        this(name, comment, protocolName, parameters, true);
    }

    private Command(MasterType masterType, String name, String comment) {
        this(masterType, name, comment, null, null);
    }

    private Command(MasterType masterType, String name, String comment, String displayName, Map<String, String> notes) {
        this.masterType = masterType;
        this.name = name;
        this.comment = comment;
        this.displayName = displayName;
        this.otherFormats = new HashMap<>(0);
        this.notes = notes != null ? notes : new HashMap<>(0);
        frequency = null;
        dutyCycle = null;
        prontoHex = null;
        intro = null;
        repeat = null;
        ending = null;
    }

    /**
     * Construct a Command from Pronto Hex form.
     *
     * @param name
     * @param comment
     * @param prontoHex
     * @throws org.harctoolbox.girr.GirrException
     */
    public Command(String name, String comment, String prontoHex) throws GirrException {
        this(MasterType.ccf, name, comment);
        this.prontoHex = new String[1];
        this.prontoHex[0] = prontoHex;
        sanityCheck();
    }

    /**
     * Create an empty command with the given name.
     * @param name Name of command.
     */
    public Command(String name) {
        this(MasterType.empty, name, null);
    }

    public Command(String protocolName, Long device, Long subdevice) {
        this(MasterType.empty, DUMMY_COMMAND_NAME, null);
        this.parameters = mkMap(device, subdevice);
        this.protocolName = protocolName;
    }

    /**
     * Generate an empty command with a dummy name.
     */
    public Command() {
        this(DUMMY_COMMAND_NAME);
    }

    /**
     * @return duty cycle, a number between 0 and 1.
     * @throws org.harctoolbox.irp.IrpException
     */
    public double getDutyCycle() throws IrpException {
        if (masterType == MasterType.parameters) {
            checkForProtocol();
            return protocol.getDutyCycle();
        }
        return dutyCycle;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param lang
     * @return the notes
     */
    public String getNotes(String lang) {
        return notes.get(lang);
    }

    /**
     * @return the masterType
     */
    public MasterType getMasterType() {
        return masterType;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return name of the protocol
     * @throws org.harctoolbox.ircore.InvalidArgumentException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.Pronto.NonProntoFormatException
     */
    public String getProtocolName() throws IrpException, IrCoreException {
        checkForParameters();
        return protocolName;
    }

    /**
     * @return the parameters
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public Map<String, Long> getParameters() throws IrpException, IrCoreException {
        checkForParameters();
        return parameters != null ? Collections.unmodifiableMap(parameters) : null;
    }

    /**
     * @return the frequency
     * @throws org.harctoolbox.irp.IrpException
     */
    public double getFrequency() throws IrpException {
        if (masterType == MasterType.parameters) {
            checkForProtocol();
            return protocol.getFrequency();
        }
        return frequency;
    }

    private void checkForProtocol() throws IrpException {
        if (protocol == null)
            protocol = irpDatabase.getProtocol(protocolName);
    }

    private boolean checkIfProtocol() {
        if (protocol != null)
            return true;
        if (protocolName == null || protocolName.isEmpty())
            return false;

        if (irpDatabase.isKnown(protocolName)) {
            try {
                protocol = irpDatabase.getProtocol(protocolName);
            } catch (IrpException ex) {
                throw new ThisCannotHappenException();
            }
        }
        return protocol != null;
    }

    /**
     * Returns the first intro sequence. Equivalent to getIntro(0);
     * @return the intro
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     */
    public String getIntro() throws GirrException, IrCoreException, IrpException {
        return getIntro(0);
    }

    /**
     *
     * @param T toggle value
     * @return intro sequence corresponding to T.
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     */
    public String getIntro(int T) throws GirrException, IrCoreException, IrpException {
        checkForRaw();
        barfIfInvalidToggle(T);
        return intro[T];
    }

    /**
     * Returns the first repeat sequence. Equivalent to getRepeat(0);
     * @return the repeat
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     */
    public String getRepeat() throws GirrException, IrCoreException, IrpException {
        return getRepeat(0);
    }

    /**
     *
     * @param T toggle value
     * @return repeat sequence corresponding to T.
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     */
    public String getRepeat(int T) throws GirrException, IrCoreException, IrpException {
        checkForRaw();
        barfIfInvalidToggle(T);
        return repeat[T];
    }

    /**
     * Returns the first ending sequence. Equivalent to getEnding(0).
     * @return the ending
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public String getEnding() throws GirrException, IrpException, IrCoreException {
        return getEnding(0);
    }

    /**
     *
     * @param T toggle value
     * @return ending sequence corresponding to T.
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public String getEnding(int T) throws GirrException, IrpException, IrCoreException {
        checkForRaw();
        barfIfInvalidToggle(T);
        return ending[T];
    }

    /**
     * Returns the  Pronto Hex version of the first signal. Equivalent to getProntoHex(0).
     * @return Pronto Hex
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public String getProntoHex() throws GirrException, IrpException, IrCoreException {
        return getProntoHex(0);
    }

    /**
     *
     * @param T toggle value
     * @return Pronto Hex corresponding to T
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public String getProntoHex(int T) throws GirrException, IrpException, IrCoreException {
        checkForProntoHex();
        barfIfInvalidToggle(T);
        return prontoHex[T];
    }

    /**
     * Checks that the parameter T is a valid toggle value; throws an exception otherwise.
     * @param T toggle value
     */
    private void barfIfInvalidToggle(Integer T) {
        barfIfInvalidToggle(T, numberOfToggleValues());
    }

    private void barfIfInvalidToggle(Integer T, int noToggles) {
        if (T != null && (T < 0 || T >= noToggles))
            throw new IllegalArgumentException("Illegal value of T = " + T);
    }

    /**
     * @return Collection of the otherFormats
     */
    public Collection<String> getOtherFormats() {
        return otherFormats.keySet();
    }

    /**
     * Returns an "other" format, identified by its name.
     * @param name format name
     * @return test string of the format.
     */
    public String getFormat(String name) {
        return otherFormats != null ? otherFormats.get(name) : null;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the IrSignal of the Command.
     * @return IrSignal
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public IrSignal toIrSignal() throws IrpException, IrCoreException {
        return toIrSignal(null);
    }

    /**
     * Returns the IrSignal of the Command.
     * @param toggle toggle value; use null for unspecified.
     * @return IrSignal corresponding to the Command.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public IrSignal toIrSignal(Integer toggle) throws IrpException, IrCoreException {
        barfIfInvalidToggle(toggle);
        int T = toggle == null ? 0 : toggle;

        switch (masterType) {
            case parameters:
                if (toggle != null) {
                    Map<String, Long> params = new LinkedHashMap<>(parameters);
                    params.put(TOGGLE_PARAMETER_NAME, toggle.longValue());
                    return irpDatabase.render(protocolName, params);
                } else
                    return irpDatabase.render(protocolName, parameters);

            case raw:
                return new IrSignal(intro[T], repeat[T], ending[T], frequency != null ? frequency.doubleValue() : null, dutyCycle);

            case ccf:
                return ShortPronto.parse(prontoHex[T]);

            case empty:
                return null;

            default:
                throw new ThisCannotHappenException();
        }
    }

    // Nice to have: a version that takes a user supplied format string as argument.
    /**
     *
     * @return A "pretty" textual representation of the protocol and the parameters.
     */
    public String nameProtocolParameterString() {
        StringBuilder str = new StringBuilder(name != null ? name : "<no_name>");
        if (protocolName == null) {
            str.append(": <no decode>");
        } else {
            str.append(": ").append(protocolName);
            if (parameters.containsKey(D_PARAMETER_NAME))
                str.append(" Device: ").append(parameters.get(D_PARAMETER_NAME));
            if (parameters.containsKey(S_PARAMETER_NAME))
                str.append(".").append(parameters.get(S_PARAMETER_NAME));
            if (parameters.containsKey(F_PARAMETER_NAME))
                str.append(" Function: ").append(parameters.get(F_PARAMETER_NAME));
            parameters.entrySet().forEach((kvp) -> {
                String parName = kvp.getKey();
                if (!(parName.equals(F_PARAMETER_NAME) || parName.equals(D_PARAMETER_NAME) || parName.equals(S_PARAMETER_NAME)))
                    str.append(SPACE).append(parName).append("=").append(kvp.getValue());
            });
        }

        return str.toString();
    }

    /**
     *
     * @return Nicely formatted string the way the user would like to see it if truncated to "small" length.
     */
    public String prettyValueString() {
        if (masterType == MasterType.empty)
            return "Empty";

        try {
            checkForParameters();
        } catch (IrCoreException | IrpException ex) {
            return "Undecoded signal";
        }

        return (parameters != null && !parameters.isEmpty()) ? protocolName + ", " + toPrintString(parameters)
                : prontoHex != null ? prontoHex[0]
                : "Undecoded signal";
    }

    /**
     *
     * @return Nicely formatted string the way the user would like to see it if truncated to "small" length.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.girr.GirrException
     */
    public String toPrintString() throws IrpException, IrCoreException, GirrException {
        StringBuilder str = new StringBuilder(name != null ? name : "<unnamed>");
        str.append(": ");
        if (parameters != null)
            str.append(protocolName).append(", ").append(toPrintString(parameters));
        else if (prontoHex != null)
            str.append(getProntoHex());
        else
            str.append(toIrSignal().toString(true));

        return str.toString();
    }

    @Override
    public String toString() {
        if (masterType == MasterType.empty)
            return "Empty";
        try {
            return toPrintString();
        } catch (IrpException | IrCoreException | GirrException ex) {
            logger.log(Level.WARNING, null, ex);
            return name + ": (<erroneous signal>)";
        }
    }

    private void sanityCheck() throws GirrException {
        boolean protocolOk = protocolName != null && !protocolName.isEmpty();
        boolean parametersOk = parameters != null /*&& !parameters.isEmpty()*/;
        boolean rawOk = (intro != null && intro[0] != null && !intro[0].isEmpty())
                || (repeat != null && repeat[0] != null && !repeat[0].isEmpty());
        boolean prontoHexOk = prontoHex != null && prontoHex[0] != null && !prontoHex[0].isEmpty();

        if (masterType == null)
            masterType = (protocolOk && parametersOk) ? MasterType.parameters
                    : rawOk ? MasterType.raw
                    : prontoHexOk ? MasterType.ccf
                    : MasterType.empty;

        if (!acceptEmptyCommands && masterType == MasterType.empty)
            throw new GirrException("Command " + name + ": Empty commands not allowed.");

        if (masterType == MasterType.parameters && !protocolOk)
            throw new GirrException("Command " + name + ": MasterType is parameters, but no protocol found.");
        if (masterType == MasterType.parameters && !parametersOk)
            throw new GirrException("Command " + name + ": MasterType is parameters, but no parameters found.");
        if (masterType == MasterType.raw && !rawOk)
            throw new GirrException("Command " + name + ": MasterType is raw, but both intro- and repeat-sequence empty.");
        if (masterType == MasterType.ccf && !prontoHexOk)
            throw new GirrException("Command " + name + ": MasterType is prontoHex, but no Pronto Hex found.");
    }


    private void generateRawProntoHexAllT(boolean generateRaw, boolean generateProntoHex) throws GirrException, DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidNameException, OddSequenceLengthException {
        if (numberOfToggleValues() == 1)
            generateRawProntoHex(generateRaw, generateProntoHex);
        else
            for (int T = 0; T < numberOfToggleValues(); T++)
                generateRawProntoHexForceT(T, generateRaw, generateProntoHex);
    }

    private void generateRawProntoHexForceT(int T, boolean generateRaw, boolean generateProntoHex) throws DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidNameException, OddSequenceLengthException {
        Map<String, Long> params = new LinkedHashMap<>(parameters);
        params.put(TOGGLE_PARAMETER_NAME, (long) T);
        generateRawProntoHex(params, T, generateRaw, generateProntoHex);
    }

    private void generateRawProntoHex(boolean generateRaw, boolean generateProntoHex) throws GirrException, DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidNameException, OddSequenceLengthException {
        if (!checkIfProtocol())
            throw new GirrException("protocol named " + this.protocolName + " not found or erroneous");

        IrSignal irSignal = protocol.toIrSignal(parameters);

        if (generateRaw)
            generateRaw(irSignal);
        if (generateProntoHex)
            generateProntoHex(irSignal);
    }

    private void generateRawProntoHex(Map<String, Long> parameters, int T, boolean generateRaw, boolean generateProntoHex) throws DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidNameException, OddSequenceLengthException {
        IrSignal irSignal = protocol.toIrSignal(parameters);
        if (generateRaw)
            generateRaw(irSignal, T);
        if (generateProntoHex)
            generateProntoHex(irSignal, T);
    }


    private void generateDecode(IrSignal irSignal) {
        if (irSignal == null) {
            notes.put(ENGLISH, "No signal information.");
            return;
        }
        Decoder.AbstractDecodesCollection<? extends ElementaryDecode> decodes = decoder.decodeIrSignalWithFallback(irSignal, decoderParameters);

        if (decodes.isEmpty())
            notes.put(ENGLISH, "Decoding was invoked, but found no decode.");
        else {
            ElementaryDecode firstDecode = decodes.getPreferred() != null ? decodes.getPreferred() : decodes.first();
            protocolName = firstDecode.getName();
            parameters = firstDecode.getMap();
        }
        if (decodes.size() > 1)
            notes.put(ENGLISH, "Several decodes");
    }

    /**
     * Tries to generate the parameter version of the signal (decoding the signals),
     * unless parameters already are present.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public void checkForParameters() throws IrpException, IrCoreException {
        if (parameters == null || parameters.isEmpty())
            generateDecode(toIrSignal());
    }

    /**
     * Tries to generate the raw version of the signal, unless already present.
     */
    private void checkForRaw() throws GirrException, DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidArgumentException, Pronto.NonProntoFormatException, InvalidNameException {
        if ((intro != null) || (repeat != null) || (ending != null))
            return;

        if (masterType == MasterType.parameters)
            generateRawProntoHexAllT(true, false);
        else {
            IrSignal irSignal = Pronto.parse(prontoHex[0]);
            generateRaw(irSignal);
        }
    }

    /**
     * Tries to generate the Pronto Hex version of the signal, unless already present.
     */
    private void checkForProntoHex() throws GirrException, IrpInvalidArgumentException, DomainViolationException, NameUnassignedException, OddSequenceLengthException, InvalidNameException {
        if (prontoHex != null)
            return;

        if (masterType == MasterType.parameters)
            generateRawProntoHexAllT(false, true);
        else {
            IrSignal irSignal;
            irSignal = new IrSignal(intro[0], repeat[0], ending[0], frequency != null ? frequency.doubleValue() : null, dutyCycle);
            generateProntoHex(irSignal);
        }
    }

    private void checkFor(MasterType type) throws GirrException, DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidArgumentException, Pronto.NonProntoFormatException, InvalidNameException, IrpException, IrCoreException {
        switch (type) {
            case parameters:
                checkForParameters();
                break;
            case ccf:
                checkForProntoHex();
                break;
            case raw:
                checkForRaw();
                break;
            default:
        }
    }

    /**
     * Returns the number of possible values of the toggle variable.
     * Must be at least 1.
     * Allowed values for the toggle are 0,...,numberOfToggleValues() - 1.
     * @return the number of possible values.
     */
    public int numberOfToggleValues() {
        if (!checkIfProtocol())
            return 1;

        return (masterType == MasterType.parameters
                && !parameters.containsKey(TOGGLE_PARAMETER_NAME)
                && protocol != null
                && protocol.hasParameter(TOGGLE_PARAMETER_NAME)
                && protocol.hasParameterMemory(TOGGLE_PARAMETER_NAME)) ? ((int) protocol.getParameterMax(TOGGLE_PARAMETER_NAME)) + 1 : 1;
    }

    private void generateRaw(IrSignal irSignal) {
        generateRaw(irSignal,  0);
    }

    private void generateRaw(IrSignal irSignal, int T) {
        barfIfInvalidToggle(T);
        frequency = irSignal.getFrequency() != null ? irSignal.getFrequency().intValue() : null;
        dutyCycle = irSignal.getDutyCycle();
        if (intro == null)
            intro = new String[numberOfToggleValues()];
        intro[T] = irSignal.getIntroSequence().toString(true, SPACE, "", "");
        if (repeat == null)
            repeat = new String[numberOfToggleValues()];
        repeat[T] = irSignal.getRepeatSequence().toString(true, SPACE, "", "");
        if (ending == null)
            ending = new String[numberOfToggleValues()];
        ending[T] = irSignal.getEndingSequence().toString(true, SPACE, "", "");
    }

    private void generateProntoHex(IrSignal irSignal) {
        generateProntoHex(irSignal, 0);
    }

    private void generateProntoHex(IrSignal irSignal, int T) {
        barfIfInvalidToggle(T);
        if (prontoHex == null)
            prontoHex = new String[numberOfToggleValues()];
        prontoHex[T] = Pronto.toString(irSignal);
    }

    /**
     * Add another format.
     * @param name Name of format.
     * @param value Text string for value of format.
     */
    public void addFormat(String name, String value) {
        if (otherFormats == null)
            otherFormats = new HashMap<>(1);
        otherFormats.put(name, value);
    }

    /**
     * Add an extra format to the Command.
     * @param format
     * @param repeatCount
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    public void addFormat(CommandTextFormat format, int repeatCount) throws IrpException, IrCoreException {
        addFormat(format.getName(), format.format(toIrSignal(), repeatCount));
    }

    @Override
    Element toElement(Document doc, boolean fatRaw, boolean generateParameters, boolean generateProntoHex, boolean generateRaw) {
        return toElement(doc, fatRaw, generateParameters, generateProntoHex, generateRaw, null, null);
    }

    /**
     * XMLExport of the Command.
     *
     * @param doc
     * @param fatRaw
     * @param generateParameters
     * @param generateProntoHex
     * @param generateRaw
     * @param inheritedProtocolName
     * @param inheritedParameters
     * @return XML Element with tag name "command".
     */
    @SuppressWarnings("null")
    Element toElement(Document doc, boolean fatRaw,
            boolean generateParameters, boolean generateProntoHex, boolean generateRaw, String inheritedProtocolName, Map<String, Long> inheritedParameters) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, COMMAND_ELEMENT_NAME);
        element.setAttribute(NAME_ATTRIBUTE_NAME, name);
        MasterType actualMasterType = actualMasterType(generateParameters, generateProntoHex, generateRaw);

        if (actualMasterType != null)
            element.setAttribute(MASTER_ATTRIBUTE_NAME, actualMasterType.name());
        if (comment != null && !comment.isEmpty())
            element.setAttribute(COMMENT_ATTRIBUTE_NAME, comment);
        if (displayName != null && !displayName.isEmpty())
            element.setAttribute(DISPLAYNAME_ATTRIBUTE_NAME, this.displayName);

        notes.entrySet().stream().map((note) -> {
            Element notesEl = doc.createElementNS(GIRR_NAMESPACE, NOTES_ELEMENT_NAME);
            notesEl.setAttribute(XML_LANG_ATTRIBUTE_NAME, note.getKey());
            notesEl.setTextContent(note.getValue());
            return notesEl;
        }).forEachOrdered((notesEl) -> {
            element.appendChild(notesEl);
        });

        if (generateParameters || actualMasterType == MasterType.parameters) {
            try {
                checkForParameters();
                if (parameters != null) {
                    if (canReduce(inheritedProtocolName, inheritedParameters)) {
                        Long parameterF = parameters.get(F_PARAMETER_NAME);
                        if (parameterF != null)
                            element.setAttribute(F_ATTRIBUTE_NAME, parameterF.toString());
                    } else {
                        Element parametersEl = doc.createElementNS(GIRR_NAMESPACE, PARAMETERS_ELEMENT_NAME);
                        if (protocolName != null)
                            parametersEl.setAttribute(PROTOCOL_ATTRIBUTE_NAME, protocolName);
                        element.appendChild(parametersEl);
                        for (Map.Entry<String, Long> kvp : parameters.entrySet()) {
                            Element parameterEl = doc.createElementNS(GIRR_NAMESPACE, PARAMETER_ELEMENT_NAME);
                            String parameterName = kvp.getKey();
                            if (inheritedParameters == null || !Objects.equals(kvp.getValue(), inheritedParameters.get(parameterName))) {
                                parameterEl = doc.createElementNS(GIRR_NAMESPACE, PARAMETER_ELEMENT_NAME);
                                parameterEl.setAttribute(NAME_ATTRIBUTE_NAME, parameterName);
                                parameterEl.setAttribute(VALUE_ATTRIBUTE_NAME, kvp.getValue().toString());
                                parametersEl.appendChild(parameterEl);
                            }
                        }
                        if (inheritedParameters != null) {
                            checkForProtocol();
                            for (ParameterSpec p : protocol.getParameterSpecs()) {
                                String parameterName = p.getName();
                                if (parameterName.equals(TOGGLE_PARAMETER_NAME) || parameters.containsKey(parameterName))
                                    continue;
                                Expression deflt = p.getDefault();
                                if (deflt == null)
                                    continue;
                                
                                NameEngine nameEngine = new NameEngine(parameters);
                                try {
                                    Long defaultValue = deflt.toLong(nameEngine);
                                    if (!Objects.equals(defaultValue, inheritedParameters.get(parameterName))) {
                                        Element el = doc.createElementNS(GIRR_NAMESPACE, PARAMETER_ELEMENT_NAME);
                                        el.setAttribute(NAME_ATTRIBUTE_NAME, parameterName);
                                        el.setAttribute(VALUE_ATTRIBUTE_NAME, Long.toString(defaultValue));
                                        parametersEl.appendChild(el);
                                    }
                                } catch (NameUnassignedException ex) {
                                }
                            }
                        }
                    }
                }
            } catch (IrCoreException | IrpException ex) {
                logger.log(Level.INFO, null, ex);
                element.appendChild(doc.createComment("Parameters requested but could not be generated."));
            }
        }
        if (generateRaw || actualMasterType == MasterType.raw) {
            try {
                checkForRaw();
                if (intro != null || repeat != null || ending != null) {
                    for (int T = 0; T < numberOfToggleValues(); T++) {
                        Element rawEl = doc.createElementNS(GIRR_NAMESPACE, RAW_ELEMENT_NAME);
                        rawEl.setAttribute(FREQUENCY_ATTRIBUTE_NAME,
                                Integer.toString(frequency != null ? frequency : (int) ModulatedIrSequence.DEFAULT_FREQUENCY));
                        if (dutyCycle != null && dutyCycle > 0.0)
                            rawEl.setAttribute(DUTYCYCLE_ATTRIBUTE_NAME, Double.toString(dutyCycle));
                        if (numberOfToggleValues() > 1)
                            rawEl.setAttribute(TOGGLE_ATTRIBUTE_NAME, Integer.toString(T));
                        element.appendChild(rawEl);
                        processRaw(doc, rawEl, intro[T], INTRO_ELEMENT_NAME, fatRaw);
                        processRaw(doc, rawEl, repeat[T], REPEAT_ELEMENT_NAME, fatRaw);
                        processRaw(doc, rawEl, ending[T], ENDING_ELEMENT_NAME, fatRaw);
                    }
                }
            } catch (IrCoreException | GirrException | IrpException ex) {
                logger.log(Level.INFO, "{0}", ex.getMessage());
                element.appendChild(doc.createComment("Raw signal requested but could not be generated: " + ex.getMessage() + "."));
            }
        }
        if (generateProntoHex || actualMasterType == MasterType.ccf) {
            try {
                checkForProntoHex();
                if (prontoHex != null) {
                    for (int T = 0; T < numberOfToggleValues(); T++) {
                        Element prontoHexEl = doc.createElementNS(GIRR_NAMESPACE, PRONTO_HEX_ELEMENT_NAME);
                        if (numberOfToggleValues() > 1)
                            prontoHexEl.setAttribute(TOGGLE_ATTRIBUTE_NAME, Integer.toString(T));
                        prontoHexEl.setTextContent(prontoHex[T]);
                        element.appendChild(prontoHexEl);
                    }
                }
            } catch (IrCoreException | IrpException | GirrException ex) {
                logger.log(Level.INFO, "{0}", ex.getMessage());
                element.appendChild(doc.createComment("Pronto Hex requested but could not be generated: " + ex.getMessage() + "."));
            }
        }
        if (otherFormats != null) {
            otherFormats.entrySet().stream().map((format) -> {
                Element formatEl = doc.createElementNS(GIRR_NAMESPACE, FORMAT_ELEMENT_NAME);
                formatEl.setAttribute(NAME_ATTRIBUTE_NAME, format.getKey());
                formatEl.setTextContent(format.getValue());
                return formatEl;
            }).forEachOrdered((formatEl) -> {
                element.appendChild(formatEl);
            });
        }
        return element;
    }

    /**
     * Returns the MasterType that is to be used.
     * @param generateParameters
     * @param generateProntoHex
     * @param generateRaw
     * @return MasterType to be used.
     */
    MasterType actualMasterType(boolean generateParameters, boolean generateProntoHex, boolean generateRaw) {
        MasterType actualMasterType = masterType;
        if (masterType == MasterType.raw && !generateRaw
                || masterType == MasterType.ccf && !generateProntoHex
                || masterType == MasterType.parameters && !generateParameters) {
            actualMasterType = generateRaw ? MasterType.raw
                    : generateParameters ? MasterType.parameters
                    : generateProntoHex ? MasterType.ccf
                    : masterType;
        }
        return actualMasterType;
    }

    private boolean canReduce(String inheritedProtocolName, Map<String, Long> inheritedParameters) throws IrpException {
        if (inheritedParameters == null || ! useInheritanceForXml || ! protocolName.equals(inheritedProtocolName))
            return false;

        for (Map.Entry<String, Long> kvp : parameters.entrySet()) {
            String parameterName = kvp.getKey();
            if (parameterName.equals(F_PARAMETER_NAME))
                continue;
            Long inheritedParameter = inheritedParameters.get(parameterName);
            if (!(inheritedParameter != null && inheritedParameter.equals(kvp.getValue())))
                return false;
        }

        checkForProtocol();
        for (ParameterSpec p : protocol.getParameterSpecs()) {
            String parameterName = p.getName();
            if (parameterName.equals(TOGGLE_PARAMETER_NAME))
                continue;
            Expression deflt = p.getDefault();
            if (deflt == null)
                continue;
            
            Long newValue = parameters.get(parameterName);
            if (newValue != null) // has been checked already
                continue;
       
            NameEngine nameEngine = new NameEngine(parameters);
            try {
                Long defaultValue = deflt.toLong(nameEngine);
                Long inheritedValue = inheritedParameters.get(parameterName);
                if (inheritedValue == null)
                    inheritedValue = deflt.toLong(new NameEngine(inheritedParameters));
                if (!Objects.equals(defaultValue, inheritedValue))
                    return false;
            } catch (NameUnassignedException ex) {
                return false;
                //Logger.getLogger(Command.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    /**
     * Removes the forms different from the one given as argument.
     * @param type
     * @throws IrpException
     * @throws GirrException
     * @throws IrCoreException
     */
    public void strip(MasterType type) throws IrpException, GirrException, IrCoreException {
        if (type != masterType)
            checkFor(type);

        if (type != MasterType.parameters) {
            this.parameters = null;
            this.protocol = null;
            this.protocolName = null;
        }
        if (type != MasterType.ccf) {
            this.prontoHex = null;
        }
        if (type != MasterType.raw) {
            intro = null;
            repeat = null ;
            ending = null;
            frequency = null;
            dutyCycle = null;
        }
        otherFormats = null;
    }

    /**
     * Removes the forms other than the master type of the Command.
     */
    public void strip() {
        try {
            strip(masterType);
        } catch (IrpException | GirrException | IrCoreException ex) {
            logger.log(Level.WARNING, ex.getLocalizedMessage());
        }
    }

    public Command transform(String str) throws InvalidNameException, NameUnassignedException, GirrException {
        List<Assignment> transformations = parseTransformations(str);
        return transform(transformations);
    }

    // Don't use NameEngine to represent a set of transformations, since it is unordered.
    public Command transform(Iterable<Assignment> transformations) throws NameUnassignedException, GirrException {
        Map<String, Long> newParameters = transformParameters(transformations);
        return new Command(name, comment, displayName, notes, protocolName, newParameters, true);
    }

    public Map<String, Long> transformParameters(Iterable<Assignment> transformations) throws NameUnassignedException {
        Map<String, Long> newParameters = new HashMap<>(parameters.size());
        NameEngine nameEngine = new NameEngine(parameters);
        for (Assignment transformation : transformations) {
            long newValue = transformation.toLong(nameEngine);
            newParameters.put(transformation.getName(), newValue);
        }
        HashMap<String, Long> result = new HashMap<>(nameEngine.toMap());
        result.putAll(newParameters);
        return result;
    }

    /**
     * This describes which representation of a Command constitutes the master,
     * from which the other representations are derived.
     * Note that raw and prontoHex cannot have toggles.
     */
    public enum MasterType {
        /** The raw representation is the master. Does not have multiple toggle values. */
        raw,

        /** The Pronto Hex representation is the master. Does not have multiple toggle values. */
        ccf, // note: was "prontoHex" in some versions

        /** The protocol/parameter version is the master. May have multiple Pronto Hex/raw representations if the protocol has a toggle. */
        parameters,

        /** This is an empty signal without data. */
        empty;

        /**
         * Safe version of valueOf. Returns null for unrecognized arguments.
         * @param s
         * @return
         */
        public static MasterType safeValueOf(String s) {
            try {
                // Some versions have been using "prontoHex" for ccf; treat this case separately
                if (s.equals("prontoHex"))
                    return MasterType.ccf;
                return valueOf(s);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    /**
     * An implementation of this interface describes a way to format an IrSignal to a text string.
     */
    public interface CommandTextFormat {
        /**
         *
         * @return Name of the format (not the signal).
         */
        public String getName();

        /**
         * Formats an IrSignal with repeatCount number of repetitions.
         * @param irSignal IrSignal to be formatted
         * @param repeatCount Number of repeat sequences to include.
         * @return string of formatted signal.
         */
        public String format(IrSignal irSignal, int repeatCount);
    }
}
