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

import static org.junit.Assert.assertTrue;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class InetAddressTest {

    @Test
    public void test_LocalhostInet4Address() throws Exception {
        InetAddress addr1 = InetAddress.getByName("localhost");
        assertTrue("1. should be an instance of Inet4Address", addr1 instanceof Inet4Address);
        assertTrue("2. host address should be 127.0.0.1", addr1.getHostAddress().equals("127.0.0.1"));
        assertTrue("3. toString() should return localhost/127.0.0.1",
                addr1.toString().equals("localhost/127.0.0.1"));
        assertTrue("4. address bytes should be [127, 0, 0, 1]",
                Arrays.equals(addr1.getAddress(), new byte[]{127, 0, 0, 1}));
        assertTrue("5. canonical host name should be localhost", addr1.getCanonicalHostName().equals("localhost"));
        assertTrue("6. host name should be localhost", addr1.getHostName().equals("localhost"));
        assertTrue("7. should not be any local address", !addr1.isAnyLocalAddress());
        assertTrue("8. should not be a link-local address", !addr1.isLinkLocalAddress());
        assertTrue("9. should be a loopback address", addr1.isLoopbackAddress());
        assertTrue("10. should not be a global multicast address", !addr1.isMCGlobal());
        assertTrue("11. should not be a node-local multicast address", !addr1.isMCNodeLocal());
        assertTrue("12. should not be an organization-local multicast address", !addr1.isMCOrgLocal());
        assertTrue("13. should not be a site-local multicast address", !addr1.isMCSiteLocal());
        assertTrue("14. should not be a multicast address", !addr1.isMulticastAddress());
        assertTrue("15. should not be a site-local address", !addr1.isSiteLocalAddress());
    }

    @Test
    public void test_Inet4AddressBasicProperties() throws Exception {
        Inet4Address addr2 = (Inet4Address) InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        assertTrue("1. should be an instance of Inet4Address", addr2 instanceof Inet4Address);
        assertTrue("2. host address should be 127.0.0.1", addr2.getHostAddress().equals("127.0.0.1"));
        assertTrue("3. toString() should return /127.0.0.1", addr2.toString().equals("/127.0.0.1"));
        assertTrue("4. address bytes should be [127, 0, 0, 1]",
                Arrays.equals(addr2.getAddress(), new byte[]{127, 0, 0, 1}));
        assertTrue("5. should not be any local address", !addr2.isAnyLocalAddress());
        assertTrue("6. should not be a link-local address", !addr2.isLinkLocalAddress());
        assertTrue("7. should be a loopback address", addr2.isLoopbackAddress());
        assertTrue("8. should not be a global multicast address", !addr2.isMCGlobal());
        assertTrue("9. should not be a node-local multicast address", !addr2.isMCNodeLocal());
        assertTrue("10. should not be an organization-local multicast address", !addr2.isMCOrgLocal());
        assertTrue("11. should not be a site-local multicast address", !addr2.isMCSiteLocal());
        assertTrue("12. should not be a multicast address", !addr2.isMulticastAddress());
        assertTrue("13. should not be a site-local address", !addr2.isSiteLocalAddress());
    }

    @Test
    public void test_Inet4AddressCustomBytes() throws Exception {
        Inet4Address addr3 = (Inet4Address) InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 1});
        assertTrue("1. should be an instance of Inet4Address", addr3 instanceof Inet4Address);
        assertTrue("2. host address should be 192.168.1.1", addr3.getHostAddress().equals("192.168.1.1"));
        assertTrue("3. toString() should return /192.168.1.1", addr3.toString().equals("/192.168.1.1"));
        assertTrue("4. address bytes should be [192, 168, 1, 1]",
                Arrays.equals(addr3.getAddress(), new byte[]{(byte) 192, (byte) 168, 1, 1}));
        assertTrue("5. should not be any local address", !addr3.isAnyLocalAddress());
        assertTrue("6. should not be a link-local address", !addr3.isLinkLocalAddress());
        assertTrue("7. should not be a loopback address", !addr3.isLoopbackAddress());
        assertTrue("8. should not be a multicast address", !addr3.isMulticastAddress());
        assertTrue("9. should not be a site-local address", addr3.isSiteLocalAddress());
    }

    @Test
    public void testLoopbackAddressIPv6() throws Exception {
        Inet6Address addr4 = (Inet6Address) InetAddress.getByAddress(new byte[]{
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        });
        assertTrue("1. should be an instance of Inet6Address", addr4 instanceof Inet6Address);
        assertTrue("2. host address should be ::1", addr4.getHostAddress().equals("::1"));
        assertTrue("3. toString() should return /::1", addr4.toString().equals("/::1"));
        assertTrue("4. address bytes should be [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]",
                Arrays.equals(addr4.getAddress(), new byte[]{
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
                }));
        assertTrue("5. should not be any local address", !addr4.isAnyLocalAddress());
        assertTrue("6. should not be a link-local address", !addr4.isLinkLocalAddress());
        assertTrue("7. should be a loopback address", addr4.isLoopbackAddress());
        assertTrue("8. should not be a global multicast address", !addr4.isMCGlobal());
        assertTrue("9. should not be a node-local multicast address", !addr4.isMCNodeLocal());
        assertTrue("10. should not be an organization-local multicast address", !addr4.isMCOrgLocal());
        assertTrue("11. should not be a site-local multicast address", !addr4.isMCSiteLocal());
        assertTrue("12. should not be a multicast address", !addr4.isMulticastAddress());
        assertTrue("13. should not be a site-local address", !addr4.isSiteLocalAddress());
    }

    @Test
    public void testCustomAddressIPv6() throws Exception {
        Inet6Address addr5 = (Inet6Address) InetAddress.getByAddress(new byte[]{
            (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        });
        assertTrue("1. should be an instance of Inet6Address", addr5 instanceof Inet6Address);
        assertTrue("2. host address should be 2001:db8::1", addr5.getHostAddress().equals("2001:db8::1"));
        assertTrue("3. toString() should return /2001:db8::1", addr5.toString().equals("/2001:db8::1"));
        assertTrue("4. address bytes should be [32, 1, 13, -72, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]",
                Arrays.equals(addr5.getAddress(), new byte[]{
                    (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
                }));
        assertTrue("5. should not be any local address", !addr5.isAnyLocalAddress());
        assertTrue("6. should not be a link-local address", !addr5.isLinkLocalAddress());
        assertTrue("7. should not be a loopback address", !addr5.isLoopbackAddress());
        assertTrue("8. should not be a global multicast address", !addr5.isMCGlobal());
        assertTrue("9. should not be a node-local multicast address", !addr5.isMCNodeLocal());
        assertTrue("10. should not be an organization-local multicast address", !addr5.isMCOrgLocal());
        assertTrue("11. should not be a site-local multicast address", !addr5.isMCSiteLocal());
        assertTrue("12. should not be a multicast address", !addr5.isMulticastAddress());
        assertTrue("13. should not be a site-local address", !addr5.isSiteLocalAddress());
    }


    @Test
    public void testMulticastAddressIPv6() throws Exception {
        Inet6Address addr6 = (Inet6Address) InetAddress.getByAddress(new byte[]{
            (byte) 0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        });
        assertTrue("1. should be an instance of Inet6Address", addr6 instanceof Inet6Address);
        assertTrue("2. host address should be ff02::1", addr6.getHostAddress().equals("ff02::1"));
        assertTrue("3. toString() should return /ff02::1", addr6.toString().equals("/ff02::1"));
        assertTrue("4. address bytes should be [255, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1]",
                Arrays.equals(addr6.getAddress(), new byte[]{
                    (byte) 0xff, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
                }));
        assertTrue("5. should not be any local address", !addr6.isAnyLocalAddress());
        assertTrue("6. should not be a link-local address", !addr6.isLinkLocalAddress());
        assertTrue("7. should not be a loopback address", !addr6.isLoopbackAddress());
        assertTrue("8. should not be a global multicast address", !addr6.isMCGlobal());
        assertTrue("9. should not be a node-local multicast address", !addr6.isMCNodeLocal());
        assertTrue("10. should not be an organization-local multicast address", !addr6.isMCOrgLocal());
        assertTrue("11. should not be a site-local multicast address", !addr6.isMCSiteLocal());
        assertTrue("12. should be a multicast address", addr6.isMulticastAddress());
        assertTrue("13. should not be a site-local address", !addr6.isSiteLocalAddress());
    }
}
