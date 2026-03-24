package moe.ore.tars;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.nio.charset.Charset;

public class TarsBase implements Serializable {
    public static final byte BYTE = 0;
    public static final byte SHORT = 1;
    public static final byte INT = 2;
    public static final byte LONG = 3;
    public static final byte FLOAT = 4;
    public static final byte DOUBLE = 5;
    public static final byte STRING1 = 6;
    public static final byte STRING4 = 7;
    public static final byte MAP = 8;
    public static final byte LIST = 9;
    public static final byte STRUCT_BEGIN = 10;
    public static final byte STRUCT_END = 11;
    public static final byte ZERO_TAG = 12;
    public static final byte SIMPLE_LIST = 13;

    public static final int MAX_STRING_LENGTH = 100 * 1024 * 1024;

    public static String toDisplaySimpleString(TarsBase struct) {
        if (struct == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        struct.displaySimple(sb, 0);
        return sb.toString();
    }

    @NotNull
    public String servantName() {
        return "";
    }

    @NotNull
    public String funcName() {
        return "";
    }

    @NotNull
    public String reqName() {
        return "";
    }

    @NotNull
    public String respName() {
        return "";
    }

    // 非必要实现
    public void writeTo(@NotNull TarsOutputStream output) {

    }

    // 非必要实现
    public void readFrom(@NotNull TarsInputStream input) {

    }

    public void display(StringBuilder sb, int level) {
    }

    public void displaySimple(StringBuilder sb, int level) {
    }

    public TarsBase newInit() {
        return null;
    }

    public void recycle() {

    }

    public boolean containField(String name) {
        return false;
    }

    public Object getFieldByName(String name) {
        return null;
    }

    public void setFieldByName(String name, Object value) {
    }

    public byte[] toByteArray() {
        TarsOutputStream os = new TarsOutputStream();
        try {
            writeTo(os);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] out = os.toByteArray();
        os.close();
        return out;
    }

    public byte[] toByteArray(Charset encoding) {
        TarsOutputStream os = new TarsOutputStream();
        os.setServerEncoding(encoding);
        try {
            writeTo(os);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] out = os.toByteArray();
        os.close();
        return out;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        display(sb, 0);
        return sb.toString();
    }
}