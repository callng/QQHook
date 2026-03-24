package moe.ore.xposed.hook

import android.content.ContentValues
import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import moe.ore.txhook.app.CatchProvider
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY
import moe.ore.txhook.helper.hex2ByteArray
import moe.ore.txhook.helper.toHexString
import moe.ore.xposed.hook.base.hostClassLoader
import moe.ore.xposed.hook.base.hostPackageName
import moe.ore.xposed.hook.base.hostVersionCode
import moe.ore.xposed.hook.enums.QQTypeEnum
import moe.ore.xposed.utils.FuzzySearchClass
import moe.ore.xposed.utils.GlobalData
import moe.ore.xposed.utils.HookUtil
import moe.ore.xposed.utils.HttpUtil
import moe.ore.xposed.utils.PacketDedupCache
import moe.ore.xposed.utils.QQ_9_2_10_29175
import moe.ore.xposed.utils.XPClassloader
import moe.ore.xposed.utils.getPatchBuffer
import moe.ore.xposed.utils.hookMethod
import java.lang.reflect.Method
import java.nio.ByteBuffer

object MainHook {
    private const val DEFAULT_URI = "content://${CatchProvider.MY_URI}"
    private const val MODE_BDH_SESSION = "bdh.session"
    private const val MODE_BDH_SESSION_KEY = "bdh.sessionkey"
    private const val MODE_MD5 = "md5"
    private const val MODE_TLV_GET_BUF = "tlv.get_buf"
    private const val MODE_TLV_SET_BUF = "tlv.set_buf"
    private const val MODE_TEA = "tea"
    private const val MODE_RECE_DATA = "receData"
    private const val MODE_SEND = "send"
    private const val TYPE_FLY = "fly"
    private const val TYPE_GETSIGN = "getsign"
    private const val TYPE_GET_FE_KIT_ATTACH = "getFeKitAttach"
    private const val TYPE_NATIVE_SET_ACCOUNT_KEY = "nativeSetAccountKey"
    private const val TYPE_ECDH_DATA = "ecdhData"

    private val defaultUri = DEFAULT_URI.toUri()
    private var isInit = false
    private var source = 0
    private val global = GlobalData()
    private val EcdhCrypt = XPClassloader.load("oicq.wlogin_sdk.tools.EcdhCrypt")!!
    private val CodecWarpper = XPClassloader.load("com.tencent.qphone.base.util.CodecWarpper")!!
    private val cryptor = XPClassloader.load("oicq.wlogin_sdk.tools.cryptor")!!
    private val tlv_t = XPClassloader.load("oicq.wlogin_sdk.tlv_type.tlv_t")!!
    private val MD5 = XPClassloader.load("oicq.wlogin_sdk.tools.MD5")!!
    private val HighwaySessionData =
        XPClassloader.load("com.tencent.mobileqq.highway.openup.SessionInfo")!!
    private val MSFKernel = XPClassloader.load("com.tencent.mobileqq.msfcore.MSFKernel")

    lateinit var unhook: XC_MethodHook.Unhook
    val hasUnhook get() = ::unhook.isInitialized

    // ==================== Entry ====================

    operator fun invoke(source: Int, ctx: Context) {
        HttpUtil.contentResolver = ctx.contentResolver
        HttpUtil.contextWeakReference = java.lang.ref.WeakReference(ctx)
        this.source = source

        hookMSFKernelPacket()
        hookCodecWarpperInit()
        hookMD5()
        hookTlv()
        hookTea()
        hookSendPacket()
        hookBDH()
        hookParams()
        hookReceData(AntiDetection.isSupportedQQVersion(hostPackageName, hostVersionCode))
    }

    // ==================== CodecWrapper Init ====================

    private fun hookCodecWarpperInit() {
        CodecWarpper.hookMethod("init")?.before {
            if (it.args.size >= 2) {
                it.args[1] = true
                if (!isInit) {
                    hookReceive(it.thisObject.javaClass)
                }
            }
        }?.after {
            isInit = true
        }
    }

    // ==================== MSF Kernel Packet ====================

