package moe.ore.xposed.hook

import android.content.ContentValues
import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import de.robv.android.xposed.XC_MethodHook
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
import moe.ore.xposed.utils.QQ_9_1_90_26520
import moe.ore.xposed.utils.XPClassloader
import moe.ore.xposed.utils.getPatchBuffer
import moe.ore.xposed.utils.hookMethod
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.ByteBuffer

object MainHook {
    private const val DEFAULT_URI = "content://${CatchProvider.Companion.MY_URI}"
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
    private var isInit: Boolean = false
    private var source = 0
    private val global = GlobalData()
    private val EcdhCrypt = XPClassloader.load("oicq.wlogin_sdk.tools.EcdhCrypt")!!
    private val CodecWarpper = XPClassloader.load("com.tencent.qphone.base.util.CodecWarpper")!!
    private val cryptor = XPClassloader.load("oicq.wlogin_sdk.tools.cryptor")!!
    private val tlv_t = XPClassloader.load("oicq.wlogin_sdk.tlv_type.tlv_t")!!
    private val MD5 = XPClassloader.load("oicq.wlogin_sdk.tools.MD5")!!
    private val HighwaySessionData = XPClassloader.load("com.tencent.mobileqq.highway.openup.SessionInfo")!!
    // private val MSFKernel = XPClassloader.load("com.tencent.mobileqq.msfcore.MSFKernel")

    lateinit var unhook: XC_MethodHook.Unhook
    val hasUnhook get() = ::unhook.isInitialized

    operator fun invoke(source: Int, ctx: Context) {
        HttpUtil.contentResolver = ctx.contentResolver
        HttpUtil.contextWeakReference = WeakReference(ctx)
        this.source = source

        hookCodecWarpperInit()
        hookMD5()
        hookTlv()
        hookTea()
        hookSendPacket()
        hookBDH()
        hookParams()
        if (QQTypeEnum.valueOfPackage(hostPackageName) == QQTypeEnum.QQ && hostVersionCode >= QQ_9_1_90_26520) {
            hookReceData(true)
        } else hookReceData(false)
    }

    private fun hookCodecWarpperInit() {
        CodecWarpper.hookMethod("init")?.before {
            if (it.args.size >= 2) {
                it.args[1] = true // 强制打开调试模式
                if (!isInit) {
                    val thisClass = it.thisObject.javaClass
                    hookReceive(thisClass)
                }
            }
        }?.after {
            isInit = true
        }
    }

