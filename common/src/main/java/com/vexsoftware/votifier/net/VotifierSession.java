package com.vexsoftware.votifier.net;

import com.vexsoftware.votifier.util.TokenUtil;
import io.netty.util.AttributeKey;

public class VotifierSession {
    public static final AttributeKey<VotifierSession> KEY = AttributeKey.valueOf("votifier_session");
    private ProtocolVersion version = ProtocolVersion.UNKNOWN;
    private final String challenge;
    private boolean hasCompletedVote = false;
    private boolean haProxyUsed = false;
    private String haProxyProtocolVersion;
    private String clientIp;
    private int clientPort;

    public VotifierSession() {
        challenge = TokenUtil.newToken();
    }

    public void setVersion(ProtocolVersion version) {
        if (this.version != ProtocolVersion.UNKNOWN)
            throw new IllegalStateException("Protocol version already switched");

        this.version = version;
    }

    public ProtocolVersion getVersion() {
        return version;
    }

    public String getChallenge() {
        return challenge;
    }

    public void completeVote() {
        if (hasCompletedVote)
            throw new IllegalStateException("Protocol completed vote twice!");

        hasCompletedVote = true;
    }

    public boolean hasCompletedVote() {
        return hasCompletedVote;
    }

    public boolean isHaProxyUsed() {
        return haProxyUsed;
    }

    public String getHaProxyProtocolVersion() {
        return haProxyProtocolVersion;
    }

    public void setHaProxyProtocolVersion(String haProxyProtocolVersion) {
        this.haProxyProtocolVersion = haProxyProtocolVersion;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setHaProxyUsed(boolean haProxyUsed) {
        this.haProxyUsed = haProxyUsed;
    }

    public enum ProtocolVersion {
        UNKNOWN("unknown"),
        ONE("protocol v1"),
        TWO("protocol v2"),
        TEST("test");

        public final String humanReadable;
        ProtocolVersion(String hr) {
            this.humanReadable = hr;
        }
    }
}
