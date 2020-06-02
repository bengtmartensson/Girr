/*
 Copyright (C) 2013, 2014, 2015, 2018 Bengt Martensson.

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
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.harctoolbox.girr.XmlExporter.COMMAND_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.DUTYCYCLE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.ENDING_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.FLASH_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.FORMAT_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.FREQUENCY_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.F_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.GAP_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.GIRR_NAMESPACE;
import static org.harctoolbox.girr.XmlExporter.INTRO_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.MASTER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.NOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.PARAMETERS_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.PARAMETER_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.PRONTO_HEX_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.PROTOCOL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.PROTOCOL_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.RAW_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.REPEAT_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlExporter.SPACE;
import static org.harctoolbox.girr.XmlExporter.TITLE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.TOGGLE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlExporter.VALUE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.ThisCannotHappenException;
import static org.harctoolbox.ircore.XmlUtils.ENGLISH;
import static org.harctoolbox.ircore.XmlUtils.XML_LANG_ATTRIBUTE_NAME;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.DomainViolationException;
import org.harctoolbox.irp.ElementaryDecode;
import org.harctoolbox.irp.InvalidNameException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpInvalidArgumentException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.NameUnassignedException;
import org.harctoolbox.irp.Protocol;
import org.harctoolbox.irp.ShortPronto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class models the command in Girr. A command is essentially an IR signal,
 * given either by protocol/parameters or timing data, and a name.
 * <p>
 * Some protocols have toggles, a persistant variable that changes between invocations.
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
public final class Command {

    private final static Logger logger = Logger.getLogger(Command.class.getName());

    /** Name of the parameter containing the toggle in the IRP protocol. */
    private static final String TOGGLE_PARAMETER_NAME = "T";
    private static final String F_PARAMETER_NAME      = "F";
    private static final String D_PARAMETER_NAME      = "D";
    private static final String S_PARAMETER_NAME      = "S";

    private static IrpDatabase irpDatabase = null;
    private static Decoder decoder = null;
    private static Decoder.DecoderParameters decoderParameters = new Decoder.DecoderParameters();

    static {
        try {
            IrpDatabase database = new IrpDatabase((String) null);
            decoder = new Decoder(database);
            irpDatabase = database;
        } catch (IOException | IrpParseException ex) {
            throw new ThisCannotHappenException(ex);
        }
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
     */
    public static void setIrpDatabase(String irpProtocolsIniPath) throws IOException, IrpParseException {
        setIrpDatabase(new IrpDatabase(irpProtocolsIniPath));
    }

    private static String toPrintString(Map<String, Long> map) {
        if (map == null || map.isEmpty())
            return "";
        StringBuilder str = new StringBuilder(64);
        map.entrySet().forEach((kvp) -> {
            str.append(kvp.getKey()).append("=").append(Long.toString(kvp.getValue())).append(SPACE);
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
            StringBuilder str = new StringBuilder(64);
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

    private static Protocol getProtocolOrNull(IrpDatabase irpDatabase, String protocolName) {
        try {
            return irpDatabase.getProtocol(protocolName);
        } catch (IrpException ex) {
            return null;
        }
    }

    private Protocol protocol;
    private MasterType masterType;
    private Map<String, String> notes;
    private String name;
    private String protocolName; // should always be lowercase
    private Map<String, Long> parameters;
    private Integer frequency;
    private Double dutyCycle;
    private String[] intro;
    private String[] repeat;
    private String[] ending;
    private String[] prontoHex;
    private String comment;
    private Map<String, String> otherFormats;

    /**
     * This constructor is for importing from the Element as first argument.
     * @param element
     * @param inheritProtocol
     * @param inheritParameters
     * @throws org.harctoolbox.girr.GirrException
     */
    public Command(Element element, String inheritProtocol, Map<String, Long> inheritParameters) throws GirrException {
        this(MasterType.safeValueOf(element.getAttribute(MASTER_ATTRIBUTE_NAME)), element.getAttribute(NAME_ATTRIBUTE_NAME), element.getAttribute(COMMENT_ATTRIBUTE_NAME));
        protocolName = inheritProtocol != null ? inheritProtocol.toLowerCase(Locale.US) : null;
        parameters = new HashMap<>(4);
        parameters.putAll(inheritParameters);
        otherFormats = new HashMap<>(0);
        notes = XmlExporter.parseElementsByLanguage(element.getElementsByTagName(NOTES_ELEMENT_NAME));

        try {
            NodeList nl;
            NodeList paramsNodeList = element.getElementsByTagName(PARAMETERS_ELEMENT_NAME);
            if (paramsNodeList.getLength() > 0) {
                Element params = (Element) paramsNodeList.item(0);
                String proto = params.getAttribute(PROTOCOL_ELEMENT_NAME);
                if (!proto.isEmpty())
                    this.protocolName = proto.toLowerCase(Locale.US);
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
                        frequency = Integer.parseInt(freq);
                    String dc = el.getAttribute(DUTYCYCLE_ATTRIBUTE_NAME);
                    if (!dc.isEmpty()) {
                        dutyCycle = Double.parseDouble(dc);
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
     * @throws org.harctoolbox.girr.GirrException
     */
    @SuppressWarnings("unchecked")
    public Command(String name, String comment, String protocolName, Map<String, Long> parameters) throws GirrException {
        this(MasterType.parameters, name, comment);
        this.parameters = new HashMap<>(parameters);
        try {
            this.protocol = irpDatabase.getProtocol(protocolName);
        } catch (IrpException ex) {
            throw new GirrException(ex);
        }
        this.protocolName = protocolName.toLowerCase(Locale.US);
        sanityCheck();
    }

    @SuppressWarnings("unchecked")
    private Command(String name, String comment, String protocolName, Protocol protocol, Map<String, Long> parameters) throws GirrException {
        this(MasterType.parameters, name, comment);
        this.parameters = new HashMap<>(parameters);
        this.protocolName = protocolName.toLowerCase(Locale.US);
        this.protocol = protocol;
        sanityCheck();
    }

    private Command(MasterType masterType, String name, String comment) {
        this.masterType = masterType;
        this.name = name;
        this.comment = comment;
        this.otherFormats = new HashMap<>(0);
        this.notes = new HashMap<>(0);
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
     * Create an empty command.
     * @param name Name of command.
     */
    public Command(String name) {
        this(MasterType.empty, name, null);
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
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<String, Long> getParameters() throws IrpException, IrCoreException {
        checkForParameters();
        return parameters;
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
                    Map<String, Long> params = new HashMap<>(parameters);
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
            return "no information present";

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
            return "no information present";
        try {
            return toPrintString();
        } catch (IrpException | IrCoreException | GirrException ex) {
            logger.log(Level.WARNING, null, ex);
            return name + ": (<erroneous signal>)";
        }
    }

    private void sanityCheck() throws GirrException {
        boolean protocolOk = protocolName != null && !protocolName.isEmpty();
        boolean parametersOk = parameters != null && !parameters.isEmpty();
        boolean rawOk = (intro != null && intro[0] != null && !intro[0].isEmpty())
                || (repeat != null && repeat[0] != null && !repeat[0].isEmpty());
        boolean prontoHexOk = prontoHex != null && prontoHex[0] != null && !prontoHex[0].isEmpty();

        if (masterType == null)
            masterType = (protocolOk && parametersOk) ? MasterType.parameters
                    : rawOk ? MasterType.raw
                    : prontoHexOk ? MasterType.ccf
                    : null;

        if (masterType == null)
            throw new GirrException("Command " + name + ": No usable data or parameters.");

        if (masterType == MasterType.parameters && !protocolOk)
            throw new GirrException("Command " + name + ": MasterType is parameters, but no protocol found.");
        if (masterType == MasterType.parameters && !parametersOk)
            throw new GirrException("Command " + name + ": MasterType is parameters, but no parameters found.");
        if (masterType == MasterType.raw && !rawOk)
            throw new GirrException("Command " + name + ": MasterType is raw, but both intro- and repeat-sequence empty.");
        if (masterType == MasterType.ccf && !prontoHexOk)
            throw new GirrException("Command " + name + ": MasterType is prontoHex, but no Pronto Hex found.");
    }


    private void generateRawProntoHexAllT(Map<String, Long> parameters, boolean generateRaw, boolean generateProntoHex) throws GirrException, DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidNameException, OddSequenceLengthException {
        if (numberOfToggleValues() == 1)
            generateRawProntoHex(parameters, generateRaw, generateProntoHex);
        else
            for (int T = 0; T < numberOfToggleValues(); T++)
                generateRawProntoHexForceT(parameters, T, generateRaw, generateProntoHex);
    }

    private void generateRawProntoHexForceT(Map<String, Long> parameter, int T, boolean generateRaw, boolean generateProntoHex) throws DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidNameException, OddSequenceLengthException {
        @SuppressWarnings("unchecked")
        Map<String, Long> params = new HashMap<>(parameters);
        params.put(TOGGLE_PARAMETER_NAME, (long) T);
        generateRawProntoHex(params, T, generateRaw, generateProntoHex);
    }

    private void generateRawProntoHex(Map<String, Long> parameter, boolean generateRaw, boolean generateProntoHex) throws GirrException, DomainViolationException, NameUnassignedException, IrpInvalidArgumentException, InvalidNameException, OddSequenceLengthException {
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
        Decoder.AbstractDecodesCollection<? extends ElementaryDecode> decodes = decoder.decodeLoose(irSignal, decoderParameters);

        if (decodes.isEmpty())
            notes.put(ENGLISH, "Decoding was invoked, but found no decode.");
        else {
            ElementaryDecode firstDecode = decodes.first();
            protocolName = firstDecode.getName().toLowerCase(Locale.US);
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
            generateRawProntoHexAllT(parameters, true, false);
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
            generateRawProntoHexAllT(parameters, false, true);
        else {
            IrSignal irSignal;
            irSignal = new IrSignal(intro[0], repeat[0], ending[0], frequency != null ? frequency.doubleValue() : null, dutyCycle);
            generateProntoHex(irSignal);
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

    /**
     * XMLExport of the Command.
     *
     * @param doc
     * @param title
     * @param fatRaw
     * @param generateRaw
     * @param generateProntoHex
     * @param generateParameters
     * @return XML Element with tag name "command".
     */
    public Element toElement(Document doc, String title, boolean fatRaw,
            boolean generateRaw, boolean generateProntoHex, boolean generateParameters) {
        Element element = doc.createElementNS(GIRR_NAMESPACE, COMMAND_ELEMENT_NAME);
        if (title != null)
            element.setAttribute(TITLE_ATTRIBUTE_NAME, title);
        element.setAttribute(NAME_ATTRIBUTE_NAME, name);
        MasterType actualMasterType = masterType;
        if (masterType == MasterType.raw && !generateRaw
                || masterType == MasterType.ccf && !generateProntoHex
                || masterType == MasterType.parameters && !generateParameters) {
            actualMasterType = generateRaw ? MasterType.raw
                    : generateParameters ? MasterType.parameters
                    : generateProntoHex ? MasterType.ccf
                    : null;
        }
        if (actualMasterType != null)
            element.setAttribute(MASTER_ATTRIBUTE_NAME, actualMasterType.name());
        if (comment != null && !comment.isEmpty())
            element.setAttribute(COMMENT_ATTRIBUTE_NAME, comment);

        notes.entrySet().stream().map((note) -> {
            Element notesEl = doc.createElementNS(GIRR_NAMESPACE, NOTES_ELEMENT_NAME);
            notesEl.setAttribute(XML_LANG_ATTRIBUTE_NAME, note.getKey());
            notesEl.setTextContent(note.getValue());
            return notesEl;
        }).forEachOrdered((notesEl) -> {
            element.appendChild(notesEl);
        });

        if (generateParameters) {
            try {
                checkForParameters();
                if (parameters != null) {
                    Element parametersEl = doc.createElementNS(GIRR_NAMESPACE, PARAMETERS_ELEMENT_NAME);
                    if (protocolName != null)
                        parametersEl.setAttribute(PROTOCOL_ATTRIBUTE_NAME, protocolName);
                    element.appendChild(parametersEl);
                    parameters.entrySet().stream().map((parameter) -> {
                        Element parameterEl = doc.createElementNS(GIRR_NAMESPACE, PARAMETER_ELEMENT_NAME);
                        parameterEl.setAttribute(NAME_ATTRIBUTE_NAME, parameter.getKey());
                        parameterEl.setAttribute(VALUE_ATTRIBUTE_NAME, parameter.getValue().toString());
                        return parameterEl;
                    }).forEachOrdered((parameterEl) -> {
                        parametersEl.appendChild(parameterEl);
                    });
                }
            } catch (IrCoreException | IrpException ex) {
                logger.log(Level.INFO, null, ex);
                element.appendChild(doc.createComment("Parameters requested but could not be generated."));
            }
        }
        if (generateRaw) {
            try {
                checkForRaw();
                if (intro != null || repeat != null || ending != null) {
                    for (int T = 0; T < numberOfToggleValues(); T++) {
                        Element rawEl = doc.createElementNS(GIRR_NAMESPACE, RAW_ELEMENT_NAME);
                        rawEl.setAttribute(FREQUENCY_ATTRIBUTE_NAME,
                                Integer.toString(frequency != null ? frequency : (int) ModulatedIrSequence.DEFAULT_FREQUENCY));
                        if (dutyCycle != null)
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
                logger.log(Level.INFO, null, ex);
                element.appendChild(doc.createComment("Raw signal requested but could not be generated. (" + ex.getMessage() + ".)"));
            }
        }
        if (generateProntoHex) {
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
                logger.log(Level.INFO, null, ex);
                element.appendChild(doc.createComment("Pronto Hex requested but could not be generated. (" + ex.getMessage() + ".)"));
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

    public static class CompareNameCaseSensitive implements Comparator<Command>, Serializable {
        @Override
        public int compare(Command o1, Command o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class CompareNameCaseInsensitive implements Comparator<Command>, Serializable {
        @Override
        public int compare(Command o1, Command o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
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
        ccf, // to be compatible with the Schema

        /** The protocol/parameter version is the master. May have multiple Pronto Hex/raw representations if the protocol has a toggle. */
        parameters,

        /** This is a dummy signal, without data. */
        empty;

        /**
         * Safe version of valueOf. Returns null for unrecognized arguments.
         * @param s
         * @return
         */
        public static MasterType safeValueOf(String s) {
            try {
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
