package com.example.pixellights

import java.nio.ByteBuffer
import java.nio.ByteOrder

class Packet(
    private var command: PixelCommand,
    var brightness: UByte,
    var speed: UByte,
    var pattern: Pattern,
    color1: Int,
    color2: Int,
    color3: Int,
    level1: UByte
) {

    enum class PixelCommand(val code: UByte){
        HC_NONE(0u),     // No Command
        HC_CONTROL(1u),  // Brightness and Speed
        HC_PATTERN(2u)   // Pattern
    }

    enum class Pattern(val code: UByte) {
        MiniTwinkle(0u),
        MiniSparkle(1u),
        Sparkle(2u),
        Rainbow(3u),
        Flash(4u),
        March(5u),
        Wipe(6u),
        Gradient(7u),
        Fixed(8u),
        Strobe(9u),
        CandyCane(10u)
    }

    var color: UIntArray = UIntArray(3)
    var level: UByteArray = UByteArray(3)

    /*
    init {
        color[0] = Color.RED.toUInt()
        color[1] = Color.WHITE.toUInt()
        color[2] = Color.GREEN.toUInt()
        level[0] = 0u
        level[1] = 0u
        level[2] = 0u
    }
     */

    fun createBytes( ): ByteArray? {
        val buffer: ByteBuffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(command.code.toByte())
        buffer.put(brightness.toByte())
        buffer.put(speed.toByte())
        buffer.put(pattern.code.toByte())
        for(a in color) {
            buffer.putInt(a.toInt())
        }
        for (a in level) {
            buffer.put(a.toByte())
        }
        return buffer.array()
    }

    init {
        color[0] = color1.toUInt()
        color[1] = color2.toUInt()
        color[2] = color3.toUInt()
        level[0] = level1
        level[1] = 255u
        level[2] = 255u
    }
}