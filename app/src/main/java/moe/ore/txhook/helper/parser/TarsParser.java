package moe.ore.txhook.helper.parser;

import org.json.JSONException;

import java.lang.reflect.Array;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import moe.ore.tars.TarsBase;
import moe.ore.tars.exc.TarsDecodeException;
import moe.ore.txhook.helper.ByteArrayExtKt;
import moe.ore.txhook.helper.DebugUtil;
import moe.ore.txhook.helper.HexUtil;

public class TarsParser {
    Charset sServerEncoding = StandardCharsets.UTF_8;
    private ByteBuffer bs;

    public TarsParser() {

    }

    public TarsParser(ByteBuffer bs) {
        this.bs = bs;
    }

    public TarsParser(byte[] bs) {
        this.bs = ByteBuffer.wrap(bs);
    }

    public TarsParser(byte[] bs, int pos) {
        this.bs = ByteBuffer.wrap(bs);
        this.bs.position(pos);
    }

    public static int readHead(TarsParser.HeadData hd, ByteBuffer bb) {
        byte b = bb.get();
        hd.type = (byte) (b & 15);
        hd.tag = ((b & (15 << 4)) >> 4);
        if (hd.tag == 15) {
            hd.tag = (bb.get() & 0x00ff);
            return 2;
        }
        return 1;
    }

