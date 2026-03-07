@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package moe.ore.txhook.app.model

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import moe.ore.protocol.SSOLoginMerge
import moe.ore.script.Consist
import moe.ore.txhook.app.CatchProvider
import moe.ore.txhook.helper.ZipUtil

object CaptureRepository {
    private val uiHandler = Handler(Looper.getMainLooper())

    val packets = mutableStateListOf<CapturePacket>()
    val actions = mutableStateListOf<CaptureAction>()

    var isCatchEnabled by mutableStateOf(Consist.isCatch)
        private set

    private var attached = false

    fun attachCatchProvider() {
        if (attached) return
        attached = true

        CatchProvider.catchHandler = object : CatchProvider.Companion.CatchHandler() {
            override fun handleMd5(source: Int, data: ByteArray, result: ByteArray) {
                addAction(CaptureAction(3).also {
                    it.buffer = data
                    it.result = result
                    it.source = source
                    it.from = true
                })
            }

            override fun handleTlvSet(tlv: Int, buf: ByteArray, source: Int) {
                addAction(CaptureAction(2).also {
                    it.buffer = buf
                    it.what = tlv
                    it.source = source
                    it.from = true
                })
            }

            override fun handleTlvGet(tlv: Int, buf: ByteArray, source: Int) {
                addAction(CaptureAction(2).also {
                    it.buffer = buf
                    it.what = tlv
                    it.source = source
                    it.from = false
                })
            }

            override fun handlePacket(time: Long, packet: CapturePacket) {
                if (packet.cmd == "SSO.LoginMerge") {
                    var buf = packet.buffer.let { it.sliceArray(4 until it.size) }
                    if (buf.firstOrNull() == 0x78.toByte()) {
                        buf = ZipUtil.unCompress(buf)
                    }
                    runCatching {
                        ProtoBuf.decodeFromByteArray<SSOLoginMerge.BusiBuffData>(buf)
                    }.onSuccess { merge ->
                        merge.buffList?.forEach {
                            addPacket(
                                CapturePacket(
                                    packet.from,
                                    it.cmd,
                                    it.seq,
                                    it.data,
                                    packet.time,
                                    packet.uin,
                                    packet.msgCookie,
                                    packet.type,
                                    packet.source,
                                    merge = false,
                                ),
                            )
                        }
                    }.onFailure {
                        addPacket(packet)
                    }
                } else {
                    addPacket(packet)
                }
            }

            override fun handleTea(isEnc: Boolean, data: ByteArray, key: ByteArray, result: ByteArray, source: Int) {
                addAction(CaptureAction(if (isEnc) 0 else 1).also {
                    it.buffer = data
                    it.result = result
                    it.from = !isEnc
                    it.source = source
                    it.key = key
                })
            }
        }
    }

    fun updateCatchEnabled(enabled: Boolean) {
        isCatchEnabled = enabled
        Consist.isCatch = enabled
    }

    fun clearPackets() {
        packets.clear()
    }

    fun clearActions() {
        actions.clear()
    }

    private fun addPacket(packet: CapturePacket) {
        uiHandler.post {
            if (isCatchEnabled) {
                packets.add(0, packet)
            }
        }
    }

    private fun addAction(action: CaptureAction) {
        uiHandler.post {
            if (isCatchEnabled) {
                actions.add(0, action)
            }
        }
    }
}

