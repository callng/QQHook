package moe.ore.txhook.helper;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

public class MD5 {
    static final byte[] PADDING = {Byte.MIN_VALUE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final byte[] buffer = new byte[64];
    private final long[] count = new long[2];
    private final byte[] digest = new byte[16];
    private final long[] state = new long[4];

    public MD5() {
        md5Init();
    }

    public static String hexDigest(String str) {
        if (str == null) {
            return "";
        }
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(str.getBytes());
            return cha(cArr, instance);
        } catch (Exception unused) {
            return null;
        }
    }

    @NotNull
    private static String cha(char[] cArr, MessageDigest instance) {
        byte[] digest = instance.digest();
        char[] cArr2 = new char[32];
        int i = 0;
        for (int i2 = 0; i2 < 16; i2++) {
            byte b = digest[i2];
            int i3 = i + 1;
            cArr2[i] = cArr[(b >>> 4) & 15];
            i = i3 + 1;
            cArr2[i3] = cArr[b & 15];
        }
        return new String(cArr2);
    }

    public static String hexDigest(byte[] bArr) {
        if (bArr == null) {
            return "";
        }
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(bArr);
            return cha(cArr, instance);
        } catch (Exception unused) {
            return null;
        }
    }

    public static long b2iu(byte b) {
        return b < 0 ? (long) (b & 255) : (long) b;
    }

    public static String byteHEX(byte b) {
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        return new String(new char[]{cArr[(b >>> 4) & 15], cArr[b & 15]});
    }

    public static byte[] toMD5Byte(byte[] bArr) {
        return new MD5().getMD5(bArr);
    }

    public static byte[] toMD5Byte(String str) {
        byte[] bytes;
        try {
            bytes = str.getBytes("ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            bytes = str.getBytes();
        }
        return new MD5().getMD5(bytes);
    }

    public static byte[] toMD5Byte(InputStream inputStream, long j) {
        return new MD5().getMD5(inputStream, j);
    }

    public static String toMD5(byte[] bArr) {
        byte[] md5 = new MD5().getMD5(bArr);
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            str.append(byteHEX(md5[i]));
        }
        return str.toString();
    }

    public static String toMD5(String str) {
        byte[] bytes;
        try {
            bytes = str.getBytes("ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            bytes = str.getBytes();
        }
        byte[] md5 = new MD5().getMD5(bytes);
        StringBuilder str2 = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            str2.append(byteHEX(md5[i]));
        }
        return str2.toString();
    }

    public static String getMD5String(byte[] bArr) {
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            MessageDigest instance = MessageDigest.getInstance("MD5");
            instance.update(bArr);
            return cha(cArr, instance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getFileMD5(File file) {
        if (file == null) {
            return "";
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bArr = new byte[8192];
            char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
            try {
                MessageDigest instance = MessageDigest.getInstance("MD5");
                while (true) {
                    int read = fileInputStream.read(bArr, 0, bArr.length);
                    if (read == -1) {
                        break;
                    }
                    instance.update(bArr, 0, read);
                }
                fileInputStream.close();
                return cha(cArr, instance);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e2) {
            return "";
        }
    }

    public byte[] getMD5(byte[] bArr) {
        md5Init();
        md5Update(new ByteArrayInputStream(bArr), bArr.length);
        md5Final();
        return this.digest;
    }

    public byte[] getMD5(InputStream inputStream, long j) {
        md5Init();
        if (!md5Update(inputStream, j)) {
            return new byte[16];
        }
        md5Final();
        return this.digest;
    }

    private void md5Init() {
        this.count[0] = 0;
        this.count[1] = 0;
        this.state[0] = 0x67452301;
        this.state[1] = 4023233417L;
        this.state[2] = 2562383102L;
        this.state[3] = 0x10325476;
    }

    private long F(long j, long j2, long j3) {
        return (j & j2) | ((~j) & j3);
    }

    private long G(long j, long j2, long j3) {
        return (j & j3) | ((~j3) & j2);
    }

    private long H(long j, long j2, long j3) {
        return (j ^ j2) ^ j3;
    }

    private long I(long j, long j2, long j3) {
        return ((~j3) | j) ^ j2;
    }

    private long FF(long j, long j2, long j3, long j4, long j5, long j6, long j7) {
        long F = F(j2, j3, j4) + j5 + j7 + j;
        return ((((int) F) >>> ((int) (32 - j6))) | ((long) ((int) F) << ((int) j6))) + j2;
    }

    private long GG(long j, long j2, long j3, long j4, long j5, long j6, long j7) {
        long G = G(j2, j3, j4) + j5 + j7 + j;
        return ((((int) G) >>> ((int) (32 - j6))) | ((long) ((int) G) << ((int) j6))) + j2;
    }

    private long HH(long j, long j2, long j3, long j4, long j5, long j6, long j7) {
        long H = H(j2, j3, j4) + j5 + j7 + j;
        return ((((int) H) >>> ((int) (32 - j6))) | ((long) ((int) H) << ((int) j6))) + j2;
    }

    private long II(long j, long j2, long j3, long j4, long j5, long j6, long j7) {
        long I = I(j2, j3, j4) + j5 + j7 + j;
        return ((((int) I) >>> ((int) (32 - j6))) | ((long) ((int) I) << ((int) j6))) + j2;
    }

    private boolean md5Update(InputStream inputStream, long j) {
        int i;
        byte[] bArr = new byte[64];
        int i2 = ((int) (this.count[0] >>> 3)) & 63;
        long[] jArr = this.count;
        long j2 = jArr[0] + (j << 3);
        jArr[0] = j2;
        if (j2 < (j << 3)) {
            long[] jArr2 = this.count;
            jArr2[1] = jArr2[1] + 1;
        }
        long[] jArr3 = this.count;
        jArr3[1] = jArr3[1] + (j >>> 29);
        int i3 = 64 - i2;
        if (j >= ((long) i3)) {
            byte[] bArr2 = new byte[i3];
            try {
                inputStream.read(bArr2, 0, i3);
                md5Memcpy(this.buffer, bArr2, i2, 0, i3);
                md5Transform(this.buffer);
                i = i3;
                while (((long) (i + 63)) < j) {
                    try {
                        inputStream.read(bArr);
                        md5Transform(bArr);
                        i += 64;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                i2 = 0;
            } catch (Exception e2) {
                e2.printStackTrace();
                return false;
            }
        } else {
            i = 0;
        }
        byte[] bArr3 = new byte[((int) (j - ((long) i)))];
        try {
            inputStream.read(bArr3);
            md5Memcpy(this.buffer, bArr3, i2, 0, bArr3.length);
            return true;
        } catch (Exception e3) {
            e3.printStackTrace();
            return false;
        }
    }

    private void md5Update(byte[] bArr, int i) {
        int i2 = 0;
        byte[] bArr2 = new byte[64];
        int i3 = ((int) (this.count[0] >>> 3)) & 63;
        long[] jArr = this.count;
        long j = jArr[0] + ((long) i << 3);
        jArr[0] = j;
        if (j < ((long) i << 3)) {
            long[] jArr2 = this.count;
            jArr2[1] = jArr2[1] + 1;
        }
        long[] jArr3 = this.count;
        jArr3[1] = jArr3[1] + ((long) (i >>> 29));
        int i4 = 64 - i3;
        if (i >= i4) {
            md5Memcpy(this.buffer, bArr, i3, 0, i4);
            md5Transform(this.buffer);
            while (i4 + 63 < i) {
                md5Memcpy(bArr2, bArr, 0, i4, 64);
                md5Transform(bArr2);
                i4 += 64;
            }
            i3 = 0;
            i2 = i4;
        }
        md5Memcpy(this.buffer, bArr, i3, i2, i - i2);
    }

    private void md5Final() {
        byte[] bArr = new byte[8];
        Encode(bArr, this.count, 8);
        int i = ((int) (this.count[0] >>> 3)) & 63;
        md5Update(PADDING, i < 56 ? 56 - i : 120 - i);
        md5Update(bArr, 8);
        Encode(this.digest, this.state, 16);
    }

    private void md5Memcpy(byte[] bArr, byte[] bArr2, int i, int i2, int i3) {
        if (i3 >= 0) System.arraycopy(bArr2, i2, bArr, i, i3);
    }

    private void md5Transform(byte[] bArr) {
        long j = this.state[0];
        long j2 = this.state[1];
        long j3 = this.state[2];
        long j4 = this.state[3];
        long[] jArr = new long[16];
        Decode(jArr, bArr, 64);
        long FF = FF(j, j2, j3, j4, jArr[0], 7, 3614090360L);
        long FF2 = FF(j4, FF, j2, j3, jArr[1], 12, 3905402710L);
        long FF3 = FF(j3, FF2, FF, j2, jArr[2], 17, 0x242070db);
        long FF4 = FF(j2, FF3, FF2, FF, jArr[3], 22, 3250441966L);
        long FF5 = FF(FF, FF4, FF3, FF2, jArr[4], 7, 4118548399L);
        long FF6 = FF(FF2, FF5, FF4, FF3, jArr[5], 12, 0x4787c62a);
        long FF7 = FF(FF3, FF6, FF5, FF4, jArr[6], 17, 2821735955L);
        long FF8 = FF(FF4, FF7, FF6, FF5, jArr[7], 22, 4249261313L);
        long FF9 = FF(FF5, FF8, FF7, FF6, jArr[8], 7, 0x698098d8);
        long FF10 = FF(FF6, FF9, FF8, FF7, jArr[9], 12, 2336552879L);
        long FF11 = FF(FF7, FF10, FF9, FF8, jArr[10], 17, 4294925233L);
        long FF12 = FF(FF8, FF11, FF10, FF9, jArr[11], 22, 2304563134L);
        long FF13 = FF(FF9, FF12, FF11, FF10, jArr[12], 7, 0x6b901122);
        long FF14 = FF(FF10, FF13, FF12, FF11, jArr[13], 12, 4254626195L);
        long FF15 = FF(FF11, FF14, FF13, FF12, jArr[14], 17, 2792965006L);
        long FF16 = FF(FF12, FF15, FF14, FF13, jArr[15], 22, 0x49b40821);
        long GG = GG(FF13, FF16, FF15, FF14, jArr[1], 5, 4129170786L);
        long GG2 = GG(FF14, GG, FF16, FF15, jArr[6], 9, 3225465664L);
        long GG3 = GG(FF15, GG2, GG, FF16, jArr[11], 14, 0x265e5a51);
        long GG4 = GG(FF16, GG3, GG2, GG, jArr[0], 20, 3921069994L);
        long GG5 = GG(GG, GG4, GG3, GG2, jArr[5], 5, 3593408605L);
        long GG6 = GG(GG2, GG5, GG4, GG3, jArr[10], 9, 0x02441453);
        long GG7 = GG(GG3, GG6, GG5, GG4, jArr[15], 14, 3634488961L);
        long GG8 = GG(GG4, GG7, GG6, GG5, jArr[4], 20, 3889429448L);
        long GG9 = GG(GG5, GG8, GG7, GG6, jArr[9], 5, 0x21e1cde6);
        long GG10 = GG(GG6, GG9, GG8, GG7, jArr[14], 9, 3275163606L);
        long GG11 = GG(GG7, GG10, GG9, GG8, jArr[3], 14, 4107603335L);
        long GG12 = GG(GG8, GG11, GG10, GG9, jArr[8], 20, 0x455a14ed);
        long GG13 = GG(GG9, GG12, GG11, GG10, jArr[13], 5, 2850285829L);
        long GG14 = GG(GG10, GG13, GG12, GG11, jArr[2], 9, 4243563512L);
        long GG15 = GG(GG11, GG14, GG13, GG12, jArr[7], 14, 0x676f02d9);
        long GG16 = GG(GG12, GG15, GG14, GG13, jArr[12], 20, 2368359562L);
        long HH = HH(GG13, GG16, GG15, GG14, jArr[5], 4, 4294588738L);
        long HH2 = HH(GG14, HH, GG16, GG15, jArr[8], 11, 2272392833L);
        long HH3 = HH(GG15, HH2, HH, GG16, jArr[11], 16, 0x6d9d6122);
        long HH4 = HH(GG16, HH3, HH2, HH, jArr[14], 23, 4259657740L);
        long HH5 = HH(HH, HH4, HH3, HH2, jArr[1], 4, 2763975236L);
        long HH6 = HH(HH2, HH5, HH4, HH3, jArr[4], 11, 0x4bdecfa9);
        long HH7 = HH(HH3, HH6, HH5, HH4, jArr[7], 16, 4139469664L);
        long HH8 = HH(HH4, HH7, HH6, HH5, jArr[10], 23, 3200236656L);
        long HH9 = HH(HH5, HH8, HH7, HH6, jArr[13], 4, 0x289b7ec6);
        long HH10 = HH(HH6, HH9, HH8, HH7, jArr[0], 11, 3936430074L);
        long HH11 = HH(HH7, HH10, HH9, HH8, jArr[3], 16, 3572445317L);
        long HH12 = HH(HH8, HH11, HH10, HH9, jArr[6], 23, 0x04881d05);
        long HH13 = HH(HH9, HH12, HH11, HH10, jArr[9], 4, 3654602809L);
        long HH14 = HH(HH10, HH13, HH12, HH11, jArr[12], 11, 3873151461L);
        long HH15 = HH(HH11, HH14, HH13, HH12, jArr[15], 16, 0x1fa27cf8);
        long HH16 = HH(HH12, HH15, HH14, HH13, jArr[2], 23, 3299628645L);
        long II = II(HH13, HH16, HH15, HH14, jArr[0], 6, 4096336452L);
        long II2 = II(HH14, II, HH16, HH15, jArr[7], 10, 0x432aff97);
        long II3 = II(HH15, II2, II, HH16, jArr[14], 15, 2878612391L);
        long II4 = II(HH16, II3, II2, II, jArr[5], 21, 4237533241L);
        long II5 = II(II, II4, II3, II2, jArr[12], 6, 0x655b59c3);
        long II6 = II(II2, II5, II4, II3, jArr[3], 10, 2399980690L);
        long II7 = II(II3, II6, II5, II4, jArr[10], 15, 4293915773L);
        long II8 = II(II4, II7, II6, II5, jArr[1], 21, 2240044497L);
        long II9 = II(II5, II8, II7, II6, jArr[8], 6, 0x6fa87e4f);
        long II10 = II(II6, II9, II8, II7, jArr[15], 10, 4264355552L);
        long II11 = II(II7, II10, II9, II8, jArr[6], 15, 2734768916L);
        long II12 = II(II8, II11, II10, II9, jArr[13], 21, 0x4e0811a1);
        long II13 = II(II9, II12, II11, II10, jArr[4], 6, 4149444226L);
        long II14 = II(II10, II13, II12, II11, jArr[11], 10, 3174756917L);
        long II15 = II(II11, II14, II13, II12, jArr[2], 15, 0x2ad7d2bb);
        long II16 = II(II12, II15, II14, II13, jArr[9], 21, 3951481745L);
        long[] jArr2 = this.state;
        jArr2[0] = jArr2[0] + II13;
        long[] jArr3 = this.state;
        jArr3[1] = II16 + jArr3[1];
        long[] jArr4 = this.state;
        jArr4[2] = jArr4[2] + II15;
        long[] jArr5 = this.state;
        jArr5[3] = jArr5[3] + II14;
    }

    private void Encode(byte[] bArr, long[] jArr, int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3 += 4) {
            bArr[i3] = (byte) ((int) (jArr[i2] & 255));
            bArr[i3 + 1] = (byte) ((int) ((jArr[i2] >>> 8) & 255));
            bArr[i3 + 2] = (byte) ((int) ((jArr[i2] >>> 16) & 255));
            bArr[i3 + 3] = (byte) ((int) ((jArr[i2] >>> 24) & 255));
            i2++;
        }
    }

    private void Decode(long[] jArr, byte[] bArr, int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3 += 4) {
            jArr[i2] = b2iu(bArr[i3]) | (b2iu(bArr[i3 + 1]) << 8) | (b2iu(bArr[i3 + 2]) << 16) | (b2iu(bArr[i3 + 3]) << 24);
            i2++;
        }
    }
}
