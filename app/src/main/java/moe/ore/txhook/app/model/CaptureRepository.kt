@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package moe.ore.txhook.app.model

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import moe.ore.protocol.SSOLoginMerge
import moe.ore.script.Consist
import moe.ore.txhook.app.CatchProvider
import moe.ore.txhook.helper.ZipUtil
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object CaptureRepository {
    private val uiHandler = Handler(Looper.getMainLooper())

    val packets = mutableStateListOf<CapturePacket>()
    val actions = mutableStateListOf<CaptureAction>()

    var packetGeneration by mutableIntStateOf(0)
        private set
    var actionGeneration by mutableIntStateOf(0)
        private set

    var isCatchEnabled by mutableStateOf(Consist.isCatch)
        private set

    private var attached = false

    private val _packetCounter = AtomicInteger(0)
    private val _actionCounter = AtomicInteger(0)

    private val _nextItemId = AtomicLong(1)
    fun nextItemId(): Long = _nextItemId.getAndIncrement()

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
                    val buf = if (packet.buffer.size > 4) {
                        packet.buffer.sliceArray(4 until packet.buffer.size)
                    } else {
                        packet.buffer
                    }
                    val finalBuf = if (buf.firstOrNull() == 0x78.toByte()) {
                        runCatching { ZipUtil.unCompress(buf) }.getOrDefault(buf)
                    } else {
                        buf
                    }
                    runCatching {
                        ProtoBuf.decodeFromByteArray<SSOLoginMerge.BusiBuffData>(finalBuf)
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
        packetGeneration++
        _packetCounter.set(0)
    }

    fun clearActions() {
        actions.clear()
        actionGeneration++
        _actionCounter.set(0)
    }

    private fun addPacket(packet: CapturePacket) {
        val id = nextItemId()
        uiHandler.post {
            if (isCatchEnabled) {
                packet.uid = id
                packets.add(0, packet)
                packetGeneration++
            }
        }
    }

    private fun addAction(action: CaptureAction) {
        val id = nextItemId()
        uiHandler.post {
            if (isCatchEnabled) {
                action.uid = id
                actions.add(0, action)
                actionGeneration++
            }
        }
    }
}
