package org.harctoolbox.girr;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import static org.harctoolbox.girr.CommandNGTest.OUTDIR;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.NamedProtocol;
import org.harctoolbox.xml.XmlUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class RemoteSetNGTest {

    private final RemoteSet remoteSet;

    public RemoteSetNGTest() throws GirrException, IOException, SAXException {
        remoteSet = new RemoteSet(new File("src/test/girr/philips_37pfl9603_all.girr"));
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

    @Test
    public void testParseFileOrDirectory() throws GirrException, SAXException, IOException {
        System.out.println("parseFileOrDirectory");
        RemoteSet result = new RemoteSet(new File("src/test/girr/marco.girr"));
        IrpDatabase database = result.getIrpDatabase();
        NamedProtocol protocol = database.iterator().next();
        String irp = protocol.toIrpString();
        assertEquals(irp, "{38.0k,421,msb}<1,-1|1,-3>(8,-3,A:63,B:63,C:2,1,-165m)");
        assertEquals(protocol.getName(), "p_48a3bbc1");
    }

    /**
     * Test of toDocument method, of class RemoteSet.
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testToDocument() throws IOException {
        System.out.println("toDocument");
        File file = new File(OUTDIR, "remoteset.girr");
        String title = "It will just go away by itself";
        boolean fatRaw = false;
        boolean generateRaw = true;
        boolean generateProntoHex = true;
        boolean generateParameters = true;
        Document document = remoteSet.toDocument(title, fatRaw, generateParameters, generateProntoHex, generateRaw);
        XmlUtils.printDOM(file, document);
        CommandNGTest.assertFileEqualContent(file);
    }

    /**
     * Test of isEmpty method, of class RemoteSet.
     */
    @Test
    public void testIsEmpty() {
        System.out.println("isEmpty");
        RemoteSet instance = new RemoteSet();
        boolean result = instance.isEmpty();
        assertTrue(result);
        assertFalse(remoteSet.isEmpty());
    }

//    /**
//     * Test of getAllCommands method, of class RemoteSet.
//     */
//    @Test
//    public void testGetAllCommands() {
//        System.out.println("getAllCommands");
//        @SuppressWarnings("deprecation")
//        List<Command> result = remoteSet.getAllCommands();
//        assertEquals(result.size(), 82);
//    }

    /**
     * Test of checkForParameters method, of class RemoteSet.
     * @throws org.harctoolbox.irp.IrpException
     * @throws org.harctoolbox.ircore.IrCoreException
     */
    @Test
    public void testCheckForParameters() throws IrpException, IrCoreException {
        System.out.println("checkForParameters");
        remoteSet.checkForParameters();
    }

    /**
     * Test of getCreatingUser method, of class RemoteSet.
     */
    @Test
    public void testGetCreatingUser() {
        System.out.println("getCreatingUser");
        String expResult = "Évariste Galois";
        String result = remoteSet.getCreatingUser();
        assertEquals(result, expResult);
    }

    /**
     * Test of getSource method, of class RemoteSet.
     */
    @Test
    public void testGetSource() {
        System.out.println("getSource");
        String expResult = "Whatever";
        String result = remoteSet.getSource();
        assertEquals(result, expResult);
    }

    /**
     * Test of getCreationDate method, of class RemoteSet.
     */
    @Test
    public void testGetCreationDate() {
        System.out.println("getCreationDate");
        String expResult = "35-septober-2999";
        String result = remoteSet.getCreationDate();
        assertEquals(result, expResult);
    }

    /**
     * Test of getTool method, of class RemoteSet.
     */
    @Test
    public void testGetTool() {
        System.out.println("getTool");
        String expResult = "hand written";
        String result = remoteSet.getTool();
        assertEquals(result, expResult);
    }

    /**
     * Test of getToolVersion method, of class RemoteSet.
     */
    @Test
    public void testGetToolVersion() {
        System.out.println("getToolVersion");
        String expResult = "0.0.0";
        String result = remoteSet.getToolVersion();
        assertEquals(result, expResult);
    }

    /**
     * Test of getTool2 method, of class RemoteSet.
     */
    @Test
    public void testGetTool2() {
        System.out.println("getTool2");
        String expResult = "nuthin";
        String result = remoteSet.getTool2();
        assertEquals(result, expResult);
    }

    /**
     * Test of getTool2Version method, of class RemoteSet.
     */
    @Test
    public void testGetTool2Version() {
        System.out.println("getTool2Version");
        String expResult = "who cares?";
        String result = remoteSet.getTool2Version();
        assertEquals(result, expResult);
    }

    /**
     * Test of getNotes method, of class RemoteSet.
     */
    @Test
    public void testGetNotes() {
        System.out.println("getNotes");
        String lang = "en";
        String expResult = "Lorem Ipsum. Or not.";
        String result = remoteSet.getNotes(lang);
        assertEquals(result, expResult);
    }

    /**
     * Test of getRemotes method, of class RemoteSet.
     */
    @Test
    public void testGetRemotes() {
        System.out.println("getRemotes");
        int expResult = 1;
        Collection<Remote> result = remoteSet.getRemotes();
        assertEquals(result.size(), expResult);
    }

    /**
     * Test of getRemote method, of class RemoteSet.
     */
    @Test
    public void testGetRemote() {
        System.out.println("getRemote");
        String name = "philips_37pfl9603";
        String expResult = "Full HD";
        Remote result = remoteSet.getRemote(name);
        assertEquals(result.getComment(), expResult);
    }

    /**
     * Test of getFirstRemote method, of class RemoteSet.
     */
    @Test
    public void testGetFirstRemote() {
        System.out.println("getFirstRemote");
        String expResult = "tv";
        Remote result = remoteSet.iterator().next();
        assertEquals(result.getDeviceClass(), expResult);
    }

    /**
     * Test of getFirstMetaData method, of class RemoteSet.
     */
    @Test
    public void testGetFirstMetaData() {
        System.out.println("getFirstMetaData");
        String expResult = "Philips 37PFL9603";
        Remote.MetaData result = remoteSet.iterator().next().getMetaData();
        assertEquals(result.getDisplayName(), expResult);
    }

    /**
     * Test of getIrpDatabase method, of class RemoteSet.
     */
    @Test
    public void testGetIrpDatabase() {
        System.out.println("getIrpDatabase");
        IrpDatabase expResult = new IrpDatabase();
        IrpDatabase result = remoteSet.getIrpDatabase();
        assertEquals(result.size(), 0);
        assertEquals(result, expResult);
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testRemoteSetOnCommand() throws IOException, SAXException {
        System.out.println("remoteOnCommand");
        try {
            new RemoteSet("src/test/girr/topping-command.girr");
            fail();
        } catch (GirrException ex) {
        }
    }

    /**
     * Test of parse method, of class RemoteSet.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testParse() throws GirrException, IOException, SAXException {
        System.out.println("parse");
        // Root element: remotes
        RemoteSet rs = RemoteSet.parse("src/test/girr/philips_37pfl9603_all.girr");
        assertEquals(rs.getCreatingUser(), "Évariste Galois");

        // Root element: remote
        rs = RemoteSet.parse("src/test/girr/sony_tv.girr");
        assertEquals(rs.getCreatingUser(), "Augustin-Louis Cauchy");

        // Root element: commandSet
        rs = RemoteSet.parse("src/test/girr/philips_tv_cmdset_rc6.girr");
        assertEquals(rs.getCreatingUser(), null);

        // Root element: command
        rs = RemoteSet.parse("src/test/girr/topping-command.girr");
        assertEquals(rs.iterator().next().iterator().next().getCommands().size(), 1);
    }

    /**
     * Test of parseFiles method, of class RemoteSet.
     * @throws java.io.IOException
     */
    public void testParseFiles() throws IOException {
        System.out.println("parseFiles");
        File path = new File("src/test/girr");
        Collection<RemoteSet> result = RemoteSet.parseAsCollection(path);
        assertEquals(result.size(), 9);
        RemoteSet rs = new RemoteSet("Imhotep", path.toString(), result);
        assertEquals(rs.size(), 9);
        rs.setCreationDate("Sometime");
        rs.strip();
        rs.sort(false);
        File filename = new File(OUTDIR, "fatremoteset.girr");
        rs.print(filename, false, false, false);
        CommandNGTest.assertFileEqualContent(filename);
    }

    /**
     * Test that "prontoHex" for "ccf" s msterType is accepted.
     */
    @Test
    public void testParseProntoHex() throws IOException, SAXException, GirrException {
        System.out.println("parseProntoHex");
        RemoteSet rs = RemoteSet.parse("src/test/girr/duhow.girr");
        Remote remote = rs.iterator().next();
        Command command = remote.iterator().next().iterator().next();
        assert(command.getMasterType() == Command.MasterType.ccf);
    }

    /**
     * Test of strip method, of class RemoteSet.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testStrip() throws GirrException, IOException, SAXException {
        System.out.println("strip");
        RemoteSet instance = new RemoteSet("src/test/girr/marco.girr");
        instance.strip();
        File filename = new File(OUTDIR, "stripped.girr");
        instance.print(filename, false, false, false);
        CommandNGTest.assertFileEqualContent(filename);

    }

    /**
     * Test of dump method, of class RemoteSet.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     * @throws java.lang.ClassNotFoundException
     */
    @Test
    public void testDump_File() throws GirrException, IOException, SAXException, ClassNotFoundException {
        System.out.println("dump");
        File file = new File("dump.bin");
        RemoteSet instance = new RemoteSet("src/test/girr/marco.girr");
        instance.dump(file);
        RemoteSet readInstance = RemoteSet.pmud("dump.bin");
        assertEquals(readInstance.getAdminData().getCreatingUser(), "Bengt Martensson");
    }
}
