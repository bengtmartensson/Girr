package org.harctoolbox.girr;

import java.io.IOException;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author bengt
 */
public class AdminDataNGTest {

    public AdminDataNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

     /**
     * Test of toFormattedString method, of class AdminData.
     * @throws org.harctoolbox.girr.GirrException
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void testToFormattedString() throws GirrException, IOException, SAXException {
        System.out.println("toFormattedString");
        AdminData instance = new RemoteSet("src/test/girr/marco.girr").getAdminData();
        instance.setCreatingUser("Nicolas Bourbaki");
        instance.setNotes("xyz");


        String expResult = "creatingUser: Nicolas Bourbaki\n"
                + "source: src/test/girr/marco.girr\n"
                + "creationDate: 2020-01-01\n"
                + "tool: IrpTransmogrifier\n"
                + "toolVersion: 1.2.7-SNAPSHOT\n"
                + "notes: xyz";
        String result = instance.toFormattedString();
        assertEquals(expResult, result);
    }
}
