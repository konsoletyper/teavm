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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class InetSocketAddressTest {

    @Test
    public void test_loopback_address_with_port() throws Exception {
        InetSocketAddress addr1 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{
            127, 0, 0, 1
        }), 8080);
        assertTrue("1. should be an instance of InetSocketAddress", addr1 instanceof InetSocketAddress);
        assertTrue("2. host string should be 127.0.0.1", addr1.getHostString().equals("127.0.0.1"));
        assertTrue("3. port should be 8080", addr1.getPort() == 8080);
        assertTrue("4. address bytes should be [127, 0, 0, 1]",
                Arrays.equals(addr1.getAddress().getAddress(), new byte[]{127, 0, 0, 1}));
        assertTrue("5. should be a loopback address", addr1.getAddress().isLoopbackAddress());
        assertTrue("6. should not be any local address", !addr1.getAddress().isAnyLocalAddress());
        assertTrue("7. should not be a multicast address", !addr1.getAddress().isMulticastAddress());
        assertTrue("8. should not be a site-local address", !addr1.getAddress().isSiteLocalAddress());
    }

    @Test
    public void test_custom_ipv4_address_with_port() throws Exception {
        InetSocketAddress addr2 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{
            (byte) 192, (byte) 168, 1, 1
        }), 9090);
        assertTrue("1. should be an instance of InetSocketAddress", addr2 instanceof InetSocketAddress);
        assertTrue("2. host string should be 192.168.1.1", addr2.getHostString().equals("192.168.1.1"));
        assertTrue("3. port should be 9090", addr2.getPort() == 9090);
        assertTrue("4. address bytes should be [192, 168, 1, 1]",
                Arrays.equals(addr2.getAddress().getAddress(), new byte[]{(byte) 192, (byte) 168, 1, 1}));
        assertTrue("5. should not be a loopback address", !addr2.getAddress().isLoopbackAddress());
        assertTrue("6. should not be any local address", !addr2.getAddress().isAnyLocalAddress());
        assertTrue("7. should not be a multicast address", !addr2.getAddress().isMulticastAddress());
        assertTrue("8. should be a site-local address", addr2.getAddress().isSiteLocalAddress());
    }

    @Test
    public void test_ipv6_address_with_port() throws Exception {
        InetSocketAddress addr3 = new InetSocketAddress(InetAddress.getByAddress(new byte[]{
            (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
        }), 443);
        assertTrue("1. should be an instance of InetSocketAddress", addr3 instanceof InetSocketAddress);
        assertTrue("2. host string should be /2001:db8::1", addr3.getHostString().equals("2001:db8::1"));
        assertTrue("3. port should be 443", addr3.getPort() == 443);
        assertTrue("4. address bytes should match 2001:db8::1",
                Arrays.equals(addr3.getAddress().getAddress(), new byte[]{
                    (byte) 0x20, (byte) 0x01, 0x0d, (byte) 0xb8, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
                }));
        assertTrue("5. should not be a loopback address", !addr3.getAddress().isLoopbackAddress());
        assertTrue("6. should not be any local address", !addr3.getAddress().isAnyLocalAddress());
        assertTrue("7. should not be a multicast address", !addr3.getAddress().isMulticastAddress());
        assertTrue("8. should not be a site-local address", !addr3.getAddress().isSiteLocalAddress());
    }
}
