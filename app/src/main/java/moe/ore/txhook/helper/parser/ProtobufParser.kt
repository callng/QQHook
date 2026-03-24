package moe.ore.txhook.helper.parser

import com.google.protobuf.ByteString
import com.google.protobuf.UnknownFieldSet
import com.google.protobuf.WireFormat
import moe.ore.txhook.helper.sub
import moe.ore.txhook.helper.toHexString

class ProtobufParser
@JvmOverloads
constructor(buffer: ByteArray, pos: Int = 0) {

    private val buffer = buffer.sub(pos, buffer.size - pos)

    fun start(): NewJsonObject {
        val objects = NewJsonObject()
        val fieldSet = UnknownFieldSet.parseFrom(buffer)

        printUnknownFields(fieldSet, objects)

        return objects
    }

    private fun printUnknownFields(fieldSet: UnknownFieldSet, objects: NewJsonObject) {
        fieldSet.asMap().entries.forEach {
            val number = it.key
            val field = it.value

            printUnknownField(number, WireFormat.WIRETYPE_VARINT, field.varintList, objects)
            printUnknownField(number, WireFormat.WIRETYPE_FIXED32, field.fixed32List, objects)
            printUnknownField(number, WireFormat.WIRETYPE_FIXED64, field.fixed64List, objects)
            printUnknownField(
                number,
                WireFormat.WIRETYPE_LENGTH_DELIMITED,
                field.lengthDelimitedList,
                objects
            )

            field.groupList.forEach { value ->
                val jsonObject = NewJsonObject()
                printUnknownFields(value, jsonObject)
                objects.put(number.toString(), jsonObject)
            }
        }
    }

    private fun printUnknownField(
        number: Int,
        wireType: Int,
        values: List<*>,
        objects: NewJsonObject
    ) {
        values.forEach {
            printUnknownFieldValue(number, wireType, it, objects)
        }
    }

    private fun printUnknownFieldValue(number: Int, tag: Int, value: Any?, objects: NewJsonObject) {
        when (WireFormat.getTagWireType(tag)) {
            WireFormat.WIRETYPE_VARINT -> objects.put(number.toString(), value as Long)
            WireFormat.WIRETYPE_FIXED32 -> objects.put(number.toString(), value as Int)
            WireFormat.WIRETYPE_FIXED64 -> objects.put(number.toString(), value as Long)
            WireFormat.WIRETYPE_LENGTH_DELIMITED -> {
                val v = value as ByteString

                kotlin.runCatching {
                    val json = NewJsonObject()
                    val msg = UnknownFieldSet.parseFrom(v)
                    printUnknownFields(msg, json)
                    objects.put(number.toString(), json)
                }.onFailure {
                    objects.put(number.toString(), "[hex]${v.toByteArray().toHexString(false)}")
                }

            }

            WireFormat.WIRETYPE_START_GROUP -> {
                val json = NewJsonObject()
                printUnknownFields(value as UnknownFieldSet, json)
                objects.put(number.toString(), json)
            }

            else -> throw RuntimeException("can not to parse the protobuf data")
        }
    }
}