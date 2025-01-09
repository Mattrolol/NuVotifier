package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Differentiates between HAProxy headers (v1 or v2) before passing the remaining data downstream.
 */
public class HAProxyProtocolDifferentiator extends ByteToMessageDecoder {
    private static final int HAPROXY_V2_SIGNATURE_LENGTH = 12;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        int readableBytes = buf.readableBytes();

        if (readableBytes < 6) {
            // Not enough data to differentiate between protocols yet
            return;
        }

        // Retrieve the session
        VotifierSession session = ctx.channel().attr(VotifierSession.KEY).get();

        // Peek at the incoming data without consuming it
        byte[] proxyCheck = new byte[Math.min(readableBytes, 16)];
        buf.getBytes(0, proxyCheck); // Peek at the buffer

        // Check for HAProxy v1 (starts with "PROXY")
        String asString = new String(proxyCheck, StandardCharsets.UTF_8);
        if (asString.startsWith("PROXY")) {
            session.setHaProxyUsed(true);
            //System.out.println("HAProxy v1 detected for session: " + session.isHaProxyUsed());
            ctx.pipeline().addAfter("haProxyProtocolDifferentiator", "haProxyV1Decoder", new HAProxyV1Decoder());
            cleanPipelineAndPass(ctx, buf);
            return;
        }

        // Check for HAProxy v2 signature
        if (readableBytes >= HAPROXY_V2_SIGNATURE_LENGTH && isHaProxyV2Signature(proxyCheck)) {
            session.setHaProxyUsed(true);
            //System.out.println("HAProxy v2 detected for session: " + session.isHaProxyUsed());
            ctx.pipeline().addAfter("haProxyProtocolDifferentiator", "haProxyV2Decoder", new HAProxyV2Decoder());
            cleanPipelineAndPass(ctx, buf);
            return;
        }

        // If no valid HAProxy signature is found, pass-through to next handler
        ctx.pipeline().remove(this);
        ctx.fireChannelRead(buf.retain());
    }

    /**
     * Validate HAProxy v2 magic signature (first 12 bytes).
     */
    private boolean isHaProxyV2Signature(byte[] header) {
        return header[0] == 0x0D && header[1] == 0x0A &&
                header[2] == 0x0D && header[3] == 0x0A &&
                header[4] == 0x00 && header[5] == 0x0D &&
                header[6] == 0x0A && header[7] == 0x51 &&
                header[8] == 0x55 && header[9] == 0x49 &&
                header[10] == 0x54 && header[11] == 0x0A;
    }

    /**
     * Clean current handler and pass ByteBuf downstream.
     */
    private void cleanPipelineAndPass(ChannelHandlerContext ctx, ByteBuf buf) {
        ctx.pipeline().remove(this); // Remove this handler after detection
        buf.skipBytes(buf.readerIndex()); // Skip to ensure only raw payload is forwarded
        ctx.fireChannelRead(buf.retain()); // Pass the buffer downstream
    }
}