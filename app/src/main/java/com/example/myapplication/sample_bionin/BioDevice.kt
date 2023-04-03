package com.example.myapplication.sample_bionin

class BioDevice {
    companion object {
        const val baseUUID: String = "fee0"
        const val notificationCharacteristic: String = "fee2"
        const val writeCharacteristic: String = "fee1"
    }

    fun turnOn(): ByteArray {
        return listOf(0).map { it.toByte() }.toByteArray()
    }

    fun turnOff(): ByteArray {
        return listOf(1).map { it.toByte() }.toByteArray()
    }

    private fun generateCmd(cmd: Int, data: List<Int> = listOf()): ByteArray {
        val request = mutableListOf(0xB0, cmd)
        if (data.isNotEmpty()) {
            request += data
        }
        return appendOneByteCheckSumToCmd(request.map { it.toByte() }.toByteArray())
    }

    private fun appendOneByteCheckSumToCmd(sourceCmd: ByteArray): ByteArray {
        val array: MutableList<Int> = sourceCmd.toList().map { it.toInt() }.toMutableList()
        array += calOneByteCheckSum(sourceCmd)
        return array.map { it.toByte() }.toByteArray()
    }

    private fun calOneByteCheckSum(sourceCmd: ByteArray): Int {
        var sum = 0
        for (value in sourceCmd.toList().map { it.toInt() }) {
            sum += value
        }
        return sum
    }

    fun getModel(): ByteArray {
        return generateCmd(0)
    }

    fun decodeModelName(value: ByteArray): String {
        var data = value.map { it.toInt().and(255) }
        data = data.subList(2, 7)
        return String(data.map { it.toByte() }.toByteArray())
    }
}