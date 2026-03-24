@file:OptIn(ExperimentalSerializationApi::class)
package moe.ore.txhook.app

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.content.UriMatcher.NO_MATCH
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.AndroKtx
import moe.ore.script.Consist.GET_TEST_DATA
import moe.ore.script.Consist.GET_TXHOOK_STATE
import moe.ore.script.Consist.GET_TXHOOK_WS_STATE
import moe.ore.txhook.app.model.CapturePacket
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY
import moe.ore.xposed.utils.PrefsManager
import moe.ore.xposed.utils.PrefsManager.KEY_PUSH_API

class CatchProvider: ContentProvider() {
    private lateinit var matcher: UriMatcher

    override fun onCreate(): Boolean {
        matcher = UriMatcher(NO_MATCH)
        matcher.addURI(MY_URI, GET_TEST_DATA, 1)
        matcher.addURI(MY_URI, GET_TXHOOK_STATE, 2)
        matcher.addURI(MY_URI, GET_TXHOOK_WS_STATE, 3)

        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        when(selection) {
            KEY_PUSH_API -> {
                return FakeCursor().apply {
                    put(KEY_PUSH_API, PrefsManager.getString(KEY_PUSH_API))
                }
            }
        }
        return null
    }

    override fun getType(uri: Uri): String? {
        return when(matcher.match(uri)) {
            1 -> "TxHookTestData"
            2 -> "running"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        if (AndroKtx.isInit) values?.let { value -> catchHandler?.let { handler ->
            when(value.getAsString("mode")) {
                "md5" -> handler.handleMd5(
                    value.getAsInteger("source"),
                    (value.get("data") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    (value.get("result") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                )
                "tlv.get_buf" -> handler.handleTlvGet(
                    value.getAsInteger("version"),
                    (value.get("data") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    value.getAsInteger("source"),
                )
                "tlv.set_buf" -> handler.handleTlvSet(
                    value.getAsInteger("version"),
                    (value.get("data") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    value.getAsInteger("source"),
                )
                "receive" -> handler.handlePacket(System.currentTimeMillis(), CapturePacket(
                    true,
                    cmd = value.getAsString("cmd"),
                    buffer = (value.get("buffer") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    uin = runCatching { value.getAsString("uin").toLong() }.getOrDefault(0L),
                    seq = value.getAsInteger("seq"),
                    msgCookie = (value.get("msgCookie") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    type = value.getAsString("type"),
                    time = System.currentTimeMillis(),
                    source = value.getAsInteger("source")
                ))
                "send" -> handler.handlePacket(System.currentTimeMillis(), CapturePacket(
                    false,
                    cmd = value.getAsString("cmd"),
                    buffer = (value.get("buffer") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    uin = runCatching { value.getAsString("uin").toLong() }.getOrDefault(0L),
                    seq = value.getAsInteger("seq"),
                    msgCookie = (value.get("msgCookie") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    type = value.getAsString("type"),
                    time = System.currentTimeMillis(),
                    source = value.getAsInteger("source")
                ))
                "tea" -> handler.handleTea(
                    value.getAsBoolean("enc"),
                    (value.get("data") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    (value.get("key") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    (value.get("result") as? ByteArray) ?: EMPTY_BYTE_ARRAY,
                    value.getAsInteger("source")
                )
            }
        } }
        return uri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        return 0
    }

    companion object {
        const val MY_URI = "moe.ore.txhook.catch"

        @JvmStatic
        var catchHandler: CatchHandler? = null

        abstract class CatchHandler {
            abstract fun handleMd5(source: Int, data: ByteArray, result: ByteArray)

            abstract fun handleTlvSet(tlv: Int, buf: ByteArray, source: Int)

            abstract fun handleTlvGet(tlv: Int, buf: ByteArray, source: Int)

            abstract fun handlePacket(time: Long, packet: CapturePacket)

            abstract fun handleTea(isEnc: Boolean, data: ByteArray, key: ByteArray, result: ByteArray, source: Int)
        }
    }
}

class FakeCursor: Cursor, HashMap<String, Any>() {
    override fun close() {

    }

    override fun getCount(): Int {
        return this.size
    }

    override fun getPosition(): Int {
        return 0
    }

    override fun move(p0: Int): Boolean {
        return false
    }

    override fun moveToPosition(p0: Int): Boolean {
        return false
    }

    override fun moveToFirst(): Boolean {
        return false
    }

    override fun moveToLast(): Boolean {
        return false
    }

    override fun moveToNext(): Boolean {
        return false
    }

    override fun moveToPrevious(): Boolean {
        return false
    }

    override fun isFirst(): Boolean {
        return false
    }

    override fun isLast(): Boolean {
        return false
    }

    override fun isBeforeFirst(): Boolean {
        return false
    }

    override fun isAfterLast(): Boolean {
        return false
    }

    override fun getColumnIndex(p0: String?): Int {
        return 0
    }

    override fun getColumnIndexOrThrow(p0: String?): Int {
        return 0
    }

    override fun getColumnName(p0: Int): String {
        return ""
    }

    override fun getColumnNames(): Array<String> {
        return arrayOf()
    }

    override fun getColumnCount(): Int {
        return 0
    }

    override fun getBlob(p0: Int): ByteArray {
        return EMPTY_BYTE_ARRAY
    }

    override fun getString(p0: Int): String {
        return ""
    }

    override fun copyStringToBuffer(p0: Int, p1: CharArrayBuffer?) {
    }

    override fun getShort(p0: Int): Short {
        return 0
    }

    override fun getInt(p0: Int): Int {
        return 0
    }

    override fun getLong(p0: Int): Long {
        return 0
    }

    override fun getFloat(p0: Int): Float {
        return 0f
    }

    override fun getDouble(p0: Int): Double {
        return 0.0
    }

    override fun getType(p0: Int): Int {
        return Cursor.FIELD_TYPE_INTEGER
    }

    override fun isNull(p0: Int): Boolean {
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun deactivate() {
    }

    @Deprecated("Deprecated in Java", ReplaceWith("false"))
    override fun requery(): Boolean {
        return false
    }

    override fun isClosed(): Boolean {
        return true
    }

    override fun registerContentObserver(p0: ContentObserver?) {
    }

    override fun unregisterContentObserver(p0: ContentObserver?) {
    }

    override fun registerDataSetObserver(p0: DataSetObserver?) {
    }

    override fun unregisterDataSetObserver(p0: DataSetObserver?) {
    }

    override fun setNotificationUri(p0: ContentResolver?, p1: Uri?) {
    }

    override fun getNotificationUri(): Uri {
        return Uri.EMPTY
    }

    override fun getWantsAllOnMoveCalls(): Boolean {
        return true
    }

    override fun setExtras(p0: Bundle?) {
    }

    override fun getExtras(): Bundle {
        val bundle = Bundle()
        forEach {
            when (it.value) {
                is String -> {
                    bundle.putString(it.key, it.value as String)
                }
                is ByteArray -> {
                    bundle.putByteArray(it.key, it.value as ByteArray)
                }
                is Number -> {
                    bundle.putInt(it.key, it.value as Int)
                }
            }
        }
        return bundle
    }

    override fun respond(p0: Bundle?): Bundle {
        return extras
    }
}

