package org.harctoolbox.girr;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import static org.harctoolbox.girr.CommandNGTest.OUTDIR;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.irp.IrpException;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CommandSetNGTest {

    private final CommandSet commandSet;

    public CommandSetNGTest() throws GirrException, IOException, SAXException {
        Command.setUseInheritanceForXml(true);
        commandSet = new CommandSet("src/test/girr/philips_tv_cmdset_rc6.girr");
    }

    @BeforeClass
    public void setUpClass() throws Exception {
        CommandNGTest.assertOutDirExists();
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of getCommands method, of class CommandSet.
     */
    @Test
    public void testGetCommands() {
        System.out.println("getCommands");
        int expResult = 41;
        Collection<Command> result = commandSet.getCommands();
        assertEquals(result.size(), expResult);
    }

    /**
     * Test of getCommand method, of class CommandSet.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testGetCommand() throws IrpException, IrCoreException {
        System.out.println("getCommand");
        String commandName = "covfefe";
        Command cmd = commandSet.getCommand(commandName);
        assertNull(cmd);
        cmd = commandSet.getCommand("epg");
        assertEquals(cmd.getProtocolName(), "rc6");
        assertEquals(cmd.getParameters().size(), 2);
    }

    /**
     * Test of getName method, of class CommandSet.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        String expResult = "rc6";
        String result = commandSet.getName();
        assertEquals(result, expResult);
    }

    /**
     * Test of size method, of class CommandSet.
     */
    @Test
    public void testSize() {
        System.out.println("size");
        int expResult = 41;
        int result = commandSet.size();
        assertEquals(result, expResult);
    }

    /**
     * Test of isEmpty method, of class CommandSet.
     */
    @Test
    public void testIsEmpty() {
        System.out.println("isEmpty");
        assertFalse(commandSet.isEmpty());
    }

    /**
     * Test of iterator method, of class CommandSet.
     */
    @Test
    public void testIterator() {
        System.out.println("iterator");
        int sum = 0;
        for (Iterator<Command> it = commandSet.iterator(); it.hasNext();) {
            it.next();
            sum++;
        }
        assertEquals(sum, 41);
    }

    /**
     * Test of sort method, of class CommandSet.
     */
    @Test
    public void testSort() {
        System.out.println("sort");
        commandSet.sort();
    }

    /**
     * Test of toElement method, of class CommandSet.
     * @throws java.io.IOException
     */
    @Test
    public void testPrint() throws IOException {
        System.out.println("print");
        File filename = new File(OUTDIR, "commandset.girr");
        commandSet.print(filename);
        CommandNGTest.assertFileEqualContent(filename);
    }

    /**
     * Test of toElement method, of class CommandSet.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testPrintSony() throws GirrException, IOException, SAXException {
        System.out.println("printSony");
        Remote remote = new Remote("src/test/girr/sony_tv.girr");
        CommandSet cmdSet = remote.iterator().next();
        File filename = new File(OUTDIR, "commandset_sony.girr");
        cmdSet.print(filename);
        CommandNGTest.assertFileEqualContent(filename);
    }

    @Test
    public void testPrintSonyPronto() throws GirrException, IOException, SAXException {
        System.out.println("printSony");
        Remote remote = new Remote("src/test/girr/sony_tv.girr");
        CommandSet cmdSet = remote.iterator().next();
        File filename = new File(OUTDIR, "commandset_sony_pronto.girr");
        cmdSet.print(filename, false, true, false);
        CommandNGTest.assertFileEqualContent(filename);
    }

    /**
     * Test of sort method, of class CommandSet.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testSort_Comparator() throws GirrException, IOException, SAXException {
        System.out.println("sort_Comparator");
        CommandSet instance = new CommandSet("src/test/girr/philips_tv_cmdset_rc6.girr");
        // Sort according to command name length (pretty silly ;-))
        instance.sort((Named c1, Named c2) -> { return c1.getName().length() - c2.getName().length(); });
        assertEquals(instance.iterator().next().getName(), "up");
    }

    /**
     * Test of sort method, of class CommandSet.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testSort_0args() throws GirrException, IOException, SAXException {
        System.out.println("sort");
        CommandSet instance = new CommandSet("src/test/girr/philips_tv_cmdset_rc6.girr");
        instance.sort();
        assertEquals(instance.iterator().next().getName(), "PP");
    }

    /**
     * Test of sortIgnoringCase method, of class CommandSet.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testSortIgnoringCase() throws GirrException, IOException, SAXException {
        System.out.println("sortIgnoringCase");
        CommandSet instance = new CommandSet("src/test/girr/philips_tv_cmdset_rc6.girr");
        instance.sortIgnoringCase();
        assertEquals(instance.iterator().next().getName(), "ambilight_mode");
    }
}
