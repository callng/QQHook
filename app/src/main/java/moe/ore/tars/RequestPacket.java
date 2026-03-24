package moe.ore.tars;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import kotlin.jvm.internal.Intrinsics;

public final class RequestPacket extends TarsBase {
    private static byte[] cache_buffer;
    private static Map<String, String> cache_context;
    private static Map<String, String> cache_status;
    public byte[] buffer;
    public Map<String, String> context;
    public String funcName;
    public String servantName;
    public Map status;
    private int messageType;
    private byte packetType;
    private int requestId;
    private int timeout;
    private int version;

    public void readFrom(TarsInputStream tarsInputStream) {
        this.version = tarsInputStream.read(this.version, 1, false);
        this.packetType = tarsInputStream.read(this.packetType, 2, false);
        this.messageType = tarsInputStream.read(this.messageType, 3, false);
        this.requestId = tarsInputStream.read(this.requestId, 4, false);
        this.servantName = tarsInputStream.read(this.servantName, 5, false);
        this.funcName = tarsInputStream.read(this.funcName, 6, false);
        if (cache_buffer == null) {
            cache_buffer = new byte[1];
        }
        this.buffer = tarsInputStream.read(cache_buffer, 7, false);
        this.timeout = tarsInputStream.read(this.timeout, 8, false);
        if (cache_context == null) {
            HashMap hashMap = new HashMap();
            hashMap.put("", "");
            cache_context = hashMap;
        }
        this.context = (Map) tarsInputStream.read(cache_context, 9, false);
        if (cache_status == null) {
            HashMap hashMap2 = new HashMap();
            hashMap2.put("", "");
            cache_status = hashMap2;
        }
        this.status = (Map) tarsInputStream.read(cache_status, 10, false);
    }

    public void writeTo(TarsOutputStream tarsOutputStream) {
        tarsOutputStream.write(this.version, 1);
        tarsOutputStream.write(this.packetType, 2);
        tarsOutputStream.write(this.messageType, 3);
        tarsOutputStream.write(this.requestId, 4);
        if (this.servantName != null) {
            tarsOutputStream.write(this.servantName, 5);
        }
        if (this.funcName != null) {
            tarsOutputStream.write(this.funcName, 6);
        }
        if (this.buffer != null) {
            tarsOutputStream.write(this.buffer, 7);
        }
        tarsOutputStream.write(this.timeout, 8);
        if (this.context != null) {
            tarsOutputStream.write(this.context, 9);
        }
        if (this.status != null) {
            tarsOutputStream.write(this.status, 10);
        }
    }

    public byte getPacketType() {
        return this.packetType;
    }

    public void setPacketType(byte b) {
        this.packetType = b;
    }

    public int getMessageType() {
        return this.messageType;
    }

    public void setMessageType(int i) {
        this.messageType = i;
    }

    public int getRequestId() {
        return this.requestId;
    }

    public void setRequestId(int i) {
        this.requestId = i;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int i) {
        this.timeout = i;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int i) {
        this.version = i;
    }

    public byte[] getBuffer() {
        byte[] bArr = this.buffer;
        if (bArr != null) {
            return bArr;
        }
        Intrinsics.throwUninitializedPropertyAccessException("buffer");
        throw null;
    }

    public void setBuffer(@NotNull byte[] bArr) {
        Intrinsics.checkNotNullParameter(bArr, "<set-?>");
        this.buffer = bArr;
    }

    @NotNull
    public String getFuncName() {
        String str = this.funcName;
        if (str != null) {
            return str;
        }
        Intrinsics.throwUninitializedPropertyAccessException("funcName");
        throw null;
    }

    public void setFuncName(@NotNull String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.funcName = str;
    }

    @NotNull
    public String getServantName() {
        String str = this.servantName;
        if (str != null) {
            return str;
        }
        Intrinsics.throwUninitializedPropertyAccessException("servantName");
        throw null;
    }

    public void setServantName(@NotNull String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.servantName = str;
    }

    @NotNull
    public Map<String, String> getStatus() {
        Map<String, String> map = this.status;
        if (map != null) {
            return map;
        }
        Intrinsics.throwUninitializedPropertyAccessException("status");
        throw null;
    }

    public void setStatus(@NotNull Map<String, String> map) {
        Intrinsics.checkNotNullParameter(map, "<set-?>");
        this.status = map;
    }

    @NotNull
    public Map<String, String> getContext() {
        Map<String, String> map = this.context;
        if (map != null) {
            return map;
        }
        Intrinsics.throwUninitializedPropertyAccessException("context");
        throw null;
    }

    public void setContext(@NotNull Map<String, String> map) {
        Intrinsics.checkNotNullParameter(map, "<set-?>");
        this.context = map;
    }
}