package org.harctoolbox.girr;

import java.io.File;
import java.io.IOException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.NamedProtocol;
import org.harctoolbox.xml.XmlUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author bengt
 */
public class RemoteSetNGTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    private final RemoteSet remoteSet;

    public RemoteSetNGTest() throws GirrException, IOException, SAXException {
        remoteSet = new RemoteSet(new File("src/test/girr/philips_37pfl9603_all.girr"));
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
        File file = null;
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
        String filename = "remoteset.girr";
        File file = new File(filename);
        String title = "It will just go away by itself";
        String stylesheetType = "";
        String stylesheetUrl = "";
        boolean fatRaw = false;
        boolean createSchemaLocation = true;
        boolean generateRaw = true;
        boolean generateCcf = true;
        boolean generateParameters = true;
        Document document = remoteSet.toDocument(title, stylesheetType, stylesheetUrl, fatRaw, createSchemaLocation, generateRaw, generateCcf, generateParameters);
        XmlUtils.printDOM(file, document);
        System.out.println("RemoteSet was written to the file " + file.getCanonicalPath() + ", please examine manually");
    }
}
