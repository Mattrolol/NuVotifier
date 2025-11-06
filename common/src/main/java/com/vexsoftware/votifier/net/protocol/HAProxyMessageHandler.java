package com.vexsoftware.votifier.net.protocol;

import com.vexsoftware.votifier.net.VotifierSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;

public class HAProxyMessageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HAProxyMessage) {
            HAProxyMessage proxyMessage = (HAProxyMessage) msg;

            if (proxyMessage.protocolVersion() == HAProxyProtocolVersion.V1 || proxyMessage.protocolVersion() == HAProxyProtocolVersion.V2) {
                String sourceAddress = proxyMessage.sourceAddress();
                int sourcePort = proxyMessage.sourcePort();

                // Store the real client address for later use
                ctx.channel().attr(VotifierSession.KEY).get().setRemoteAddress(sourceAddress, sourcePort);
            }

            proxyMessage.release();
        } else {
            super.channelRead(ctx, msg);
        }
    }
}