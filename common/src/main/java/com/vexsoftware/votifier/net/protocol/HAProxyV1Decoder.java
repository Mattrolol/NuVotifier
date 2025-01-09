package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class HAProxyV1Decoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        String proxyHeader = in.toString(StandardCharsets.US_ASCII); // Read the complete content

        if (proxyHeader.startsWith("PROXY")) {
            String[] parts = proxyHeader.split(" ");
            int headerLength = proxyHeader.indexOf("\r\n") + 2; // Include "\r\n" terminator
            if (headerLength <= 1) {
                throw new IllegalStateException("Malformed HAProxy header: no terminator found!");
            }

            // Skip all the HAProxy header bytes first
            in.skipBytes(headerLength); // Skip processed bytes so downstream handler gets the payload

            //System.out.println("HAProxy header skipped: " + headerLength + " bytes");

            if (parts.length >= 6) {
                // Extract client IP and port details
                String clientIp = parts[2];
                int clientPort = Integer.parseInt(parts[4]);

                // Update Votifier session with extracted details
                VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();
                if (session != null) {
                    session.setHaProxyProtocolVersion("v1");
                    session.setClientIp(clientIp);
                    session.setClientPort(clientPort);
                    session.setHaProxyUsed(true);
                } else {
                    System.err.println("VotifierSession was null while processing HAProxy v1.");
                }
                //System.out.println("HAProxy v1: Client IP: " + clientIp + ", Port: " + clientPort);
            } else {
                throw new IllegalStateException("HAProxy header does not contain sufficient data!");
            }

            // Remove this handler from the pipeline
            ctx.pipeline().remove(this);

            // Pass the remaining payload downstream
            ctx.fireChannelRead(in.retain());
        } else {
            throw new IllegalStateException("Invalid HAProxy header received!");
        }
    }
}