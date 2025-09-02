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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class InetSocketAddressTest {

    @Test
    public void loopbackAddressWithPort() throws Exception {
        var addr1 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 8080);

        assertTrue("1. should be an instance of InetSocketAddress", addr1 instanceof InetSocketAddress);
        assertEquals("2. host string should be 127.0.0.1", "127.0.0.1", addr1.getHostString());
        assertEquals("3. port should be 8080", 8080, addr1.getPort());
        assertArrayEquals("4. address bytes should be [127, 0, 0, 1]",
                new byte[]{127, 0, 0, 1}, addr1.getAddress().getAddress());
        assertTrue("5. should be a loopback address", addr1.getAddress().isLoopbackAddress());
        assertFalse("6. should not be any local address", addr1.getAddress().isAnyLocalAddress());
        assertFalse("7. should not be a multicast address", addr1.getAddress().isMulticastAddress());
        assertFalse("8. should not be a site-local address", addr1.getAddress().isSiteLocalAddress());
    }

    @Test
    public void customIpv4AddressWithPort() throws Exception {
        var addr2 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{(byte) 192, (byte) 168, 1, 1}), 9090);

        assertTrue("1. should be an instance of InetSocketAddress", addr2 instanceof InetSocketAddress);
        assertEquals("2. host string should be 192.168.1.1", "192.168.1.1", addr2.getHostString());
        assertEquals("3. port should be 9090", 9090, addr2.getPort());
        assertArrayEquals("4. address bytes should be [192, 168, 1, 1]",
                new byte[]{(byte) 192, (byte) 168, 1, 1}, addr2.getAddress().getAddress());
        assertFalse("5. should not be a loopback address", addr2.getAddress().isLoopbackAddress());
        assertFalse("6. should not be any local address", addr2.getAddress().isAnyLocalAddress());
        assertFalse("7. should not be a multicast address", addr2.getAddress().isMulticastAddress());
        assertTrue("8. should be a site-local address", addr2.getAddress().isSiteLocalAddress());
    }

    @Test
    public void ipv6AddressWithPort() throws Exception {
        var addr3 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{
                (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        }), 443);

        assertTrue("1. should be an instance of InetSocketAddress", addr3 instanceof InetSocketAddress);
        assertEquals("2. host string should be 2001:db8::1", "2001:db8::1", addr3.getHostString());
        assertEquals("3. port should be 443", 443, addr3.getPort());
        assertArrayEquals("4. address bytes should match 2001:db8::1",
                new byte[]{
                        (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
                        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
                }, addr3.getAddress().getAddress());
        assertFalse("5. should not be a loopback address", addr3.getAddress().isLoopbackAddress());
        assertFalse("6. should not be any local address", addr3.getAddress().isAnyLocalAddress());
        assertFalse("7. should not be a multicast address", addr3.getAddress().isMulticastAddress());
        assertFalse("8. should not be a site-local address", addr3.getAddress().isSiteLocalAddress());
    }
}
