package moe.ore.txhook.helper.parser

import org.json.JSONObject

class NewJsonObject : JSONObject() {
    override fun put(name: String, value: Int): JSONObject {
        val n = getNameByOldName(name)
        return super.put(n, value)
    }

    override fun put(name: String, value: Any?): JSONObject {
        val n = getNameByOldName(name)
        return super.put(n, value)
    }

    override fun put(name: String, value: Boolean): JSONObject {
        val n = getNameByOldName(name)
        return super.put(n, value)
    }

    override fun put(name: String, value: Double): JSONObject {
        val n = getNameByOldName(name)
        return super.put(n, value)
    }

    override fun put(name: String, value: Long): JSONObject {
        val n = getNameByOldName(name)
        return super.put(n, value)
    }

    private fun getNameByOldName(sourceName: String, index: Int = 0): String {
        val ret = if (has("$sourceName-$index")) {
            val ix = index + 1
            getNameByOldName(sourceName, ix)
        } else "$sourceName-$index"
        println("ret: $ret")
        return ret
    }
}