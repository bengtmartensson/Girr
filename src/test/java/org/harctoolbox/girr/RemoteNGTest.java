package org.harctoolbox.girr;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class RemoteNGTest {

    private final Remote sonyRemote;
    private final Remote philipsRemote;

    public RemoteNGTest() throws GirrException, IOException, SAXException {
        sonyRemote = new Remote("src/test/girr/sony_tv.girr");
        philipsRemote = new RemoteSet("src/test/girr/philips_37pfl9603_all.girr").iterator().next();
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
     * Test of getCommands method, of class Remote.
     */
    @Test
    public void testGetCommands() {
        System.out.println("getCommands");
        int expResult = 25;
        @SuppressWarnings("deprecation")
        Collection<Command> result = sonyRemote.getCommands();
        assertEquals(result.size(), expResult);
    }

    /**
     * Test of toElement method, of class Remote.
     * @throws java.io.IOException
     */
    @Test
    public void testPrint() throws IOException {
        System.out.println("print");
        File filename = new File(OUTDIR, "remote.girr");
        sonyRemote.print(filename);
        CommandNGTest.assertFileEqualContent(filename);
    }

    /**
     * Test of sort method, of class Remote.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testSort_0args() throws GirrException, IOException, SAXException {
        System.out.println("sort");
        Remote instance = new Remote("src/test/girr/sony_tv.girr");
        @SuppressWarnings("deprecation")
        String first = instance.getCommands().iterator().next().getName();
        assertEquals(first, "volume_up");
        instance.sortCommands();
        first = instance.getCommands().iterator().next().getName();
        assertEquals(first, "channel_down");
    }

    /**
     * Test of normalize method, of class Remote.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testNormalize() throws GirrException, IOException, SAXException {
        System.out.println("normalize");
        Remote instance = new RemoteSet("src/test/girr/philips_37pfl9603_all.girr").iterator().next();
        assertEquals(instance.getCommandSets().size(), 3);
        instance.normalize();
        assertEquals(instance.getCommandSets().size(), 1);
    }

    /**
     * Test of containsThisProtocol method, of class Remote.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testContainsThisProtocol() throws IrpException, IrCoreException {
        System.out.println("containsThisProtocol");
        assertTrue(philipsRemote.containsThisProtocol("rc5"));
        assertTrue(philipsRemote.containsThisProtocol("rc6"));
        assertFalse(philipsRemote.containsThisProtocol("nec1"));
    }

    /**
     * Test of containsThisProtocol method, of class Remote.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testHasThisProtocol() throws IrpException, IrCoreException {
        System.out.println("containsThisProtocol");
        assertFalse(philipsRemote.hasThisProtocol("rc5"));
        assertFalse(philipsRemote.hasThisProtocol("rc6"));
        assertFalse(philipsRemote.hasThisProtocol("nec1"));
        assertTrue(sonyRemote.hasThisProtocol("sony12"));
    }

    /**
     * Test of getAllCommands method, of class Remote.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testGetAllCommands_String() throws GirrException, IOException, SAXException {
        System.out.println("getAllCommands");
        String commandName = "cmd_0";
        Remote instance = new RemoteSet("src/test/girr/philips_37pfl9603_all.girr").iterator().next();
        int expResult = 2;
        @SuppressWarnings("deprecation")
        List<Command> result = instance.getCommand(commandName);
        assertEquals(result.size(), expResult);
    }

    /**
     * Test of getCommand method, of class Remote.
     */
    @Test
    public void testGetCommand() {
        System.out.println("getCommand");
        String commandName = "power_toggle";
        String expResult = "power_toggle: rc5, D=0 F=12";

        @SuppressWarnings("deprecation")
        List<Command> result = philipsRemote.getCommand(commandName);
        assertEquals(result.get(0).toString(), expResult);
        assertEquals(result.size(), 2);
    }

    /**
     * Test of getMetaData method, of class Remote.
     */
    @Test
    public void testGetMetaData() {
        System.out.println("getMetaData");
        String expResult = "37PFL9603";
        Remote.MetaData result = philipsRemote.getMetaData();
        assertEquals(result.getModel(), expResult);
    }

    /**
     * Test of getName method, of class Remote.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        String expResult = "philips_37pfl9603";
        String result = philipsRemote.getName();
        assertEquals(result, expResult);
    }

    /**
     * Test of getDisplayName method, of class Remote.
     */
    @Test
    public void testGetDisplayName() {
        System.out.println("getDisplayName");
        String expResult = "Philips 37PFL9603";
        String result = philipsRemote.getDisplayName();
        assertEquals(result, expResult);
    }

    /**
     * Test of getComment method, of class Remote.
     */
    @Test
    public void testGetComment() {
        System.out.println("getComment");
        String expResult = "Full HD";
        String result = philipsRemote.getComment();
        assertEquals(result, expResult);
    }

    /**
     * Test of numberAllCommands method, of class Remote.
     */
    @Test
    public void testNumberAllCommands() {
        System.out.println("numberAllCommands");
        Remote instance = philipsRemote;
        int expResult = 114;
        @SuppressWarnings("deprecation")
        int result = instance.getNumberOfCommands();
        assertEquals(result, expResult);
    }

    /**
     * Test of numberCommands method, of class Remote.
     */
    @Test
    public void testNumberCommands() {
        System.out.println("numberCommands");
        Remote instance = philipsRemote;
        int expResult = 114;
        @SuppressWarnings("deprecation")
        int result = instance.getNumberOfCommands();
        assertEquals(result, expResult);
    }

    /**
     * Test of getAllCommands method, of class Remote.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testGetAllCommands_0args() throws GirrException, IOException, SAXException {
        System.out.println("getAllCommands");
        Remote instance = new RemoteSet("src/test/girr/philips_37pfl9603_all.girr").iterator().next();
        @SuppressWarnings("deprecation")
        Collection<Command> result = instance.getCommands();
        assertEquals(result.size(), 114);
    }

    /**
     * Test of getApplicationParameters method, of class Remote.
     */
    @Test
    public void testGetApplicationParameters() {
        System.out.println("getApplicationParameters");
        Map<String, Map<String, String>> expResult = new HashMap<>(0);
        Map<String, Map<String, String>> result = philipsRemote.getApplicationParameters();
        assertEquals(result, expResult);
    }

    /**
     * Test of getManufacturer method, of class Remote.
     */
    @Test
    public void testGetManufacturer() {
        System.out.println("getManufacturer");
        String expResult = "Philips";
        String result = philipsRemote.getManufacturer();
        assertEquals(result, expResult);
    }

    /**
     * Test of getModel method, of class Remote.
     */
    @Test
    public void testGetModel() {
        System.out.println("getModel");
        String expResult = "37PFL9603";
        String result = philipsRemote.getModel();
        assertEquals(result, expResult);
    }

    /**
     * Test of getDeviceClass method, of class Remote.
     */
    @Test
    public void testGetDeviceClass() {
        System.out.println("getDeviceClass");
        String expResult = "tv";
        String result = philipsRemote.getDeviceClass();
        assertEquals(result, expResult);
    }

    /**
     * Test of getRemoteName method, of class Remote.
     */
    @Test
    public void testGetRemoteName() {
        System.out.println("getRemoteName");
        String expResult = "untitled";
        String result = philipsRemote.getRemoteName();
        assertEquals(result, expResult);
    }

    /**
     * Test of getNotes method, of class Remote.
     */
    @Test
    public void testGetNotes() {
        System.out.println("getNotes");
        String lang = "en";
        Remote instance = philipsRemote;
        String expResult = "This basic";
        String result = instance.getNotes(lang).substring(0, 10);
        assertEquals(result, expResult);
    }

    /**
     * Test of checkForParameters method, of class Remote.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testCheckForParameters() throws IrpException, IrCoreException {
        System.out.println("checkForParameters");
        sonyRemote.checkForParameters();
    }

    /**
     * Test of toFormattedString method, of class MetaData.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testToFormattedString() throws GirrException, IOException, SAXException {
        System.out.println("toFormattedString");
        Remote remote = new RemoteSet("src/test/girr/onkyo-t-4555.girr").iterator().next();
        Remote.MetaData metaData = remote.getMetaData();
        String expResult = "name: onkyo_t-4555\n"
                + "displayName: Integra/Onkyo TUN-3.7/T-4555 Multi Platform Radio Tuner  Remote ID 1, 2, 3.\n"
                + "manufacturer: Onkyo\n"
                + "model: Integra T-4555\n"
                + "deviceClass: Tuner\n"
                + "remoteName: RC-671T";
        String result = metaData.toFormattedString();
        assertEquals(result, expResult);
    }
}
