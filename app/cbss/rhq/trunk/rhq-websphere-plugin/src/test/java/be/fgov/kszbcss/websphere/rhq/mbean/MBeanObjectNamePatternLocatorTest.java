package be.fgov.kszbcss.websphere.rhq.mbean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.management.ObjectName;

import org.junit.Test;

public class MBeanObjectNamePatternLocatorTest {
    @Test
    public void testEquals() throws Exception {
        MBeanLocator locator1 = new MBeanObjectNamePatternLocator(new ObjectName("WebSphere:type=Server,*"));
        MBeanLocator locator2 = new MBeanObjectNamePatternLocator(new ObjectName("WebSphere:type=Server,*"));
        assertTrue(locator1.equals(locator2));
    }
    
    @Test
    public void testHashCode() throws Exception {
        MBeanLocator locator1 = new MBeanObjectNamePatternLocator(new ObjectName("WebSphere:type=Server,*"));
        MBeanLocator locator2 = new MBeanObjectNamePatternLocator(new ObjectName("WebSphere:type=Server,*"));
        assertEquals(locator1.hashCode(), locator2.hashCode());
    }
}