    private fun hookMSFKernelPacket() {
        if (QQTypeEnum.valueOfPackage(hostPackageName) == QQTypeEnum.QQ &&
            hostVersionCode > QQ_9_2_10_29175
        ) {
            hookMSFKernelSend()
            hookMSFKernelReceive()
        }
    }

    private fun hookMSFKernelReceive() {
        FuzzySearchClass.findClassWithMethod(
            classLoader = hostClassLoader,
            packagePrefix = "com.tencent.mobileqq.msf.core",
            innerClassPath = "c.b\$e",
            methodName = "onMSFPacketState",
            parameterTypes = arrayOf(
                XPClassloader.load("com.tencent.mobileqq.msfcore.MSFResponseAdapter")!!
            )
        )?.hookMethod("onMSFPacketState")?.after {
            val from = it.args[0]
            val cmd = from.getField<String>("mCmd")
            val seq = from.getField<Int>("mSeq")
            val uin = from.getField<String>("mUin")
            val data = from.getField<ByteArray>("mRecvData")

            if (PacketDedupCache.shouldProcess(seq, "receive_$cmd", data)) {
                sendTo("receive") {
                    put("cmd", cmd); put("buffer", data)
                    put("uin", uin); put("seq", seq)
                    put("msgCookie", EMPTY_BYTE_ARRAY); put("type", "unknown")
                }
            }
        }
    }

    private fun hookMSFKernelSend() {
        MSFKernel?.hookMethod("sendPacket")?.after {
            val from = it.args[0]
            if (from.javaClass.name != "com.tencent.mobileqq.msfcore.MSFRequestAdapter") return@after

            val cmd = from.getField<String>("mCmd")
            val seq = from.getField<Int>("mSeq")
            val uin = from.getField<String>("mUin")
            val data = from.getField<ByteArray>("mData")

            if (PacketDedupCache.shouldProcess(seq, "send_$cmd", data)) {
                sendTo(MODE_SEND) {
                    put("cmd", cmd); put("buffer", data)
                    put("uin", uin); put("seq", seq)
                    put("msgCookie", EMPTY_BYTE_ARRAY); put("type", "unknown")
                }
            }
        }
    }

    // ==================== BDH (Highway) ====================

    private fun hookBDH() {
        hookForceUseHttp()
        HighwaySessionData.hookMethod("getHttpconn_sig_session")?.after {
            sendHexData(MODE_BDH_SESSION, it.result as ByteArray)
        }
        HighwaySessionData.hookMethod("getSessionKey")?.after {
            sendHexData(MODE_BDH_SESSION_KEY, it.result as ByteArray)
        }
    }

