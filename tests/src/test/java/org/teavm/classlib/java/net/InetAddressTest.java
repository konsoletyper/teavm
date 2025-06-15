/*
 *  Copyright 2025 Maksim Tiushev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.net;

import static org.junit.Assert.*;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class InetAddressTest {

    @Test
    public void localhostInet4Address() throws Exception {
        var addr1 = InetAddress.getByName("localhost");

        assertTrue("1. should be an instance of Inet4Address", addr1 instanceof Inet4Address);
        assertEquals("2. host address should be 127.0.0.1", "127.0.0.1", addr1.getHostAddress());
        assertEquals("3. toString() should return localhost/127.0.0.1", "localhost/127.0.0.1", addr1.toString());
        assertArrayEquals("4. address bytes should be [127, 0, 0, 1]", new byte[]{127, 0, 0, 1}, addr1.getAddress());
        assertEquals("5. canonical host name should be localhost", "localhost", addr1.getCanonicalHostName());
        assertEquals("6. host name should be localhost", "localhost", addr1.getHostName());
        assertFalse("7. should not be any local address", addr1.isAnyLocalAddress());
        assertFalse("8. should not be a link-local address", addr1.isLinkLocalAddress());
        assertTrue("9. should be a loopback address", addr1.isLoopbackAddress());
        assertFalse("10. should not be a global multicast address", addr1.isMCGlobal());
        assertFalse("11. should not be a node-local multicast address", addr1.isMCNodeLocal());
        assertFalse("12. should not be an organization-local multicast address", addr1.isMCOrgLocal());
        assertFalse("13. should not be a site-local multicast address", addr1.isMCSiteLocal());
        assertFalse("14. should not be a multicast address", addr1.isMulticastAddress());
        assertFalse("15. should not be a site-local address", addr1.isSiteLocalAddress());
    }

    @Test
    public void inet4AddressBasicProperties() throws Exception {
        var addr2 = (Inet4Address) InetAddress.getByAddress(new byte[]{127, 0, 0, 1});

        assertTrue("1. should be an instance of Inet4Address", addr2 instanceof Inet4Address);
        assertEquals("2. host address should be 127.0.0.1", "127.0.0.1", addr2.getHostAddress());
        assertEquals("3. toString() should return /127.0.0.1", "/127.0.0.1", addr2.toString());
        assertArrayEquals("4. address bytes should be [127, 0, 0, 1]", new byte[]{127, 0, 0, 1}, addr2.getAddress());
        assertFalse("5. should not be any local address", addr2.isAnyLocalAddress());
        assertFalse("6. should not be a link-local address", addr2.isLinkLocalAddress());
        assertTrue("7. should be a loopback address", addr2.isLoopbackAddress());
        assertFalse("8. should not be a global multicast address", addr2.isMCGlobal());
        assertFalse("9. should not be a node-local multicast address", addr2.isMCNodeLocal());
        assertFalse("10. should not be an organization-local multicast address", addr2.isMCOrgLocal());
        assertFalse("11. should not be a site-local multicast address", addr2.isMCSiteLocal());
        assertFalse("12. should not be a multicast address", addr2.isMulticastAddress());
        assertFalse("13. should not be a site-local address", addr2.isSiteLocalAddress());
    }

    @Test
    public void inet4AddressCustomBytes() throws Exception {
        var addr3 = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});

        assertTrue("1. should be an instance of Inet4Address", addr3 instanceof Inet4Address);
        assertEquals("2. host address should be 192.168.1.1", "192.168.1.1", addr3.getHostAddress());
        assertEquals("3. toString() should return /192.168.1.1", "/192.168.1.1", addr3.toString());
        assertArrayEquals("4. address bytes should be [192, 168, 1, 1]",
                new byte[]{(byte) 192, (byte) 168, 1, 1}, addr3.getAddress());
        assertFalse("5. should not be any local address", addr3.isAnyLocalAddress());
        assertFalse("6. should not be a link-local address", addr3.isLinkLocalAddress());
        assertFalse("7. should not be a loopback address", addr3.isLoopbackAddress());
        assertFalse("8. should not be a multicast address", addr3.isMulticastAddress());
        assertTrue("9. should be a site-local address", addr3.isSiteLocalAddress());
    }

    @Test
    public void loopbackAddressIPv6() throws Exception {
        var addr4 = (Inet6Address) InetAddress.getByAddress(new byte[]{
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        });

        assertTrue("1. should be an instance of Inet6Address", addr4 instanceof Inet6Address);
        assertEquals("2. host address should be ::1", "::1", addr4.getHostAddress());
        assertEquals("3. toString() should return /::1", "/::1", addr4.toString());
        assertArrayEquals("4. address bytes should be [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]",
                new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1}, addr4.getAddress());
        assertFalse("5. should not be any local address", addr4.isAnyLocalAddress());
        assertFalse("6. should not be a link-local address", addr4.isLinkLocalAddress());
        assertTrue("7. should be a loopback address", addr4.isLoopbackAddress());
        assertFalse("8. should not be a global multicast address", addr4.isMCGlobal());
        assertFalse("9. should not be a node-local multicast address", addr4.isMCNodeLocal());
        assertFalse("10. should not be an organization-local multicast address", addr4.isMCOrgLocal());
        assertFalse("11. should not be a site-local multicast address", addr4.isMCSiteLocal());
        assertFalse("12. should not be a multicast address", addr4.isMulticastAddress());
        assertFalse("13. should not be a site-local address", addr4.isSiteLocalAddress());
    }

    @Test
    public void customAddressIPv6() throws Exception {
        var addr5 = (Inet6Address) InetAddress.getByAddress(new byte[]{
            (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        });

        assertTrue("1. should be an instance of Inet6Address", addr5 instanceof Inet6Address);
        assertEquals("2. host address should be 2001:db8::1", "2001:db8::1", addr5.getHostAddress());
        assertEquals("3. toString() should return /2001:db8::1", "/2001:db8::1", addr5.toString());
        assertArrayEquals("4. address bytes should be [32, 1, 13, -72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]",
                new byte[]{
                        (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
                }, addr5.getAddress());
        assertFalse("5. should not be any local address", addr5.isAnyLocalAddress());
        assertFalse("6. should not be a link-local address", addr5.isLinkLocalAddress());
        assertFalse("7. should not be a loopback address", addr5.isLoopbackAddress());
        assertFalse("8. should not be a global multicast address", addr5.isMCGlobal());
        assertFalse("9. should not be a node-local multicast address", addr5.isMCNodeLocal());
        assertFalse("10. should not be an organization-local multicast address", addr5.isMCOrgLocal());
        assertFalse("11. should not be a site-local multicast address", addr5.isMCSiteLocal());
        assertFalse("12. should not be a multicast address", addr5.isMulticastAddress());
        assertFalse("13. should not be a site-local address", addr5.isSiteLocalAddress());
    }


    @Test
    public void multicastAddressIPv6() throws Exception {
        var addr6 = (Inet6Address) InetAddress.getByAddress(new byte[]{
            (byte) 0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        });
        assertTrue("1. should be an instance of Inet6Address", addr6 instanceof Inet6Address);
        assertEquals("2. host address should be ff02::1", "ff02::1", addr6.getHostAddress());
        assertEquals("3. toString() should return /ff02::1", "/ff02::1", addr6.toString());
        assertArrayEquals("4. address bytes should be [255, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]",
                new byte[]{
                        (byte) 0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
                }, addr6.getAddress());
        assertFalse("5. should not be any local address", addr6.isAnyLocalAddress());
        assertFalse("6. should not be a link-local address", addr6.isLinkLocalAddress());
        assertFalse("7. should not be a loopback address", addr6.isLoopbackAddress());
        assertFalse("8. should not be a global multicast address", addr6.isMCGlobal());
        assertFalse("9. should not be a node-local multicast address", addr6.isMCNodeLocal());
        assertFalse("10. should not be an organization-local multicast address", addr6.isMCOrgLocal());
        assertFalse("11. should not be a site-local multicast address", addr6.isMCSiteLocal());
        assertTrue("12. should be a multicast address", addr6.isMulticastAddress());
        assertFalse("13. should not be a site-local address", addr6.isSiteLocalAddress());
    }
}
