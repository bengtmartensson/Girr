package org.harctoolbox.girr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CommandNGTest {
    private static final String NEC1_12_34_56_CCF = "0000 006C 0022 0002 015B 00AD 0016 0016 0016 0016 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0041 0016 0041 0016 0041 0016 0016 0016 0016 0016 0016 0016 0041 0016 0041 0016 06A4 015B 0057 0016 0E6C";
    private static final String IRP_PROTOCOLS_PATH = "../IrpMaster/src/main/resources/IrpProtocols.ini";
    private static final String NEC1_12_34_56_INTRO = "+9024 -4512 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -1692 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -1692 +564 -564 +564 -564 +564 -564 +564 -1692 +564 -1692 +564 -44268";
    private static final String NEC1_REPEAT = "+9024 -2256 +564 -96156";
    private static final String RC5_12_34_0 = "+889 -889 +1778 -889 +889 -1778 +889 -889 +1778 -889 +889 -1778 +1778 -889 +889 -889 +889 -1778 +1778 -90886";
    private static final String RC5_12_34_1 = "+889 -889 +889 -889 +1778 -1778 +889 -889 +1778 -889 +889 -1778 +1778 -889 +889 -889 +889 -1778 +1778 -90886";
    private static final String RC5_12_34_0_CCF = "0000 0073 0000 000A 0020 0020 0040 0020 0020 0040 0020 0020 0040 0020 0020 0040 0040 0020 0020 0020 0020 0040 0040 0CC8";
    private static final String RC5_12_34_1_CCF = "0000 0073 0000 000A 0020 0020 0020 0020 0040 0040 0020 0020 0040 0020 0020 0040 0040 0020 0020 0020 0020 0040 0040 0CC8";

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    //private final Command nec1_12_34_56_ccf;
    private final Command nec1_12_34_56_param;
    private final Command nec1_12_34_56_irSignal;
    private final Map<String, Long> nec1Params;
    private final Command rc5_12_34;
    private final Map<String, Long> rc5Params;
    private final IrSignal irSignal;

    public CommandNGTest() throws IrpMasterException, IOException {
        Command.setIrpMaster(IRP_PROTOCOLS_PATH);
        //nec1_12_34_56_ccf = new Command("nec1_12_34_56_ccf", "Test ccf", NEC1_12_34_56_CCF);
        nec1Params = new HashMap<>(3);
        nec1Params.put("D", 12L);
        nec1Params.put("S", 34L);
        nec1Params.put("F", 56L);
        nec1_12_34_56_param = new Command("nec1_12_34_56_param", "Parametrized signal", "nec1", nec1Params);
        irSignal = new IrSignal(38321, 0.42, NEC1_12_34_56_INTRO, NEC1_REPEAT, null);
        nec1_12_34_56_irSignal = new Command("nec1_12_34_56_irSignal", "Command from IrSignal", irSignal);
        rc5Params = new HashMap<>(2);
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
     * Test of parseParameter method, of class Command.
     */
    @Test
    public void testParseParameter() {
        System.out.println("parseParameter");
        assertEquals(Command.parseParameter("1234567890"), 1234567890L);
        assertEquals(Command.parseParameter("0x1234abcdef"), 0x1234abcdefL);
    }

    /**
     * Test of setIrpMaster method, of class Command.
     * @throws java.io.IOException
     * @throws org.harctoolbox.IrpMaster.IncompatibleArgumentException
     */
    @Test
    public void testSetIrpMaster_String() throws IOException, IncompatibleArgumentException {
        System.out.println("setIrpMaster");
        Command.setIrpMaster(IRP_PROTOCOLS_PATH);
    }

//    /**
//     * Test of concatenateAsSequence method, of class Command.
//     */
//    @Test
//    public void testConcatenateAsSequence() throws Exception {
//        System.out.println("concatenateAsSequence");
//        Collection<Command> commands = null;
//        ModulatedIrSequence expResult = null;
//        ModulatedIrSequence result = Command.concatenateAsSequence(commands);
//        assertEquals(result, expResult);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of getDutyCycle method, of class Command.
     */
    @Test
    public void testGetDutyCycle() {
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
        String result = nec1_12_34_56_irSignal.getNotes();
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
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetProtocolName() throws IrpMasterException {
        System.out.println("getProtocolName");
        String result = nec1_12_34_56_irSignal.getProtocolName();
        assertEquals(result, "NEC1");
    }

    /**
     * Test of getParameters method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetParameters() throws IrpMasterException {
        System.out.println("getParameters");
        Map<String, Long> result = nec1_12_34_56_irSignal.getParameters();
        assertEquals(result, nec1Params);
    }

    /**
     * Test of getFrequency method, of class Command.
     */
    @Test
    public void testGetFrequency() {
        System.out.println("getFrequency");
        double expResult = 38321f;
        double result = nec1_12_34_56_irSignal.getFrequency();
        assertEquals(result, expResult, 0.0);
    }

    /**
     * Test of getIntro method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetIntro_0args() throws IrpMasterException {
        System.out.println("getIntro");
        String expResult = NEC1_12_34_56_INTRO;
        String result = nec1_12_34_56_irSignal.getIntro();
        assertEquals(result, expResult);
    }

    /**
     * Test of getIntro method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetIntro_int() throws IrpMasterException {
        System.out.println("getIntro");
        int T = 0;
        String expResult = NEC1_12_34_56_INTRO;
        String result = nec1_12_34_56_irSignal.getIntro(T);
        assertEquals(result, expResult);
    }

    /**
     * Test of getRepeat method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetRepeat_0args() throws IrpMasterException {
        System.out.println("getRepeat");
        String expResult = NEC1_REPEAT;
        String result = nec1_12_34_56_irSignal.getRepeat();
        assertEquals(result, expResult);
    }

    /**
     * Test of getRepeat method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetRepeat_int() throws IrpMasterException {
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
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetEnding_0args() throws IrpMasterException {
        System.out.println("getEnding");
        Command instance = null;
        String expResult = "";
        String result = rc5_12_34.getEnding();
        assertEquals(result, expResult);
    }

    /**
     * Test of getEnding method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetEnding_int() throws IrpMasterException {
        System.out.println("getEnding");
        int T = 0;
        String expResult = "";
        String result = rc5_12_34.getEnding(T);
        assertEquals(result, expResult);
    }

    /**
     * Test of getCcf method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetCcf_0args() throws IrpMasterException {
        System.out.println("getCcf");
        String expResult = NEC1_12_34_56_CCF;
        String result = nec1_12_34_56_param.getCcf();
        assertEquals(result, expResult);
    }

    /**
     * Test of getCcf method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testGetCcf_int() throws IrpMasterException {
        System.out.println("getCcf");
        int T = 0;
        String expResult = RC5_12_34_0_CCF;
        String result = rc5_12_34.getCcf(T);
        assertEquals(result, expResult);
        T = 1;
        expResult = RC5_12_34_1_CCF;
        result = rc5_12_34.getCcf(T);
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
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testToIrSignal_0args() throws IrpMasterException, FileNotFoundException {
        System.out.println("toIrSignal");
        IrSignal expResult = new IrSignal(IRP_PROTOCOLS_PATH, "NEC1", nec1Params);
        IrSignal result = nec1_12_34_56_param.toIrSignal();
        assertTrue(result.isEqual(expResult)); // TODO: IrSignal should probably implement equals(Object)
    }

    /**
     * Test of toIrSignal method, of class Command.
     * @throws java.io.FileNotFoundException
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testToIrSignal_int() throws FileNotFoundException, IrpMasterException {
        System.out.println("toIrSignal");
        int T = 0;
        Map<String, Long> params = new HashMap<>(3);
        params.put("D", 12L);
        params.put("F", 34L);
        params.put("T", (long) T);
        IrSignal expResult = new IrSignal(IRP_PROTOCOLS_PATH, "RC5", params);
        IrSignal result = rc5_12_34.toIrSignal(T);
        assertTrue(result.isEqual(expResult));
        T = 1;
        params.put("T", (long) T);
        expResult = new IrSignal(IRP_PROTOCOLS_PATH, "RC5", params);
        result = rc5_12_34.toIrSignal(T);
        assertTrue(result.isEqual(expResult));
    }

    /**
     * Test of nameProtocolParameterString method, of class Command.
     */
    @Test
    public void testNameProtocolParameterString() {
        System.out.println("nameProtocolParameterString");
        String expResult = "nec1_12_34_56_param: nec1 Device: 12.34 Function: 56 S=34";
        String result = nec1_12_34_56_param.nameProtocolParameterString();
        assertEquals(result, expResult);
    }

    /**
     * Test of prettyValueString method, of class Command.
     */
    @Test
    public void testPrettyValueString() {
        System.out.println("prettyValueString");
        String expResult = "nec1, S=34 D=12 F=56";
        String result = nec1_12_34_56_param.prettyValueString();
        assertEquals(result, expResult);
    }

    /**
     * Test of toPrintString method, of class Command.
     * @throws org.harctoolbox.IrpMaster.IrpMasterException
     */
    @Test
    public void testToPrintString() throws IrpMasterException {
        System.out.println("toPrintString");
        String expResult = "nec1_12_34_56_param: nec1, S=34 D=12 F=56";
        String result = nec1_12_34_56_param.toPrintString();
        assertEquals(result, expResult);
    }

    /**
     * Test of toString method, of class Command.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        String expResult = "nec1_12_34_56_param: nec1, S=34 D=12 F=56";
        String result = nec1_12_34_56_param.toString();
        assertEquals(result, expResult);
    }

    /**
     * Test of numberOfToggleValues method, of class Command.
     */
    @Test
    public void testNumberOfToggleValues() {
        System.out.println("numberOfToggleValues");
        Command instance = null;
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
}