    private fun hookForceUseHttp() {
        val connMng = XPClassloader.load("com.tencent.mobileqq.highway.config.ConfigManager")
        connMng.hookMethod("getNextSrvAddr")?.after {
            XposedHelpers.setIntField(it.result, "protoType", 2)
        }

        val pointClz = XPClassloader.load("com.tencent.mobileqq.highway.utils.EndPoint")
        val tcpConn = XPClassloader.load("com.tencent.mobileqq.highway.conn.TcpConnection")
        XposedBridge.hookAllConstructors(tcpConn, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args.filter { it.javaClass == pointClz }.forEach {
                    XposedHelpers.setIntField(it, "protoType", 2)
                }
            }
        })
    }

    // ==================== Params ====================

    private fun hookParams() {
        hookEcdhCrypt()
        hookByteDataGetSign()
        hookDandelionFly()
        hookQQSecuritySignGetSign()
        hookQSecGetFeKitAttach()
        hookD2Key()
    }

    private fun hookEcdhCrypt() {
        fun collectEcdhData(ecdhCrypt: Any) {
            runCatching {
                val cPubKey = ecdhCrypt.invokeMethod<ByteArray>("get_c_pub_key")
                val gShareKey = ecdhCrypt.invokeMethod<ByteArray>("get_g_share_key")
                val pubKeyVer = ecdhCrypt.invokeMethod<Int>("get_pub_key_ver")

                HttpUtil.postTo("ecdh_data", JsonObject().apply {
                    addProperty("type", TYPE_ECDH_DATA)
                    addProperty("c_pub_key", cPubKey.toHexString())
                    addProperty("g_share_key", gShareKey.toHexString())
                    addProperty("pub_key_ver", pubKeyVer)
                    addProperty("source", source)
                    addProperty("stacktrace", HookUtil.getFormattedStackTrace())
                }, source)
            }.onFailure { XposedBridge.log("[TXHook] Error collecting EcdhCrypt data: ${it.message}") }
        }

        for (method in listOf("initShareKey", "initShareKeyByDefault", "GenECDHKeyEx")) {
            EcdhCrypt.hookMethod(method)?.after { collectEcdhData(it.thisObject) }
        }
    }

    private fun hookByteDataGetSign() {
        XPClassloader.load("com.tencent.secprotocol.ByteData")?.hookMethod("getSign")?.after {
            postCallToken(TYPE_FLY) {
                addProperty("data", it.args[1] as String)
                addProperty("salt", (it.args[2] as ByteArray).toHexString())
                addProperty("result", (it.result as ByteArray).toHexString())
            }
        }
    }

    private fun hookDandelionFly() {
        XPClassloader.load("com.tencent.mobileqq.qsec.qsecdandelionsdk.Dandelion")?.hookMethod("fly")?.after {
            postCallToken(TYPE_FLY) {
                addProperty("data", it.args[0] as String)
                addProperty("salt", (it.args[1] as ByteArray).toHexString())
                addProperty("result", (it.result as ByteArray).toHexString())
            }
        }
    }

    private fun hookQQSecuritySignGetSign() {
        XPClassloader.load("com.tencent.mobileqq.sign.QQSecuritySign")?.declaredMethods?.firstOrNull {
            it.name == "getSign" && it.parameterTypes.size == 5 &&
                it.parameterTypes[1] == String::class.java &&
                it.parameterTypes[2] == ByteArray::class.java &&
                it.parameterTypes[3] == ByteArray::class.java &&
                it.parameterTypes[4] == String::class.java
        }?.let { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val result = param.result!!
                    fun readHex(name: String): String =
                        result.javaClass.getField(name).get(result).let { (it as ByteArray).toHexString() }
                    val signResult = Result(
                        extra = readHex("extra"),
                        sign = readHex("sign"),
                        token = readHex("token"),
                    )
                    HttpUtil.postTo("callToken", Gson().toJson(Wrapper(
                        type = TYPE_GETSIGN,
                        cmd = param.args[1] as String,
                        buffer = (param.args[2] as ByteArray).toHexString(),
                        seq = ByteBuffer.wrap(param.args[3] as ByteArray).int,
                        uin = param.args[4] as String,
                        result = signResult,
                        source = source,
                        bit = global.consume("checkData"),
                        stacktrace = HookUtil.getFormattedStackTrace(),
                    )))
                }
            })
        }
    }

    private fun hookQSecGetFeKitAttach() {
        XPClassloader.load("com.tencent.mobileqq.qsec.qsecurity.QSec")?.hookMethod("getFeKitAttach")?.after {
            postCallToken(TYPE_GET_FE_KIT_ATTACH) {
                addProperty("uin", it.args[1] as String)
                addProperty("cmd", it.args[2] as String)
                addProperty("subcmd", it.args[3] as String)
                addProperty("result", (it.result as ByteArray).toHexString())
            }
        }
    }

    private fun hookD2Key() {
        val methodName = when (source) {
            3, 4 -> "setAccountKey"
            else -> "nativeSetAccountKey"
        }
        CodecWarpper.hookMethod(methodName)?.after {
            HttpUtil.postTo("callToken", JsonObject().apply {
                addProperty("type", TYPE_NATIVE_SET_ACCOUNT_KEY)
                addProperty("uin", it.args[0] as String)
                addProperty("d2key", (it.args[7] as ByteArray).toHexString())
                addProperty("stacktrace", HookUtil.getFormattedStackTrace())
            }, source)
        }
    }

    // ==================== MD5 ====================

    private fun hookMD5() {
        hookMD5Method("toMD5Byte", ByteArray::class.java) { param ->
            val data = param.args[0] as ByteArray
            val result = param.result as ByteArray? ?: EMPTY_BYTE_ARRAY
            data to result
        }
        hookMD5Method("toMD5Byte", String::class.java) { param ->
            val data = (param.args[0] as? String)?.toByteArray() ?: return@hookMD5Method null
            data to (param.result as ByteArray)
        }
        hookMD5Method("toMD5", String::class.java) { param ->
            val data = (param.args[0] as? String)?.toByteArray() ?: return@hookMD5Method null
            data to (param.result as String).hex2ByteArray()
        }
        hookMD5Method("toMD5", ByteArray::class.java) { param ->
            val data = param.args[0] as ByteArray
            data to (param.result as String).hex2ByteArray()
        }
    }

    private fun hookMD5Method(
        methodName: String,
        paramType: Class<*>,
        extract: (MethodHookParam) -> Pair<ByteArray, ByteArray>?
    ) {
        XposedHelpers.findAndHookMethod(MD5, methodName, paramType, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val (data, result) = extract(param) ?: return
                sendTo(MODE_MD5) { put("data", data); put("result", result) }
            }
        })
    }

    // ==================== TLV ====================

    private fun hookTlv() {
        runCatching {
            val cmdField = tlv_t.getDeclaredField("_cmd").also { it.isAccessible = true }
            val bufField = tlv_t.getDeclaredField("_buf").also { it.isAccessible = true }

            tlv_t.hookMethod("get_buf")?.after {
                val result = it.result as ByteArray
                val ver = cmdField.get(it.thisObject) as Int
                sendTo(MODE_TLV_GET_BUF) { put("data", result); put("version", ver) }
            }
            tlv_t.hookMethod("get_tlv")?.after {
                val result = bufField.get(it.thisObject) as ByteArray
                val ver = cmdField.get(it.thisObject) as Int
                sendTo(MODE_TLV_SET_BUF) { put("data", result); put("version", ver) }
            }
        }
    }

    // ==================== TEA ====================

    private fun hookTea() {
        cryptor.hookMethod("encrypt")?.after { handleTeaHook(it, true) }
        cryptor.hookMethod("decrypt")?.after { handleTeaHook(it, false) }
    }

    private fun handleTeaHook(param: MethodHookParam, isEnc: Boolean) {
        val len = param.args[2] as Int
        if (len <= 0) return

        val skip = param.args[1] as Int
        val data = (param.args[0] as ByteArray).copyOfRange(skip, skip + len)
        sendTo(MODE_TEA) {
            put("enc", isEnc); put("data", data.toHexString()); put("len", len)
            put("key", (param.args[3] as ByteArray).toHexString())
            put("result", (param.result as ByteArray).toHexString())
        }
    }

    // ==================== ReceData ====================

    private fun hookReceData(isEnablePatch: Boolean) {
        if (isEnablePatch) {
            val hook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val patches = (50001..50005).map { getPatchBuffer(it) }
                    val combined = ByteArray(patches.sumOf { it.size })
                    var offset = 0
                    for (packet in patches) {
                        System.arraycopy(packet, 0, combined, offset, packet.size)
                        offset += packet.size
                    }

                    if (hasUnhook) unhook.unhook()

                    try {
                        (param.method as Method).invoke(param.thisObject, combined, 0)
                    } catch (e: Throwable) {
                        XposedBridge.log("[TXHook] nativeOnReceData invoke: $e")
                    }

                    param.result = Unit
                }
            }

            unhook = XposedHelpers.findAndHookMethod(
                CodecWarpper, "nativeOnReceData",
                ByteArray::class.java, Int::class.java, hook
            )
        }

        CodecWarpper.hookMethod("onReceData")?.after {
            val data = it.args[0] as ByteArray
            val size = if (it.args.size == 1) data.size else {
                (it.args[1] as Int).let { if (it == 0) data.size else it }
            }
            sendTo(MODE_RECE_DATA) { put("data", data.toHexString()); put("size", size) }
        }
    }

    // ==================== Send Packet ====================

    private fun hookSendPacket() {
        CodecWarpper.hookMethod("encodeRequest")?.after { param ->
            val args = param.args
            if (args.size !in setOf(14, 15, 16, 17)) {
                XposedBridge.log("[TXHook] encodeRequest 不知道hook到了个不知道什么东西")
                return@after
            }

            val seq = args[0] as? Int
            val cmd = args[5] as? String
            val msgCookie = args[6] as? ByteArray
            val uin = args[9] as? String
            val buffer = when (args.size) {
                17 -> args[15]; 16 -> args[14]; 15 -> args[13]; else -> args[12]
            } as? ByteArray

            sendTo(MODE_SEND) {
                put("uin", uin ?: ""); put("seq", seq ?: 0); put("cmd", cmd ?: "")
                put("type", "unknown")
                put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
                put("buffer", buffer ?: EMPTY_BYTE_ARRAY)
                put("result", param.result as? ByteArray ?: EMPTY_BYTE_ARRAY)
            }
        }
    }

    // ==================== Receive (from CodecWrapper init) ====================

    private fun hookReceive(clazz: Class<*>) {
        clazz.hookMethod("onResponse")?.after { param ->
            val from = param.args[1]
            val seq = HttpUtil.invokeFromObjectMethod(from, "getRequestSsoSeq") as Int
            val cmd = HttpUtil.invokeFromObjectMethod(from, "getServiceCmd") as String
            val msgCookie = HttpUtil.invokeFromObjectMethod(from, "getMsgCookie") as? ByteArray
            val uin = HttpUtil.invokeFromObjectMethod(from, "getUin") as String
            val buffer = HttpUtil.invokeFromObjectMethod(from, "getWupBuffer") as ByteArray

            sendTo("receive") {
                put("cmd", cmd); put("uin", uin); put("seq", seq)
                put("buffer", buffer); put("type", "unknown")
                put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
            }
        }
    }

    // ==================== Helpers ====================

    /** 构建 ContentValues 并自动追加 mode + stacktrace 后发送 */
    private fun sendTo(mode: String, block: ContentValues.() -> Unit) {
        ContentValues().apply {
            put("mode", mode)
            put("stacktrace", HookUtil.getFormattedStackTrace())
            block()
        }.let { HttpUtil.sendTo(defaultUri, it, source) }
    }

    /** 快捷发送 hex 编码的 ByteArray 数据 */
    private fun sendHexData(mode: String, data: ByteArray) {
        sendTo(mode) { put("data", data.toHexString()) }
    }

    /** 构建 callToken JsonObject，自动添加 type + stacktrace + checkData */
    private fun postCallToken(type: String, block: JsonObject.() -> Unit) {
        HttpUtil.postTo("callToken", JsonObject().apply {
            addProperty("type", type)
            addProperty("bit", global.consume("checkData"))
            addProperty("stacktrace", HookUtil.getFormattedStackTrace())
            block()
        }, source)
    }

    // ==================== Reflection Helpers ====================

    /** 读取对象的 declaredField 值，自动设置 accessible */
    @Suppress("UNCHECKED_CAST")
    private fun <T> Any.getField(name: String): T =
        javaClass.getDeclaredField(name).also { it.isAccessible = true }.get(this) as T

    /** 调用对象的 declaredMethod，自动设置 accessible */
    @Suppress("UNCHECKED_CAST")
    private fun <T> Any.invokeMethod(name: String): T {
        val method = javaClass.getDeclaredMethod(name)
        method.isAccessible = true
        return method.invoke(this) as T
    }

    /** 从 GlobalData 取值并移除，不存在返回空字符串 */
    private fun GlobalData.consume(key: String): String =
        (get(key) as? String).also { if (it != null) remove(key) } ?: ""
}

data class Result(
    @SerializedName("extra") val extra: String,
    @SerializedName("sign") val sign: String,
    @SerializedName("token") val token: String
)

data class Wrapper(
    @SerializedName("source") val source: Int,
    @SerializedName("type") val type: String,
    @SerializedName("cmd") val cmd: String,
    @SerializedName("buffer") val buffer: String,
    @SerializedName("seq") val seq: Int,
    @SerializedName("uin") val uin: String,
    @SerializedName("result") val result: Result,
    @SerializedName("bit") val bit: String,
    @SerializedName("stacktrace") val stacktrace: String
)
