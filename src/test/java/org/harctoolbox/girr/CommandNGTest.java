package org.harctoolbox.girr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.xml.XmlUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CommandNGTest {
    private static final String NEC1_12_34_56_CCF = "0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C";
    private static final String IRP_PROTOCOLS_PATH = "src/test/resources/IrpProtocols.xml";
    private static final String NEC1_12_34_56_INTRO = "+9024 -4512 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -1692 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -44268";
    private static final String NEC1_REPEAT = "+9024 -2256 +564 -96156";
    private static final String RC5_12_34_0 = "+889 -889 +1778 -889 +889 -1778 +889 -889 +1778 -889 +889 -1778 +1778 -889 +889 -889 +889 -1778 +1778 -90886";
    private static final String RC5_12_34_1 = "+889 -889 +889 -889 +1778 -1778 +889 -889 +1778 -889 +889 -1778 +1778 -889 +889 -889 +889 -1778 +1778 -90886";
    private static final String RC5_12_34_0_CCF = "0000 0073 0000 000A 0020 0020 0040 0020 0020 0040 0020 0020 0040 0020 0020 0040 0040 0020 0020 0020 0020 0040 0040 0CC8";
    private static final String RC5_12_34_1_CCF = "0000 0073 0000 000A 0020 0020 0020 0020 0040 0040 0020 0020 0040 0020 0020 0040 0040 0020 0020 0020 0020 0040 0040 0CC8";
    private static final String XMP = "0000 006D 0012 0012 0008 0027 0008 003C 0008 0022 0008 006A 0008 0032 0008 0032 0008 001D 0008 001D 0008 020C 0008 0027 0008 0060 0008 001D 0008 0022 0008 001D 0008 001D 0008 001D 0008 001D 0008 0BEF 0008 0027 0008 003C 0008 0022 0008 006A 0008 0032 0008 0032 0008 001D 0008 001D 0008 020C 0008 0027 0008 0037 0008 0046 0008 0022 0008 001D 0008 001D 0008 001D 0008 001D 0008 0BEF";
    private static final String REFERENCE_DIR = "src/test/reference";

    public static final File OUTDIR = new File("out");

    public static void assertOutDirExists() {
        if (!OUTDIR.isDirectory())
            OUTDIR.mkdirs();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        assertOutDirExists();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    // This tries to compensate for differences in different DOM renderers javax.xml.transform.Transformer,
    // taking a DOM tree to plain text.
    // It is just "good enought to ge the job done", and is not a generic comparision engine
    public static void assertFileEqualContent(File testFile, File referenceFile) throws IOException {
        try (BufferedReader testReader = new BufferedReader(new InputStreamReader(new FileInputStream(testFile), XmlUtils.DEFAULT_CHARSETNAME));
                BufferedReader refReader = new BufferedReader(new InputStreamReader(new FileInputStream(referenceFile), XmlUtils.DEFAULT_CHARSETNAME))) {
            StringBuilder testLine = new StringBuilder(256);
            StringBuilder refLine = new StringBuilder(256);

            while (true) {
                load(testLine, testReader);
                load(refLine, refReader);
                if (testLine.length() == 0 && refLine.length() == 0)
                    return;
                if (testLine.length() == 0 || refLine.length() == 0)
                    fail();

                int compareLength = Math.min(testLine.length(), refLine.length());
                assertEquals(testLine.substring(0, compareLength), refLine.substring(0, compareLength));
                testLine.delete(0, compareLength);
                refLine.delete(0, compareLength);
            }
        }
    }

    private static void load(StringBuilder stringBuilder, BufferedReader reader) throws IOException {
        if (stringBuilder.length() > 0)
            return;

        String line;
        do {
            line = reader.readLine();
            if (line == null) {
                stringBuilder.setLength(0);
                return;
            } else
                line = line.trim();
        } while (line.isEmpty());
        stringBuilder.append(line);
    }

    public static void assertFileEqualContent(File file) throws IOException {
        String filename = file.getName();
        assertFileEqualContent(file, new File(REFERENCE_DIR, filename));
    }

    //private final Command nec1_12_34_56_ccf;
    private final Command nec1_12_34_56_param;
    private final Command nec1_12_34_56_irSignal;
    private final Map<String, Long> nec1Params;
    private final Command rc5_12_34;
    private final Map<String, Long> rc5Params;
    private final IrSignal irSignal;
    private final IrpDatabase irpDatabase;

    public CommandNGTest() throws IOException, IrCoreException, GirrException, IrpParseException, SAXException {
        irpDatabase = new IrpDatabase(IRP_PROTOCOLS_PATH);
        Command.setIrpDatabase(IRP_PROTOCOLS_PATH);
        //nec1_12_34_56_ccf = new Command("nec1_12_34_56_ccf", "Test ccf", NEC1_12_34_56_CCF);
        nec1Params = new LinkedHashMap<>(5); // Use LinkedHashMap to get reproducible and well defined test results
        nec1Params.put("D", 12L);
        nec1Params.put("S", 34L);
        nec1Params.put("F", 56L);
        nec1_12_34_56_param = new Command("nec1_12_34_56_param", "Parametrized signal", "nec1", nec1Params);
        irSignal = new IrSignal(NEC1_12_34_56_INTRO, NEC1_REPEAT, null, 38321.0, 0.42);
        nec1_12_34_56_irSignal = new Command("nec1_12_34_56_irSignal", "Command from IrSignal", irSignal);
        rc5Params = new LinkedHashMap<>(3); // Use LinkedHashMap to get reproducible and well defined test results
        rc5Params.put("D", 12L);
        rc5Params.put("F", 34L);
        rc5_12_34 = new Command("rc5_12_34", "An RC5 signal", "rc5", rc5Params);
    }


    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of setIrpSetDatabase method, of class Command.
     * @throws java.io.IOException
     * @throws org.harctoolbox.irp.IrpParseException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testSetIrpDatabase_String() throws IOException, IrpParseException, SAXException {
        System.out.println("setIrpDatabase");
        Command.setIrpDatabase(IRP_PROTOCOLS_PATH);
    }

    /**
     * Test of concatenateAsSequence method, of class Command.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testConcatenateAsSequence() throws IrpException, IrCoreException {
        System.out.println("concatenateAsSequence");
        Collection<Command> commands = new ArrayList<>(0);
        ModulatedIrSequence expResult = new ModulatedIrSequence();
        ModulatedIrSequence result = Command.concatenateAsSequence(commands);
        assertTrue(result.approximatelyEquals(expResult));
    }

    /**
     * Test of getDutyCycle method, of class Command.
     * @throws org.harctoolbox.irp.IrpException
     */
    @Test
    public void testGetDutyCycle() throws IrpException {
        System.out.println("getDutyCycle");
        double expResult = 0.42;
        double result = nec1_12_34_56_irSignal.getDutyCycle();
        assertEquals(result, expResult, 0.0001);
    }

    /**
     * Test of getComment method, of class Command.
     */
    @Test
    public void testGetComment() {
        System.out.println("getComment");
        String result = nec1_12_34_56_irSignal.getComment();
        assertEquals(result, "Command from IrSignal");
    }

    /**
     * Test of getNotes method, of class Command.
     */
    @Test
    public void testGetNotes() {
        System.out.println("getNotes");
        String result = nec1_12_34_56_irSignal.getNotes(XmlUtils.ENGLISH);
        assertEquals(result, null);
    }

    /**
     * Test of getMasterType method, of class Command.
     */
    @Test
    public void testGetMasterType() {
        System.out.println("getMasterType");
        Command.MasterType result = nec1_12_34_56_irSignal.getMasterType();
        assertEquals(result, Command.MasterType.raw);
    }

    /**
     * Test of getName method, of class Command.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        String result = nec1_12_34_56_irSignal.getName();
        assertEquals(result, "nec1_12_34_56_irSignal");
    }

    /**
     * Test of getProtocolName method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     */
    @Test
    public void testGetProtocolName() throws IrCoreException, IrpException {
        System.out.println("getProtocolName");
        String result = nec1_12_34_56_irSignal.getProtocolName();
        assertEquals(result, "NEC1");
    }

    /**
     * Test of getParameters method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     */
    @Test
    public void testGetParameters() throws IrCoreException, IrpException {
        System.out.println("getParameters");
        Map<String, Long> result = nec1_12_34_56_irSignal.getParameters();
        assertEquals(result, nec1Params);
    }

    /**
     * Test of getFrequency method, of class Command.
     * @throws org.harctoolbox.irp.IrpException
     */
    @Test
    public void testGetFrequency() throws IrpException {
        System.out.println("getFrequency");
        double expResult = 38321f;
        double result = nec1_12_34_56_irSignal.getFrequency();
        assertEquals(result, expResult, 0.0);
    }

    /**
     * Test of getIntro method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetIntro_0args() throws IrCoreException, IrpException, GirrException {
        System.out.println("getIntro");
        String expResult = NEC1_12_34_56_INTRO;
        String result = nec1_12_34_56_irSignal.getIntro();
        assertEquals(result, expResult);
    }

    /**
     * Test of getIntro method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetIntro_int() throws IrCoreException, IrpException, GirrException {
        System.out.println("getIntro");
        int T = 0;
        String expResult = NEC1_12_34_56_INTRO;
        String result = nec1_12_34_56_irSignal.getIntro(T);
        assertEquals(result, expResult);
    }

    /**
     * Test of getRepeat method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetRepeat_0args() throws IrCoreException, IrpException, GirrException {
        System.out.println("getRepeat");
        String expResult = NEC1_REPEAT;
        String result = nec1_12_34_56_irSignal.getRepeat();
        assertEquals(result, expResult);
    }

    /**
     * Test of getRepeat method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetRepeat_int() throws IrCoreException, IrpException, GirrException {
        System.out.println("getRepeat");
        int T = 0;
        String expResult = RC5_12_34_0;
        String result = rc5_12_34.getRepeat(T);
        assertEquals(result, expResult);
        T = 1;
        expResult = RC5_12_34_1;
        result = rc5_12_34.getRepeat(T);
        assertEquals(result, expResult);

    }

    /**
     * Test of getEnding method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetEnding_0args() throws IrCoreException, IrpException, GirrException {
        System.out.println("getEnding");
        String expResult = "";
        String result = rc5_12_34.getEnding();
        assertEquals(result, expResult);
    }

    /**
     * Test of getEnding method, of class Command.
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetEnding_int() throws IrCoreException, IrpException, GirrException {
        System.out.println("getEnding");
        int T = 0;
        String expResult = "";
        String result = rc5_12_34.getEnding(T);
        assertEquals(result, expResult);
    }

    /**
     * Test of getProntoHex method, of class Command.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetProntoHex_0args() throws IrpException, IrCoreException, GirrException {
        System.out.println("getProntoHex");
        String expResult = NEC1_12_34_56_CCF;
        String result = nec1_12_34_56_param.getProntoHex();
        assertEquals(result, expResult);
    }

    /**
     * Test of getProntoHex method, of class Command.
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testGetProntoHex_error() throws GirrException  {
        System.out.println("getProntoHex_error");
        try {
            new Command("name", null, "nonexisting-protocol", nec1Params);
            fail();
        } catch (GirrException ex) {
        }
    }

    /**
     * Test of getProntoHex method, of class Command.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testGetPronoHex_int() throws IrpException, IrCoreException, GirrException {
        System.out.println("getProntoHex");
        int T = 0;
        String expResult = RC5_12_34_0_CCF;
        String result = rc5_12_34.getProntoHex(T);
        assertEquals(result, expResult);
        T = 1;
        expResult = RC5_12_34_1_CCF;
        result = rc5_12_34.getProntoHex(T);
        assertEquals(result, expResult);
    }

    /**
     * Test of getOtherFormats method, of class Command.
     */
    @Test
    public void testGetOtherFormats() {
        System.out.println("getOtherFormats");
        Collection<String> expResult = new HashSet<>(0);
        Collection<String> result = rc5_12_34.getOtherFormats();
        assertEquals(result, expResult);
    }

    /**
     * Test of getFormat method, of class Command.
     */
    @Test
    public void testGetFormat() {
        System.out.println("getFormat");
        String name = "covfefe";
        String result = nec1_12_34_56_param.getFormat(name);
        assertEquals(result, null);
    }

    /**
     * Test of toIrSignal method, of class Command.
     * @throws java.io.FileNotFoundException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testToIrSignal_0args() throws FileNotFoundException, IrpException, IrCoreException {
        System.out.println("toIrSignal");
        IrSignal expResult = irpDatabase.render("NEC1", nec1Params);
        IrSignal result = nec1_12_34_56_param.toIrSignal();
        assertTrue(result.approximatelyEquals(expResult)); // TODO: IrSignal should probably implement equals(Object)
    }

    /**
     * Test of toIrSignal method, of class Command.
     * @throws java.io.FileNotFoundException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testToIrSignal_int() throws FileNotFoundException, IrpException, IrCoreException {
        System.out.println("toIrSignal");
        int T = 0;
        Map<String, Long> params = new HashMap<>(3);
        params.put("D", 12L);
        params.put("F", 34L);
        params.put("T", (long) T);
        IrSignal expResult = irpDatabase.render("RC5", params);
        IrSignal result = rc5_12_34.toIrSignal(T);
        assertTrue(result.approximatelyEquals(expResult));
        T = 1;
        params.put("T", (long) T);
        expResult = irpDatabase.render("RC5", params);
        result = rc5_12_34.toIrSignal(T);
        assertTrue(result.approximatelyEquals(expResult));
    }

    /**
     * Test of nameProtocolParameterString method, of class Command.
     */
    @Test
    public void testNameProtocolParameterString() {
        System.out.println("nameProtocolParameterString");
        String expResult = "nec1_12_34_56_param: nec1 Device: 12.34 Function: 56";
        String result = nec1_12_34_56_param.nameProtocolParameterString();
        assertEquals(result, expResult);
    }

    /**
     * Test of prettyValueString method, of class Command.
     */
    @Test
    public void testPrettyValueString() {
        System.out.println("prettyValueString");
        String expResult = "nec1, D=12 S=34 F=56";
        String result = nec1_12_34_56_param.prettyValueString();
        assertEquals(result, expResult);
    }

    /**
     * Test of toPrintString method, of class Command.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     * @throws org.harctoolbox.girr.GirrException
     */
    @Test
    public void testToPrintString() throws IrpException, IrCoreException, GirrException {
        System.out.println("toPrintString");
        String expResult = "nec1_12_34_56_param: nec1, D=12 S=34 F=56";
        String result = nec1_12_34_56_param.toPrintString();
        assertEquals(result, expResult);
    }

    /**
     * Test of toString method, of class Command.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        String expResult = "nec1_12_34_56_param: nec1, D=12 S=34 F=56";
        String result = nec1_12_34_56_param.toString();
        assertEquals(result, expResult);
    }

    /**
     * Test of numberOfToggleValues method, of class Command.
     */
    @Test
    public void testNumberOfToggleValues() {
        System.out.println("numberOfToggleValues");
        int expResult = 1;
        int result = nec1_12_34_56_param.numberOfToggleValues();
        assertEquals(result, expResult);
        expResult = 2;
        result = rc5_12_34.numberOfToggleValues();
        assertEquals(result, expResult);
    }

    /**
     * Test of addFormat method, of class Command.
     */
    @Test
    public void testAddFormat_String_String() {
        System.out.println("addFormat");
        String name = "xxx";
        String value = "yyy";
        Command cmd = new Command("covfefe");
        cmd.addFormat(name, value);
        assertEquals(cmd.getFormat(name), value);
    }

    /**
     * Test of Command(String, String, String)
     *
     * @throws org.harctoolbox.girr.GirrException
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testCommandPronto() throws GirrException, IrpException, IrCoreException {
        System.out.println("CommandPronto");
        Decoder.setDebugProtocolRegExp("xmp-1");
        Command xmp1Command = new Command("xyz", "covfefe", XMP);
        String protocolName = xmp1Command.getProtocolName();
        assertTrue(protocolName.toLowerCase(Locale.US).startsWith("xmp"));

        // https://github.com/bengtmartensson/Girr/issues/12
        Decoder.DecoderParameters params = new Decoder.DecoderParameters();
        params.setRelativeTolerance(0.3);
        params.setOverride(true);
        Command.setDecoderParameters(params);
        xmp1Command = new Command("xyz", "covfefe", XMP);
        protocolName = xmp1Command.getProtocolName();
        assertNull(protocolName);

        params.setOverride(false);
        xmp1Command = new Command("xyz", "covfefe", XMP);
        protocolName = xmp1Command.getProtocolName();
        assertTrue(protocolName.toLowerCase(Locale.US).startsWith("xmp"));
    }

    /**
     * Test of toElement method, of class Command.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testToElement() throws GirrException, IOException, SAXException {
        System.out.println("toElement");
        File file = new File(OUTDIR, "command.girr");
        Command cmd = new Command(new File("src/test/girr/topping-command.girr"));
        cmd.print(file);
        assertFileEqualContent(file);
    }
}