    public NewJsonObject start() {

        NewJsonObject object = new NewJsonObject();

        while (bs.hasRemaining()) {
            HeadData headData = new HeadData();
            this.readHead(headData);

            try {
                analyzeData(headData.tag, headData.type, object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return object;
    }

    private void analyzeData(int tag, byte type, NewJsonObject object) throws JSONException {
        object.put(String.valueOf(tag), getValue(type));
        /*
        switch (type) {
            case 0:
                break;
            case 1:
                object.put(String.valueOf(tag), bs.getShort());
                break;
            case 2:
                object.put(String.valueOf(tag), bs.getInt());
                break;
            case 3:
                object.put(String.valueOf(tag), bs.getLong());
                break;
            case 4:
                object.put(String.valueOf(tag), bs.getFloat());
                break;
            case 5:
                object.put(String.valueOf(tag), bs.getDouble());
            case 6:
                byte tmp = bs.get();
                int t2 = tmp;
                if(tmp < 0)
                    t2 = tmp + 256;
                byte[] arr = new byte[t2];
                bs.get(arr);
                object.put(String.valueOf(tag), new String(arr));
                break;
            case 7:
                int len = bs.getInt();
                if (len > TarsBase.MAX_STRING_LENGTH || len < 0)
                    throw new TarsDecodeException("String too long: " + len);
                byte[] ss = new byte[len];
                //bs.get(ss);
                bs.get(ss);
                String s = new String(ss, sServerEncoding);
                object.put(String.valueOf(tag), s);
                break;
            case 8: {
                int size = read((int)0, 0, true);
                if (size < 0)
                    throw new TarsDecodeException("size is < 0 " + size);
                JSONObject jsonObject = new JSONObject();
                for (int i = 0; i < size; i++) {
                    Object key;
                    { // key
                        HeadData headData = new HeadData();
                        readHead(headData);
                        switch (headData.type) {
                            case 0:
                                key = bs.get();
                                break;
                            case 1:
                                key = bs.getShort();
                                break;
                            case 2:
                                key = bs.getInt();
                                break;
                            case 3:
                                key = bs.getLong();
                                break;
                            case 4:
                                key = bs.getFloat();
                                break;
                            case 5:
                                key = bs.getDouble();
                            case 6:
                                byte tt = bs.get();
                                int t3 = tt;
                                if(tt < 0)
                                    t3 = tt + 256;
                                byte[] ar = new byte[t3];
                                bs.get(ar);
                                key = new String(ar);
                                break;
                            case 7:
                                int si = bs.getInt();
                                if (si > TarsBase.MAX_STRING_LENGTH || si < 0)
                                    throw new TarsDecodeException("String too long: " + si);
                                byte[] sss = new byte[si];
                                bs.get(sss);
                                key = new String(sss, sServerEncoding);
                                break;
                        }
                    }
                    jsonObject.put(String.valueOf(readString(0, true)), readString(1, true));
                }
                object.put(String.valueOf(tag), jsonObject);
                break;
            }
            case 9: {
                JSONObject jsonObject = new JSONObject();
                int size = read((int)0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                for (int i = 0; i < size; ++i) {
                    HeadData hd = new HeadData();
                    readHead(hd);
                    analyzeData(hd.tag, hd.type, jsonObject);
                }
                object.put(String.valueOf(tag), jsonObject);
                break;
            }
            case 10: {
                JSONObject jsonObject = new JSONObject();
                int i = 0;
                while (true) {
                    try {
                        HeadData hd = new HeadData();
                        readHead(hd);
                        if (i <= hd.tag) {
                            i = hd.tag;
                        } else {
                            break;
                        }
                        analyzeData(hd.tag, hd.type, jsonObject);
                        i++;
                    } catch (Exception e) {
                        break;
                    }
                }
                object.put(String.valueOf(tag), jsonObject);
                break;
            }
            case 12:
                object.put(String.valueOf(tag), 0);
                break;
            case 13: {
                HeadData hd = new HeadData();
                readHead(hd);
                if (hd.type != 0) {
                    throw new TarsDecodeException("simple list must be bytearray");
                }
                int size = read((int)0, 0, true);
                if (size < 0 || size > this.bs.capacity()) throw new TarsDecodeException("size invalid: " + size);
                byte[] b = new byte[size];
                //bs.get(ss);
                bs.get(b);
                object.put(String.valueOf(tag), "[hex]" + HexUtil.Bin2Hex(b));
            }
            break;
            default: throw new TarsDecodeException("invalid type.");
        }*/
    }

    private Object getValue(byte type) throws JSONException {
        switch (type) {
            case 0:
                return bs.get();
            case 1:
                return bs.getShort();
            case 2:
                return bs.getInt();
            case 3:
                return bs.getLong();
            case 4:
                return bs.getFloat();
            case 5:
                return bs.getDouble();
            case 6:
                byte tmp = bs.get();
                int t2 = tmp;
                if (tmp < 0)
                    t2 = tmp + 256;

                byte[] arr = new byte[t2];
                bs.get(arr);

                return new String(arr);
            case 7:
                int len = bs.getInt();
                if (len > TarsBase.MAX_STRING_LENGTH || len < 0)
                    throw new TarsDecodeException("String too long: " + len);
                byte[] ss = new byte[len];
                //bs.get(ss);
                bs.get(ss);

                return new String(ss, sServerEncoding);
            case 8: {
                int size = read(0, 0, true);
                if (size < 0)
                    throw new TarsDecodeException("size is < 0 " + size);

                NewJsonObject jsonObject = new NewJsonObject();

                for (int i = 0; i < size; i++) {
                    Object key;
                    { // key
                        HeadData headData = new HeadData();
                        readHead(headData);

                        key = getValue(headData.type);
                    }

                    Object value;
                    { // value
                        HeadData headData = new HeadData();
                        readHead(headData);

                        value = getValue(headData.type);
                    }

                    jsonObject.put(String.valueOf(key), value);
                }

                return jsonObject;
            }
            case 9: {
                NewJsonObject jsonObject = new NewJsonObject();
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                for (int i = 0; i < size; ++i) {

                    HeadData hd = new HeadData();
                    readHead(hd);

                    analyzeData(hd.tag, hd.type, jsonObject);
                }
                return jsonObject;
            }
            case 10: {
                NewJsonObject jsonObject = new NewJsonObject();

                int i = 0;
                while (true) {
                    try {
                        HeadData hd = new HeadData();
                        readHead(hd);
                        if (i <= hd.tag) {
                            i = hd.tag;
                        } else {
                            break;
                        }
                        analyzeData(hd.tag, hd.type, jsonObject);
                        i++;
                    } catch (Exception e) {
                        break;
                    }
                }
                return jsonObject;
            }
            case 12:
                return 0;
            case 13: {
                HeadData hd = new HeadData();
                readHead(hd);
                if (hd.type != 0) {
                    throw new TarsDecodeException("simple list must be bytearray");
                }
                int size = read(0, 0, true);
                if (size < 0 || size > this.bs.capacity())
                    throw new TarsDecodeException("size invalid: " + size);

                byte[] b = new byte[size];
                //bs.get(ss);
                bs.get(b);

                return ("[hex]" + HexUtil.Bin2Hex(b));
            }
            default:
                throw new TarsDecodeException("invalid type.");
        }
    }

    public void warp(byte[] bs) {
        wrap(bs);
    }

    public void wrap(byte[] bs) {
        // this.bs = ByteBuffer.wrap(bs);
        this.bs = ByteBuffer.wrap(bs);
    }

    public void readHead(TarsParser.HeadData hd) {
        readHead(hd, bs);
    }

    private int peakHead(TarsParser.HeadData hd) {
        return readHead(hd, bs.duplicate());
    }

    private void skip(int len) {
        bs.position(bs.position() + len);
        // bs.readerIndex(bs.readerIndex() + len);
    }

    public boolean skipToTag(int tag) {
        try {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            while (true) {
                int len = peakHead(hd);
                if (hd.type == TarsBase.STRUCT_END) {
                    return false;
                }
                if (tag <= hd.tag) return tag == hd.tag;
                skip(len);
                skipField(hd.type);
            }
        } catch (TarsDecodeException | BufferUnderflowException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void skipToStructEnd() {
        TarsParser.HeadData hd = new TarsParser.HeadData();
        do {
            readHead(hd);
            skipField(hd.type);
        } while (hd.type != TarsBase.STRUCT_END);
    }

    private void skipField() {
        TarsParser.HeadData hd = new TarsParser.HeadData();
        readHead(hd);
        skipField(hd.type);
    }

    private void skipField(byte type) {
        switch (type) {
            case TarsBase.BYTE:
                skip(1);
                break;
            case TarsBase.SHORT:
                skip(2);
                break;
            case TarsBase.INT:
            case TarsBase.FLOAT:
                skip(4);
                break;
            case TarsBase.LONG:
            case TarsBase.DOUBLE:
                skip(8);
                break;
            case TarsBase.STRING1: {
                //int len = bs.get();
                int len = bs.get();
                if (len < 0) len += 256;
                skip(len);
                break;
            }
            case TarsBase.STRING4: {
                skip(bs.getInt());
                break;
            }
            case TarsBase.MAP: {
                int size = read(0, 0, true);
                for (int i = 0; i < size * 2; ++i)
                    skipField();
                break;
            }
            case TarsBase.LIST: {
                int size = read(0, 0, true);
                for (int i = 0; i < size; ++i)
                    skipField();
                break;
            }
            case TarsBase.SIMPLE_LIST: {
                TarsParser.HeadData hd = new TarsParser.HeadData();
                readHead(hd);
                if (hd.type != TarsBase.BYTE) {
                    throw new TarsDecodeException("skipField with invalid type, type value: " + type + ", " + hd.type);
                }
                int size = read(0, 0, true);
                skip(size);
                break;
            }
            case TarsBase.STRUCT_BEGIN:
                skipToStructEnd();
                break;
            case TarsBase.STRUCT_END:
            case TarsBase.ZERO_TAG:
                break;
            default:
                throw new TarsDecodeException("invalid type.");
        }
    }

    public boolean read(boolean b, int tag, boolean isRequire) {
        byte c = read((byte) 0x0, tag, isRequire);
        return c != 0;
    }

    public byte read(byte c, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.ZERO_TAG:
                    c = 0x0;
                    break;
                case TarsBase.BYTE:
                    c = bs.get();
                    break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return c;
    }

    public short read(short n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.ZERO_TAG:
                    n = 0;
                    break;
                case TarsBase.BYTE:
                    n = bs.get();
                    break;
                case TarsBase.SHORT:
                    n = bs.getShort();
                    break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return n;
    }

    public int read(int n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.ZERO_TAG:
                    n = 0;
                    break;
                case TarsBase.BYTE:
                    n = bs.get();
                    break;
                case TarsBase.SHORT:
                    n = bs.getShort();
                    break;
                case TarsBase.INT:
                    n = bs.getInt();
                    break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return n;
    }

    public long read(long n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.ZERO_TAG:
                    n = 0;
                    break;
                case TarsBase.BYTE:
                    n = bs.get();
                    break;
                case TarsBase.SHORT:
                    n = bs.getShort();
                    break;
                case TarsBase.INT:
                    n = bs.getInt();
                    break;
                case TarsBase.LONG:
                    n = bs.getLong();
                    break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return n;
    }

    public float read(float n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.ZERO_TAG:
                    n = 0;
                    break;
                case TarsBase.FLOAT:
                    n = bs.getFloat();
                    break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return n;
    }

    public double read(double n, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.ZERO_TAG:
                    n = 0;
                    break;
                case TarsBase.FLOAT:
                    n = bs.getFloat();
                    break;
                case TarsBase.DOUBLE:
                    n = bs.getDouble();
                    break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return n;
    }

    public String readByteString(String s, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.STRING1: {
                    int len = bs.get();
                    if (len < 0) len += 256;
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.get(ss);
                    s = ByteArrayExtKt.toHexString(ss);
                }
                break;
                case TarsBase.STRING4: {
                    int len = bs.getInt();
                    if (len > TarsBase.MAX_STRING_LENGTH || len < 0)
                        throw new TarsDecodeException("String too long: " + len);
                    byte[] ss = new byte[len];
                    bs.get(ss);
                    s = ByteArrayExtKt.toHexString(ss);
                }
                break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return s;
    }

    public String read(String s, int tag, boolean isRequire) {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.STRING1: {
                    int len = bs.get();
                    if (len < 0) len += 256;
                    byte[] ss = new byte[len];
                    bs.get(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                case TarsBase.STRING4: {
                    int len = bs.getInt();
                    if (len > TarsBase.MAX_STRING_LENGTH || len < 0)
                        throw new TarsDecodeException("String too long: " + len);
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.get(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return s;
    }

    public String readString(int tag, boolean isRequire) throws JSONException {
        String s = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.STRING1: {
                    int len = bs.get();
                    if (len < 0) len += 256;
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.get(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                case TarsBase.STRING4: {
                    int len = bs.getInt();
                    if (len > TarsBase.MAX_STRING_LENGTH || len < 0)
                        throw new TarsDecodeException("String too long: " + len);
                    byte[] ss = new byte[len];
                    //bs.get(ss);
                    bs.get(ss);
                    s = new String(ss, sServerEncoding);
                }
                break;
                /*
                case 8: {
                    JSONObject object = new JSONObject();
                    int size = read((int)0, 0, true);
                    if (size < 0) {
                        throw new TarsDecodeException("size too small: " + size);
                    }
                    for (int i = 0; i < size; i++) {
                        object.put(String.valueOf(readString(0, true)), readString(1, true));
                    }
                    return object;
                }
                case 13: {
                    HeadData headData = new HeadData();
                    readHead(headData);
                    if (hd.type != 0) {
                        throw new TarsDecodeException("simple list must be bytearray");
                    }
                    int size = read((int)0, 0, true);
                    if (size < 0 || size > this.bs.capacity()) throw new TarsDecodeException("size invalid: " + size);
                    byte[] b = new byte[size];
                    //bs.get(ss);
                    bs.get(b);
                    return "[hex]" + HexUtil.Bin2Hex(b);
                }*/
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch. real type is " + hd.type);
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return s;
    }

    public String[] read(String[] s, int tag, boolean isRequire) throws JSONException {
        return readArray(s, tag, isRequire);
    }

    public Map<String, String> readStringMap(int tag, boolean isRequire) throws JSONException {
        HashMap<String, String> mr = new HashMap<>();
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.MAP) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                for (int i = 0; i < size; ++i) {
                    String k = String.valueOf(readString(0, true));
                    String v = String.valueOf(readString(1, true));
                    mr.put(k, v);
                }
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return mr;
    }

    public <K, V> HashMap<K, V> readMap(Map<K, V> m, int tag, boolean isRequire) throws JSONException {
        return (HashMap<K, V>) readMap(new HashMap<>(), m, tag, isRequire);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <K, V> Map<K, V> readMap(Map<K, V> mr, Map<K, V> m, int tag, boolean isRequire) throws JSONException {
        if (m == null || m.isEmpty()) {
            return new HashMap();
        }

        Iterator<Map.Entry<K, V>> it = m.entrySet().iterator();
        Map.Entry<K, V> en = it.next();
        K mk = en.getKey();
        V mv = en.getValue();

        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.MAP) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                for (int i = 0; i < size; ++i) {
                    K k = (K) read(mk, 0, true);
                    V v = (V) read(mv, 1, true);
                    mr.put(k, v);
                }
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return mr;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List readList(int tag, boolean isRequire) {
        List lr = new ArrayList();
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                for (int i = 0; i < size; ++i) {
                    TarsParser.HeadData subH = new TarsParser.HeadData();
                    readHead(subH);
                    switch (subH.type) {
                        case TarsBase.BYTE:
                            skip(1);
                            break;
                        case TarsBase.SHORT:
                            skip(2);
                            break;
                        case TarsBase.INT:
                        case TarsBase.FLOAT:
                            skip(4);
                            break;
                        case TarsBase.LONG:
                        case TarsBase.DOUBLE:
                            skip(8);
                            break;
                        case TarsBase.STRING1: {
                            int len = bs.get();
                            if (len < 0) len += 256;
                            skip(len);
                        }
                        break;
                        case TarsBase.STRING4: {
                            skip(bs.getInt());
                        }
                        break;
                        case TarsBase.MAP:
                        case TarsBase.LIST: {

                        }
                        break;
                        case TarsBase.STRUCT_BEGIN:
                            try {
                                /*
                                Class<?> newoneClass = Class.forName(TarsBase.class.getName());
                                Constructor<?> cons = newoneClass.getConstructor();
                                TarsBase struct = (TarsBase) cons.newInstance();
                                struct.readFrom(this);
                                skipToStructEnd();
                                lr.add(struct);*/
                            } catch (Exception e) {
                                throw new TarsDecodeException("[" + tag + "]type mismatch." + e);
                            }
                            break;
                        case TarsBase.ZERO_TAG:
                            lr.add(0);
                            break;
                        default:
                            throw new TarsDecodeException("[" + tag + "]type mismatch.");
                    }
                }
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public boolean[] read(boolean[] l, int tag, boolean isRequire) {
        boolean[] lr = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                lr = new boolean[size];
                for (int i = 0; i < size; ++i)
                    lr[i] = read(lr[0], 0, true);
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public byte[] read(byte[] l, int tag, boolean isRequire) {
        byte[] lr = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            switch (hd.type) {
                case TarsBase.SIMPLE_LIST: {
                    TarsParser.HeadData hh = new TarsParser.HeadData();
                    readHead(hh);
                    if (hh.type != TarsBase.BYTE) {
                        throw new TarsDecodeException("type mismatch, tag: " + tag + ", type: " + hd.type + ", " + hh.type);
                    }
                    int size = read(0, 0, true);
                    if (size < 0)
                        throw new TarsDecodeException("invalid size, tag: " + tag + ", type: " + hd.type + ", " + hh.type + ", size: " + size);
                    lr = new byte[size];
                    //bs.get(lr);
                    bs.get(lr);
                    break;
                }
                case TarsBase.LIST: {
                    int size = read(0, 0, true);
                    if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                    lr = new byte[size];
                    for (int i = 0; i < size; ++i)
                        lr[i] = read(lr[0], 0, true);
                    break;
                }
                default:
                    throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public short[] read(short[] l, int tag, boolean isRequire) {
        short[] lr = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                lr = new short[size];
                for (int i = 0; i < size; ++i)
                    lr[i] = read(lr[0], 0, true);
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public int[] read(int[] l, int tag, boolean isRequire) {
        int[] lr = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                lr = new int[size];
                for (int i = 0; i < size; ++i)
                    lr[i] = read(lr[0], 0, true);
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public long[] read(long[] l, int tag, boolean isRequire) {
        long[] lr = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                lr = new long[size];
                for (int i = 0; i < size; ++i)
                    lr[i] = read(lr[0], 0, true);
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public float[] read(float[] l, int tag, boolean isRequire) {
        float[] lr = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                lr = new float[size];
                for (int i = 0; i < size; ++i)
                    lr[i] = read(lr[0], 0, true);
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public double[] read(double[] l, int tag, boolean isRequire) {
        double[] lr = null;
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                lr = new double[size];
                for (int i = 0; i < size; ++i)
                    lr[i] = read(lr[0], 0, true);
            } else {
                throw new TarsDecodeException("[" + tag + "]type mismatch.");
            }
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return lr;
    }

    public <T> T[] readArray(T[] l, int tag, boolean isRequire) throws JSONException {
        if (l == null || l.length == 0)
            throw new TarsDecodeException("unable to get type of key and value.");
        return readArrayImpl(l[0], tag, isRequire);
    }

    public <T> List<T> readArray(List<T> l, int tag, boolean isRequire) throws JSONException {
        if (l == null || l.isEmpty()) {
            return new ArrayList<>();
        }
        T[] tt = readArrayImpl(l.get(0), tag, isRequire);
        if (tt == null) return null;
        return new ArrayList<>(Arrays.asList(tt));
    }

    @SuppressWarnings("unchecked")
    private <T> T[] readArrayImpl(T mt, int tag, boolean isRequire) throws JSONException {
        if (skipToTag(tag)) {
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type == TarsBase.LIST) {
                int size = read(0, 0, true);
                if (size < 0) throw new TarsDecodeException("size invalid: " + size);
                T[] lr = (T[]) Array.newInstance(mt.getClass(), size);
                for (int i = 0; i < size; ++i) {
                    T t = (T) read(mt, 0, true);
                    lr[i] = t;
                }
                return lr;
            }
            throw new TarsDecodeException("[" + tag + "]type mismatch.");
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return null;
    }

    public <T extends TarsBase> T read(T o, int tag, boolean isRequire) {
        TarsBase ref = null;
        if (skipToTag(tag)) {
            try {
                ref = o.getClass().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                throw new TarsDecodeException(e.getMessage());
            }

            /*
            TarsParser.HeadData hd = new TarsParser.HeadData();
            readHead(hd);
            if (hd.type != TarsBase.STRUCT_BEGIN) throw new TarsDecodeException("[" + tag + "]type mismatch.");
            ref.readFrom(this);
             */
            skipToStructEnd();
        } else if (isRequire) {
            throw new TarsDecodeException("require field not exist.");
        }
        return DebugUtil.forcedConvert(ref);
    }

    public TarsBase[] read(TarsBase[] o, int tag, boolean isRequire) throws JSONException {
        return readArray(o, tag, isRequire);
    }

    public <O> Object read(O o, int tag, boolean isRequire) throws JSONException {
        if (o instanceof Byte) {
            return read((byte) 0x0, tag, isRequire);
        } else if (o instanceof Boolean) {
            return read(false, tag, isRequire);
        } else if (o instanceof Short) {
            return read((short) 0, tag, isRequire);
        } else if (o instanceof Integer) {
            return read(0, tag, isRequire);
        } else if (o instanceof Long) {
            return read((long) 0, tag, isRequire);
        } else if (o instanceof Float) {
            return read((float) 0, tag, isRequire);
        } else if (o instanceof Double) {
            return read((double) 0, tag, isRequire);
        } else if (o instanceof String) {
            return readString(tag, isRequire);
        } else if (o instanceof Map) {
            return readMap((Map<?, ?>) o, tag, isRequire);
        } else if (o instanceof List) {
            return readArray((List<?>) o, tag, isRequire);
        } else if (o instanceof TarsBase) {
            return read((TarsBase) o, tag, isRequire);
        } else if (o.getClass().isArray()) {
            if (o instanceof byte[] || o instanceof Byte[]) {
                return read((byte[]) null, tag, isRequire);
            } else if (o instanceof boolean[]) {
                return read((boolean[]) null, tag, isRequire);
            } else if (o instanceof short[]) {
                return read((short[]) null, tag, isRequire);
            } else if (o instanceof int[]) {
                return read((int[]) null, tag, isRequire);
            } else if (o instanceof long[]) {
                return read((long[]) null, tag, isRequire);
            } else if (o instanceof float[]) {
                return read((float[]) null, tag, isRequire);
            } else if (o instanceof double[]) {
                return read((double[]) null, tag, isRequire);
            } else {
                return readArray((Object[]) o, tag, isRequire);
            }
        } else {
            throw new RuntimeException("UNKNOWN TYPE...");
        }
    }

    public int setServerEncoding(Charset se) {
        sServerEncoding = se;
        return 0;
    }

    public ByteBuffer getBs() {
        return this.bs;
    }

    public byte[] toByteArray() {
        return bs.array();
    }

    public static class HeadData {
        public byte type;
        public int tag;

        public void clear() {
            type = 0;
            tag = 0;
        }
    }
}