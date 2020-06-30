package org.harctoolbox.girr;

import java.io.File;
import java.io.IOException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.NamedProtocol;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
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

    public RemoteSetNGTest() {
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
}
