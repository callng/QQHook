package moe.ore.xposed.utils;

import static moe.ore.xposed.utils.PrefsManager.KEY_PUSH_API;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.OptIn;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

import kotlinx.serialization.ExperimentalSerializationApi;
import moe.ore.script.Consist;
import moe.ore.txhook.app.CatchProvider;
import moe.ore.txhook.helper.HexUtil;
import moe.ore.txhook.helper.KotlinExtKt;

public final class HttpUtil {
    private static final Uri URI_GET_TXHOOK_STATE = Uri.parse("content://" + CatchProvider.MY_URI + "/" + Consist.GET_TXHOOK_STATE);
    public static WeakReference<Context> contextWeakReference;
    // private static final Uri URI_GET_TXHOOK_WS_STATE = Uri.parse("content://" + CatchProvider.MY_URI + "/" + Consist.GET_TXHOOK_WS_STATE);
    public static ContentResolver contentResolver;

    public static void sendTo(Uri uri, ContentValues contentValues, int source) {
        try {
            String url = getApiUrl();
            String mode = contentValues.getAsString("mode");
            if (url != null && !url.isEmpty()) {
                String name = switch (mode) {
                    case "send", "receive" -> "packet";
                    default -> mode;
                };
                JsonObject object = new JsonObject();
                for (String key : contentValues.keySet()) {
                    Object value = contentValues.get(key);
                    if (value == null) continue;
                    if (value instanceof Number) {
                        object.addProperty(key, (Number) value);
                    } else if (value instanceof String) {
                        object.addProperty(key, (String) value);
                    } else if (value instanceof Boolean) {
                        object.addProperty(key, (Boolean) value);
                    } else if (value instanceof byte[]) {
                        object.addProperty(key, HexUtil.Bin2Hex((byte[]) value));
                    }
                }
                postTo(url, name, object, source);
            }

            // tryToConnectWS();
            if ("running".equals(contentResolver.getType(URI_GET_TXHOOK_STATE))) {
                contentValues.put("source", source);
                contentResolver.insert(uri, contentValues);
                // 如果查询不到状态就不发了
            }
        } catch (IllegalArgumentException ignored) {

        }
    }

    public static void postTo(String action, JsonObject jsonObject, int source) {
        String url = getApiUrl();
        postTo(url, action, jsonObject, source);
    }

    public static void postTo(String action, String str) {
        String url = getApiUrl();
        postTo(url, action, str);
    }

    public static void postTo(String url, String action, JsonObject jsonObject, int source) {
        if (url != null && !url.isEmpty()) {
            jsonObject.addProperty("source", source);
            moe.ore.android.util.HttpUtil.INSTANCE.postJson("http://" + url + "/" + action, jsonObject.toString());
        }
    }

    public static void postTo(String url, String action, String str) {
        if (url != null && !url.isEmpty()) {
            moe.ore.android.util.HttpUtil.INSTANCE.postJson("http://" + url + "/" + action, str);
        }
    }

    @OptIn(markerClass = ExperimentalSerializationApi.class)
    public static String getApiUrl() {
        String url = null;
        if (PrefsManager.INSTANCE.isInitialized()) {
            url = PrefsManager.INSTANCE.getString(KEY_PUSH_API, "");
        }
        if (contentResolver != null) {
            try {
                Cursor cursor = contentResolver.query(URI_GET_TXHOOK_STATE, null, KEY_PUSH_API, null, null);
                if (cursor != null) {
                    try {
                        Bundle extras = cursor.getExtras();
                        if (extras != null) {
                            url = extras.getString(KEY_PUSH_API);
                        }
                    } catch (Exception ignored) {

                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception ignored) {

            }
        }
        return url;
    }

    public static Object invokeFromObjectMethod(Object from, String mn, Class<?>... parameterTypes) {
        Class<?> c = from.getClass();
        try {
            return c.getDeclaredMethod(mn, parameterTypes).invoke(from);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] castToBytes(Object result) {
        if (result == null) {
            return KotlinExtKt.EMPTY_BYTE_ARRAY;
        }
        if (result instanceof byte[]) {
            return (byte[]) result;
        } else {
            // throw new IllegalArgumentException("Provided object is not a byte array");
            return KotlinExtKt.EMPTY_BYTE_ARRAY;
        }
    }

    @NotNull
    public static String castToString(Object result) {
        if (result == null) {
            return "";
        }
        if (result instanceof String) {
            return (String) result;
        } else {
            return "";
        }
    }
}