    private fun hookMSFKernelPacket() {
        hookMSFKernelSend()
        hookMSFKernelReceive()
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

            val cmdField = from.javaClass.getDeclaredField("mCmd").also { it.isAccessible = true }
            val cmd = cmdField.get(from) as String
            val seqField = from.javaClass.getDeclaredField("mSeq").also { it.isAccessible = true }
            val seq = seqField.get(from) as Int
            val uinField = from.javaClass.getDeclaredField("mUin").also { it.isAccessible = true }
            val uin = uinField.get(from) as String
            val dataField = from.javaClass.getDeclaredField("mRecvData").also { it.isAccessible = true }
            val data = dataField.get(from) as ByteArray

            if (PacketDedupCache.shouldProcess(seq, "receive_$cmd", data)) {
                val stackTrace = HookUtil.getFormattedStackTrace()

                val value = ContentValues()
                value.put("cmd", cmd)
                value.put("buffer", data)
                value.put("uin", uin)
                value.put("seq", seq)
                value.put("msgCookie", EMPTY_BYTE_ARRAY)
                value.put("type", "unknown")
                value.put("mode", "receive")
                value.put("stacktrace", stackTrace)
                HttpUtil.sendTo(defaultUri, value, source)
            }
        }
    }

    private fun hookMSFKernelSend() {
        /*MSFKernel?.hookMethod("sendPacket")?.after {
            val from = it.args[0]
            if (from.javaClass.name == "com.tencent.mobileqq.msfcore.MSFRequestAdapter") {
                val cmdField = from.javaClass.getDeclaredField("mCmd").also { it.isAccessible = true }
                val cmd = cmdField.get(from) as String
                val seqField = from.javaClass.getDeclaredField("mSeq").also { it.isAccessible = true }
                val seq = seqField.get(from) as Int
                val uinField = from.javaClass.getDeclaredField("mUin").also { it.isAccessible = true }
                val uin = uinField.get(from) as String
                val dataField = from.javaClass.getDeclaredField("mData").also { it.isAccessible = true }
                val data = dataField.get(from) as ByteArray

                if (PacketDedupCache.shouldProcess(seq, "send_$cmd", data)) {
                    val stackTrace = HookUtil.getFormattedStackTrace()

                    val value = ContentValues()
                    value.put("cmd", cmd)
                    value.put("buffer", data)
                    value.put("uin", uin)
                    value.put("seq", seq)
                    value.put("msgCookie", EMPTY_BYTE_ARRAY)
                    value.put("type", "unknown")
                    value.put("mode", MODE_SEND)
                    value.put("stacktrace", stackTrace)
                    HttpUtil.sendTo(defaultUri, value, source)
                }
            }
        }*/
    }

    private fun hookBDH() {
        hookForceUseHttp()
        hookGetSession()
        hookGetSessionKey()
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

    private fun hookGetSession() {
        HighwaySessionData.hookMethod("getHttpconn_sig_session")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val result = it.result as ByteArray
            sendDataToDefaultUri(MODE_BDH_SESSION, result, stackTrace)
        }
    }

    private fun hookGetSessionKey() {
        HighwaySessionData.hookMethod("getSessionKey")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val result = it.result as ByteArray
            sendDataToDefaultUri(MODE_BDH_SESSION_KEY, result, stackTrace)
        }
    }

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
            try {
                val cPubKeyMethod = ecdhCrypt.javaClass.getDeclaredMethod("get_c_pub_key")
                val gShareKeyMethod = ecdhCrypt.javaClass.getDeclaredMethod("get_g_share_key")
                val pubKeyVerMethod = ecdhCrypt.javaClass.getDeclaredMethod("get_pub_key_ver")

                cPubKeyMethod.isAccessible = true
                gShareKeyMethod.isAccessible = true
                pubKeyVerMethod.isAccessible = true

                val cPubKey = cPubKeyMethod.invoke(ecdhCrypt) as ByteArray
                val gShareKey = gShareKeyMethod.invoke(ecdhCrypt) as ByteArray
                val pubKeyVer = pubKeyVerMethod.invoke(ecdhCrypt) as Int

                val stackTrace = HookUtil.getFormattedStackTrace()

                val jsonObject = JsonObject().apply {
                    addProperty("type", TYPE_ECDH_DATA)
                    addProperty("c_pub_key", cPubKey.toHexString())
                    addProperty("g_share_key", gShareKey.toHexString())
                    addProperty("pub_key_ver", pubKeyVer)
                    addProperty("stacktrace", stackTrace)
                    addProperty("source", source)
                }
                val json = Gson().toJson(jsonObject)
                HttpUtil.postTo("ecdh_data", json)
            } catch (e: Exception) {
                XposedBridge.log("[TXHook] Error collecting EcdhCrypt data: ${e.message}")
            }
        }

        EcdhCrypt.hookMethod("initShareKey")?.after { param ->
            val ecdhCrypt = param.thisObject
            collectEcdhData(ecdhCrypt)
        }

        EcdhCrypt.hookMethod("initShareKeyByDefault")?.after { param ->
            val ecdhCrypt = param.thisObject
            collectEcdhData(ecdhCrypt)
        }

        EcdhCrypt.hookMethod("GenECDHKeyEx")?.after { param ->
            val ecdhCrypt = param.thisObject
            collectEcdhData(ecdhCrypt)
        }
    }

    private fun hookByteDataGetSign() {
        val bytedataClz = XPClassloader.load("com.tencent.secprotocol.ByteData")
        bytedataClz?.hookMethod("getSign")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val result = it.result as ByteArray
            val data = it.args[1] as String
            val salt = it.args[2] as ByteArray
            postCallToken(TYPE_FLY, data, salt, result, stackTrace)
        }
    }

    private fun hookDandelionFly() {
        val dandelionClz =
            XPClassloader.load("com.tencent.mobileqq.qsec.qsecdandelionsdk.Dandelion")
        dandelionClz?.hookMethod("fly")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val result = it.result as ByteArray
            val data = it.args[0] as String
            val salt = it.args[1] as ByteArray
            postCallToken(TYPE_FLY, data, salt, result, stackTrace)
        }
    }

    private fun hookQQSecuritySignGetSign() {
        val qqsecuritysignClz = XPClassloader.load("com.tencent.mobileqq.sign.QQSecuritySign")
        qqsecuritysignClz?.declaredMethods?.firstOrNull {
            it.name == "getSign" && it.parameterTypes.size == 5 &&
                    it.parameterTypes[1] == String::class.java &&
                    it.parameterTypes[2] == ByteArray::class.java &&
                    it.parameterTypes[3] == ByteArray::class.java &&
                    it.parameterTypes[4] == String::class.java
        }?.let { method ->
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val stackTrace = HookUtil.getFormattedStackTrace()

                    val cmd = param.args[1] as String
                    val buffer = param.args[2] as ByteArray
                    val seq = param.args[3] as ByteArray
                    val uin = param.args[4] as String
                    val result = param.result
                    val resultData = getQQSecuritySignResultData(result)
                    val checkData = getCheckDataAndRemoveFromGlobal()
                    val wrapper = Wrapper(
                        type = TYPE_GETSIGN,
                        cmd = cmd,
                        buffer = buffer.toHexString(),
                        seq = ByteBuffer.wrap(seq).int,
                        uin = uin,
                        result = resultData,
                        source = source,
                        bit = checkData,
                        stacktrace = stackTrace
                    )
                    val json = Gson().toJson(wrapper)
                    HttpUtil.postTo("callToken", json)
                }
            })
        }
    }

    private fun getQQSecuritySignResultData(result: Any?): Result {
        var extra: ByteArray? = null
        var sign: ByteArray? = null
        var token: ByteArray? = null
        result?.let {
            if (it.javaClass.name == "com.tencent.mobileqq.sign.QQSecuritySign\$SignResult") {
                extra = it.javaClass.getField("extra").get(it) as? ByteArray
                sign = it.javaClass.getField("sign").get(it) as? ByteArray
                token = it.javaClass.getField("token").get(it) as? ByteArray
            }
        }
        return Result(extra!!.toHexString(), sign!!.toHexString(), token!!.toHexString())
    }

    private fun getCheckDataAndRemoveFromGlobal(): String {
        var checkData = ""
        if ("checkData" in global) {
            checkData = global["checkData"] as String
            global.remove("checkData")
        }
        return checkData
    }

    private fun hookQSecGetFeKitAttach() {
        val qsecClz = XPClassloader.load("com.tencent.mobileqq.qsec.qsecurity.QSec")
        qsecClz?.hookMethod("getFeKitAttach")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val uin = it.args[1] as String
            val cmd = it.args[2] as String
            val subcmd = it.args[3] as String
            val result = it.result as ByteArray
            postCallTokenWithExtraInfo(TYPE_GET_FE_KIT_ATTACH, uin, cmd, subcmd, result, stackTrace)
        }
    }

    private fun hookD2Key() {
        when (source) {
            0, 1, 2, 8 -> hookCodecWarpperNativeSetAccountKey()
            3, 4 -> hookCodecWarpperSetAccountKey()
        }
    }

    private fun hookCodecWarpperNativeSetAccountKey() {
        CodecWarpper.hookMethod("nativeSetAccountKey")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val uin = it.args[0] as String
            val d2key = it.args[7] as ByteArray
            postCallTokenWithD2Key(uin, d2key, stackTrace)
        }
    }

    private fun hookCodecWarpperSetAccountKey() {
        CodecWarpper.hookMethod("setAccountKey")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val uin = it.args[0] as String
            val d2key = it.args[7] as ByteArray
            postCallTokenWithD2Key(uin, d2key, stackTrace)
        }
    }

    private fun hookMD5() {
        hookMD5ToMD5ByteByteArray()
        hookMD5ToMD5ByteString()
        hookMD5ToMD5String()
        hookMD5ToMD5ByteArray()
    }

    private fun hookMD5ToMD5ByteByteArray() {
        XposedHelpers.findAndHookMethod(MD5, "toMD5Byte", ByteArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val stackTrace = HookUtil.getFormattedStackTrace()

                val data = param.args[0] as ByteArray
                val result = param.result as ByteArray? ?: EMPTY_BYTE_ARRAY
                submitMd5(data, result, stackTrace)
            }
        })
    }

    private fun hookMD5ToMD5ByteString() {
        XposedHelpers.findAndHookMethod(MD5, "toMD5Byte", String::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val stackTrace = HookUtil.getFormattedStackTrace()

                val data = (param.args[0] as String?)?.toByteArray()
                data?.let {
                    val result = param.result as ByteArray
                    submitMd5(it, result, stackTrace)
                }
            }
        })
    }

    private fun hookMD5ToMD5String() {
        XposedHelpers.findAndHookMethod(MD5, "toMD5", String::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val stackTrace = HookUtil.getFormattedStackTrace()

                val data = (param.args[0] as String?)?.toByteArray()
                data?.let {
                    val result = (param.result as String).hex2ByteArray()
                    submitMd5(it, result, stackTrace)
                }
            }
        })
    }

    private fun hookMD5ToMD5ByteArray() {
        XposedHelpers.findAndHookMethod(MD5, "toMD5", ByteArray::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val stackTrace = HookUtil.getFormattedStackTrace()

                val data = param.args[0] as ByteArray
                val result = (param.result as String).hex2ByteArray()
                submitMd5(data, result, stackTrace)
            }
        })
    }

    private fun submitMd5(data: ByteArray, result: ByteArray, stacktrace: String) {
        val value = ContentValues()
        value.put("mode", MODE_MD5)
        value.put("data", data)
        value.put("result", result)
        value.put("stacktrace", stacktrace)
        HttpUtil.sendTo(defaultUri, value, source)
    }

    private fun hookTlv() {
        runCatching {
            val cmd = tlv_t.getDeclaredField("_cmd").also {
                it.isAccessible = true
            }
            hookTlvGetBuf(cmd)
            hookTlvSetBuf(cmd)
        }
    }

    private fun hookTlvGetBuf(cmd: Field) {
        tlv_t.hookMethod("get_buf")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val thiz = it.thisObject
            val result = it.result as ByteArray
            val tlvVer = cmd.get(thiz) as Int
            sendTlvDataToDefaultUri(MODE_TLV_GET_BUF, result, tlvVer, stackTrace)
        }
    }

    private fun hookTlvSetBuf(cmd: Field) {
        val buf = tlv_t.getDeclaredField("_buf").also {
            it.isAccessible = true
        }
        tlv_t.hookMethod("get_tlv")?.after {
            val stackTrace = HookUtil.getFormattedStackTrace()

            val thiz = it.thisObject
            val result = buf.get(thiz) as ByteArray
            val tlvVer = cmd.get(thiz) as Int
            sendTlvDataToDefaultUri(MODE_TLV_SET_BUF, result, tlvVer, stackTrace)
        }
    }

    private fun sendTlvDataToDefaultUri(mode: String, data: ByteArray, version: Int, stacktrace: String) {
        val value = ContentValues()
        value.put("mode", mode)
        value.put("data", data)
        value.put("version", version)
        value.put("stacktrace", stacktrace)
        HttpUtil.sendTo(defaultUri, value, source)
    }

    private fun hookTea() {
        hookTeaEncrypt()
        hookTeaDecrypt()
    }

    private fun hookTeaEncrypt() {
        cryptor.hookMethod("encrypt")?.after {
            handleTeaHook(it, true)
        }
    }

    private fun hookTeaDecrypt() {
        cryptor.hookMethod("decrypt")?.after {
            handleTeaHook(it, false)
        }
    }

    private fun handleTeaHook(it: XC_MethodHook.MethodHookParam, isEnc: Boolean) {
        val stackTrace = HookUtil.getFormattedStackTrace()

        val data = it.args[0] as ByteArray
        val skip = it.args[1] as Int
        val len = it.args[2] as Int
        val key = (it.args[3] as ByteArray).toHexString()
        val result = (it.result as ByteArray).toHexString()
        if (len > 0) {
            val newData = data.copyOfRange(skip, skip + len).toHexString()
            val value = ContentValues()
            value.put("enc", isEnc)
            value.put("mode", MODE_TEA)
            value.put("data", newData)
            value.put("len", len)
            value.put("result", result)
            value.put("key", key)
            value.put("stacktrace", stackTrace)
            HttpUtil.sendTo(defaultUri, value, source)
        }
    }

    private fun hookReceData(isEnablePatch: Boolean) {
        if (isEnablePatch) {
            val hook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val modifiedPackets = ArrayList<ByteArray>(5).apply {
                        add(getPatchBuffer(50001))
                        add(getPatchBuffer(50002))
                        add(getPatchBuffer(50003))
                        add(getPatchBuffer(50004))
                        add(getPatchBuffer(50005))
                    }

                    val totalSize = modifiedPackets.sumOf { it.size }
                    val finalBytes = ByteArray(totalSize)
                    var offset = 0
                    for (packet in modifiedPackets) {
                        System.arraycopy(packet, 0, finalBytes, offset, packet.size)
                        offset += packet.size
                    }

                    if (hasUnhook) unhook.unhook()

                    try {
                        val method = param.method as Method
                        method.invoke(param.thisObject, finalBytes, 0)
                    } catch (e: Throwable) {
                        XposedBridge.log("[TXHook] nativeOnReceData invoke: $e")
                    }

                    param.result = Unit
                }
            }

            unhook = XposedHelpers.findAndHookMethod(
                CodecWarpper,
                "nativeOnReceData",
                ByteArray::class.java, Int::class.java,
                hook)

        }

        CodecWarpper.hookMethod("onReceData")?.after {
            val args = it.args
            when (args.size) {
                1 -> handleReceDataOneArg(args)
                2, 3 -> handleReceDataTwoArgs(args)
                else -> XposedBridge.log("[TXHook] onReceData 不知道hook到了个不知道什么东西")
            }
        }
    }

    private fun handleReceDataOneArg(args: Array<Any>) {
        val stackTrace = HookUtil.getFormattedStackTrace()

        val data = args[0] as ByteArray
        val size = data.size
        sendReceDataToDefaultUri(data, size, stackTrace)
    }

    private fun handleReceDataTwoArgs(args: Array<Any>) {
        val stackTrace = HookUtil.getFormattedStackTrace()

        val data = args[0] as ByteArray
        var size = args[1] as Int
        if (size == 0) size = data.size
        sendReceDataToDefaultUri(data, size, stackTrace)
    }

    private fun sendReceDataToDefaultUri(data: ByteArray, size: Int, stacktrace: String) {
        val util = ContentValues()
        util.put("data", data.toHexString())
        util.put("size", size)
        util.put("mode", MODE_RECE_DATA)
        util.put("stacktrace", stacktrace)
        HttpUtil.sendTo(defaultUri, util, source)
    }

    private fun hookSendPacket() {
        CodecWarpper.hookMethod("encodeRequest")?.before {
            /*val cmd = it.args[5] as String
            if (!cmd.startsWith("trpc.o3.ecdh_access.EcdhAccess.SsoSecure")) {
                it.result = Unit
            }*/
        }?.after { param ->
            val args = param.args
            when (args.size) {
                17, 14, 16, 15 -> {
                    val result = param.result as? ByteArray
                    handleSendPacket(args, result)
                }
                else -> XposedBridge.log("[TXHook] encodeRequest 不知道hook到了个不知道什么东西")
            }
        }
    }

    private fun handleSendPacket(args: Array<Any>, result: ByteArray?) {
        val stackTrace = HookUtil.getFormattedStackTrace()

        val seq = args[0] as? Int
        val cmd = args[5] as? String
        val msgCookie = args[6] as? ByteArray
        val uin = args[9] as? String
        val buffer = when (args.size) {
            17 -> args[15] as? ByteArray
            14 -> args[12] as? ByteArray
            16 -> args[14] as? ByteArray
            15 -> args[13] as? ByteArray
            else -> EMPTY_BYTE_ARRAY
        }
        sendSendPacketDataToDefaultUri(uin, seq, cmd, msgCookie, buffer, result, stackTrace)
    }

    private fun sendSendPacketDataToDefaultUri(uin: String?, seq: Int?, cmd: String?, msgCookie: ByteArray?, buffer: ByteArray?, result: ByteArray?, stacktrace: String) {
        val util = ContentValues()
        util.put("uin", uin ?: "")
        util.put("seq", seq ?: 0)
        util.put("cmd", cmd ?: "")
        util.put("type", "unknown")
        util.put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
        util.put("buffer", buffer ?: EMPTY_BYTE_ARRAY)
        util.put("result", result ?: EMPTY_BYTE_ARRAY)
        util.put("mode", MODE_SEND)
        util.put("stacktrace", stacktrace)
        HttpUtil.sendTo(defaultUri, util, source)
    }

    private fun hookReceive(clazz: Class<*>) {
        clazz.hookMethod("onResponse")?.after { param ->
            val stackTrace = HookUtil.getFormattedStackTrace()

            val from = param.args[1]
            val seq = HttpUtil.invokeFromObjectMethod(from, "getRequestSsoSeq") as Int
            val cmd = HttpUtil.invokeFromObjectMethod(from, "getServiceCmd") as String
            val msgCookie = HttpUtil.invokeFromObjectMethod(from, "getMsgCookie") as? ByteArray
            val uin = HttpUtil.invokeFromObjectMethod(from, "getUin") as String
            val buffer = HttpUtil.invokeFromObjectMethod(from, "getWupBuffer") as ByteArray
            // -- qimei [15] imei [2] version [4]

            val util = ContentValues()
            util.put("uin", uin)
            util.put("seq", seq)
            util.put("cmd", cmd)
            util.put("type", "unknown")
            util.put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
            util.put("buffer", buffer)
            util.put("stacktrace", stackTrace)
            util.put("mode", "receive")

            HttpUtil.sendTo(defaultUri, util, source)
        }
    }

    private fun sendDataToDefaultUri(mode: String, data: ByteArray, stacktrace: String) {
        val values = ContentValues()
        values.put("mode", mode)
        values.put("data", data.toHexString())
        values.put("stacktrace", stacktrace)
        HttpUtil.sendTo(defaultUri, values, source)
    }

    private fun postCallToken(type: String, data: String, salt: ByteArray, result: ByteArray, stacktrace: String) {
        HttpUtil.postTo("callToken", JsonObject().apply {
            addProperty("type", type)
            addProperty("data", data)
            addProperty("salt", salt.toHexString())
            addProperty("result", result.toHexString())
            if ("checkData" in global) {
                addProperty("bit", global["checkData"] as String)
                global.remove("checkData")
            }
            addProperty("stacktrace", stacktrace)
        }, source)
    }

    private fun postCallTokenWithExtraInfo(type: String, uin: String, cmd: String, subcmd: String, result: ByteArray, stacktrace: String) {
        HttpUtil.postTo("callToken", JsonObject().apply {
            addProperty("type", type)
            addProperty("uin", uin)
            addProperty("cmd", cmd)
            addProperty("subcmd", subcmd)
            addProperty("result", result.toHexString())
            if ("checkData" in global) {
                addProperty("bit", global["checkData"] as String)
                global.remove("checkData")
            }
            addProperty("stacktrace", stacktrace)
        }, source)
    }

    private fun postCallTokenWithD2Key(uin: String, d2key: ByteArray, stacktrace: String) {
        HttpUtil.postTo("callToken", JsonObject().apply {
            addProperty("type", TYPE_NATIVE_SET_ACCOUNT_KEY)
            addProperty("uin", uin)
            addProperty("d2key", d2key.toHexString())
            addProperty("stacktrace", stacktrace)
        }, source)
    }
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
