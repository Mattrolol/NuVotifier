package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.net.InetAddress;
import java.util.List;

public class HAProxyV2Decoder extends ByteToMessageDecoder {
    private static final int HA_PROXY_V2_HEADER_LENGTH = 16;
    private static final byte[] HA_PROXY_V2_SIGNATURE = {
            0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
    };

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HA_PROXY_V2_HEADER_LENGTH) {
            // Wait for the full header to arrive
            return;
        }

        // Check signature
        for (int i = 0; i < HA_PROXY_V2_SIGNATURE.length; i++) {
            if (in.getByte(i) != HA_PROXY_V2_SIGNATURE[i]) {
                throw new IllegalStateException("Invalid HAProxy v2 signature!");
            }
        }

        // Read command and address family
        byte command = in.getByte(12);
        if ((command & 0x0F) == 0x00) { // LOCAL command
            //System.out.println("HAProxy v2: LOCAL command received. Closing connection and stopping processing.");
            // Close the channel to stop further processing
            ctx.channel().close();
            return; // Stop processing further
        } else if ((command & 0x0F) != 0x01) { // Unsupported command
            throw new IllegalStateException("Unsupported HAProxy v2 command: " + (command & 0x0F));
        }

        byte addressFamily = in.getByte(13);
        int addressLength;
        if ((addressFamily & 0xF0) == 0x10) { // IPv4
            addressLength = 12; // 4 bytes source IP, 4 bytes dest IP, 2 bytes src port, 2 bytes dest port
        } else if ((addressFamily & 0xF0) == 0x20) { // IPv6
            addressLength = 36; // 16 bytes source IP, 16 bytes dest IP, 2 bytes src port, 2 bytes dest port
        } else {
            throw new IllegalStateException("Unsupported HAProxy v2 address family: " + (addressFamily & 0xF0));
        }

        if (in.readableBytes() < HA_PROXY_V2_HEADER_LENGTH + addressLength) {
            return; // Wait for the full address info to arrive
        }

        // Skip signature and header bytes
        in.skipBytes(HA_PROXY_V2_HEADER_LENGTH);

        // Parse address fields
        InetAddress clientIp;
        int clientPort;
        if (addressLength == 12) { // IPv4
            byte[] ipv4 = new byte[4];
            in.readBytes(ipv4);
            clientIp = InetAddress.getByAddress(ipv4);
            clientPort = in.readUnsignedShort();

            // Skip server IP (4 bytes) + server port (2 bytes)
            in.skipBytes(6);
            //System.out.println("HAProxy v2: Server address skipped: 6 bytes (IPv4)");
        } else { // IPv6
            byte[] ipv6 = new byte[16];
            in.readBytes(ipv6);
            clientIp = InetAddress.getByAddress(ipv6);
            clientPort = in.readUnsignedShort();

            // Skip server IP (16 bytes) + server port (2 bytes)
            in.skipBytes(18);
            //System.out.println("HAProxy v2: Server address skipped: 18 bytes (IPv6)");
        }

        // Update VotifierSession
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();
        if (session != null) {
            session.setHaProxyProtocolVersion("v2");
            session.setClientIp(clientIp.getHostAddress());
            session.setClientPort(clientPort);
            session.setHaProxyUsed(true);
            //System.out.println("HAProxy v2: Client IP: " + clientIp.getHostAddress() + ", Port: " + clientPort);
        } else {
            System.err.println("VotifierSession was null while processing HAProxy v2.");
        }

        // Remove this handler from the pipeline
        ctx.pipeline().remove(this);

        // Pass the remaining payload downstream
        ctx.fireChannelRead(in.retain());
    }
}