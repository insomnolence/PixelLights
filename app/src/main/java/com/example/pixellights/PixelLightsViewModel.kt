package com.example.pixellights

import android.bluetooth.*
import android.graphics.Color
import android.graphics.Point
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import java.util.*
import kotlin.math.roundToInt

class PixelLightsViewModel : ViewModel() {

    // The next series of variables hold data
    // to be used when setting parameters from the
    // Manual Fragment
    var colorPoint1 : Point? = null
    var colorPoint2 : Point? = null
    var colorPoint3 : Point? = null

    // Set up the values being tracked across the fragments.
    var color1 = Color.RED
    var color2 = Color.WHITE
    var color3 = Color.GREEN

    var patternValue = Packet.Pattern.MiniTwinkle

    var intensityValue = 128
    var rateValue = 100
    var levelValue = 128

    var bluetoothGatt : BluetoothGatt? = null
    var bluetoothDeviceAddress : String = ""
    var bluetoothDevice : BluetoothDevice? = null


    //
    // Routines for sending packets on Bluetooth
    //
    private val pixelModelJob = SupervisorJob()
    private val pixelModelCoroutineScope = CoroutineScope((Dispatchers.IO + pixelModelJob))
    private var job : Job? = null

    // Create Channel for callback to notify Packet Sender to send next packet
    val packetChannel = Channel<String>(CONFLATED)

    class BluetoothPacketSender(private val gatt: BluetoothGatt, packet: Packet )  {

        private val UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val UART_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private var bytesToSend : ByteArray?
        private var bytesToSendPosition : Int

        init {
            this.bytesToSend = packet.createBytes()!!
            this.bytesToSendPosition = 0
        }

        fun writeCharacteristic( ) {

            val gattService = gatt.getService(UART_SERVICE)
            if (gattService == null) {
                Log.w("BluetoothPacketSender", "UART Service not found")
                return
            }
            val gattChar = gattService.getCharacteristic(UART_TX)
            if (gattChar == null) {
                Log.w("BluetoothPacketSender", "Tx Characteristic not found")
                return
            }

            // Check to see if there is anything to write
            if ( bytesToSend != null ) {
                // Message to send
                val num: Int = bytesToSendPosition / 20 + 1

                val sendByte = ByteArray(
                    Integer.min(
                        20,
                        bytesToSend!!.size - bytesToSendPosition
                    )
                )

                bytesToSend!!.copyInto(
                    sendByte,
                    0,
                    bytesToSendPosition,
                    Integer.min(
                        bytesToSendPosition + 20,
                        bytesToSend!!.size - 1
                    )
                )

                bytesToSendPosition += 20
                if (bytesToSendPosition > bytesToSend!!.size - 1) {
                    // Clear out the information
                    bytesToSend = null
                    bytesToSendPosition = 0
                }

                Log.i("BluetoothPacketSender", "Sending packet $num with ${sendByte.size}")

                gattChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE //writeType
                gattChar.value = sendByte
                gatt.writeCharacteristic(gattChar)

            }
        }

    }

    // Convert a progress to a number between 0 - 255
    fun convertFromProgress(progress : Int) : Int {
        return (progress * 2.55).roundToInt()
    }

    fun convertToProgress(value : Int ) : Int {
        return (value / 2.55 ).roundToInt()
    }

    @InternalCoroutinesApi
    fun usePattern(pattern : String)
    {
        Log.w("usePattern","Send a job for $pattern")
        if (job?.isActive == true)
        {
            job?.cancel()
            job = null
        }

        if (bluetoothDevice == null )
        {
            Log.w("usePattern", "Bluetooth device is null")
        }

        // Lookup the Pattern that is already been created and send it
        // to a co-routine to send it to the device
        val patternSteps = patterns[pattern]
        Log.w("usePattern", "Launch")
        job = pixelModelCoroutineScope.launch {
            withContext(Dispatchers.IO) {
                if (bluetoothGatt == null)
                {
                    Log.w("usePattern", "Gatt is null ?!")
                }

                if (patternSteps != null) {
                    bluetoothGatt?.let { sendPattern(patternSteps, it) }
                }
            }
        }
    }

    @InternalCoroutinesApi
    suspend fun sendPattern(steps : Steps, bluetoothGatt: BluetoothGatt)
    {
        for (i in steps) {
            Log.w(
                "UsePattern", "Pattern step duration: ${i.duration}, speed: ${i.pattern.speed} " +
                        ", brightness: ${i.pattern.brightness}, pattern: ${i.pattern.pattern}, level: ${i.pattern.level}"
            )
            if (!isActive) {
                return
            }

            val bluetoothSender = BluetoothPacketSender(bluetoothGatt, i.pattern)
            Log.w("UsePattern", "writing packet")
            bluetoothSender.writeCharacteristic()

            delay(i.duration.toLong() * 1000)
        }
    }

    @InternalCoroutinesApi
    fun processPacketInformation(controlPacket : Boolean = false)
    {
        // Get the appropriate information and send it along
        val packetToSend = Steps()

        if ( controlPacket ) {
            val step = Step(
                0, Packet(
                    Packet.PixelCommand.HC_CONTROL, intensityValue.toUByte(),
                    rateValue.toUByte(), patternValue, color1, color2, color3, levelValue.toUByte()
                )
            )

            packetToSend.addStep(step)
        }
        else {
            packetToSend.addStep(
                0,
                intensityValue.toUByte(),
                rateValue.toUByte(),
                levelValue.toUByte(),
                patternValue,
                color1,
                color2,
                color3
            )
        }

        job = pixelModelCoroutineScope.launch {
            withContext(Dispatchers.IO) {
                if (bluetoothGatt == null)
                {
                    Log.w("usePattern", "Gatt is null ?!")
                }

                bluetoothGatt?.let { sendPattern(packetToSend, it) }
            }
        }

    }

    val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    bluetoothDeviceAddress = deviceAddress
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()

                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w(
                    "BluetoothGattCallback",
                    "Discovered ${services.size} services for ${device.address}"
                )
                printGattTable() // See implementation just above this section
                // Consider connection setup as complete here


                // Change mtu?
                gatt.requestMtu(517)

            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i(
                            "BluetoothGattCallback",
                            "Wrote to characteristic $uuid | value: $value"
                        )
                        // Send the next packet
                        //writeCharacteristic( )
                        packetChannel.offer("Continue")
                        Log.d("BluetoothGattCallback","Wrote characteristic, checking for next.")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e(
                            "BluetoothGattCallback",
                            "Characteristic write failed for $uuid, error: $status"
                        )
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.w(
                "BluetoothGattCallback",
                "ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}"
            )

        }

    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    //
    // Information for light colors, patterns, speed, etc.
    //

    class Step(val duration: Int, val pattern : Packet)
    class Steps : ArrayList<Step>()
    {
        fun addStep(duration: Int, brightness : UByte, speed : UByte,
                    level1 : UByte, pattern : Packet.Pattern, color1 : Int,
                    color2 : Int, color3 : Int )
        {
            this.add(
                Step(duration, Packet(Packet.PixelCommand.HC_PATTERN, brightness, speed,
                pattern, color1, color2, color3, level1))
            )
        }

        fun addStep(step : Step)
        {
            this.add(step)
        }
    }

    private var patterns = mutableMapOf<String, Steps>()

    fun createPatterns() {
        val idle = Step(
            0, Packet(
                Packet.PixelCommand.HC_PATTERN, 20u,
                35u, Packet.Pattern.Gradient, Color.RED, Color.WHITE, Color.GREEN, 17u
            )
        )

        // Warning
        //Step( duration(secs), brightness(0-255), speed(%), level1, pattern, color1, color2, color3 )


        val warning : () -> Steps =
        {
            val steps = Steps()
            steps.addStep(
                4, 255u, 100u, 255u, Packet.Pattern.Flash,
                Color.YELLOW, Color.YELLOW, Color.YELLOW
            )
            steps.addStep(
                60, 255u, 40u, 34u, Packet.Pattern.March,
                Color.YELLOW, Color.YELLOW, Color.YELLOW
            )
            steps.addStep(
                60, 255u, 100u, 75u,
                Packet.Pattern.MiniTwinkle, Color.YELLOW, Color.rgb(255, 255, 64),
                Color.YELLOW
            )
            steps.addStep(
                0, 127u, 75u, 75u, Packet.Pattern.Gradient,
                Color.YELLOW, Color.rgb(255, 255, 64), Color.YELLOW
            )
            steps
        }

        patterns["Warning"] = warning()

        val exit : () -> Steps =
        {
            val steps = Steps()
            steps.addStep( 4, 255u, 100u, 255u, Packet.Pattern.Flash,
                Color.RED, Color.RED, Color.RED )
            steps.addStep( 60, 255u,  40u,  34u, Packet.Pattern.March,
                Color.RED, Color.RED, Color.RED )
            steps.addStep( 60, 255u, 100u,  75u,
                Packet.Pattern.MiniTwinkle, Color.RED, Color.rgb( 255, 64, 64 ),
                Color.RED )
            steps.addStep( 0, 127u,  75u,  75u,
                Packet.Pattern.Gradient, Color.RED, Color.rgb( 255, 64, 64 ),
                Color.RED )
            steps
        }

        patterns["Exit"] = exit()

        val idlePattern : () -> Steps =
        {
            val steps = Steps()
            steps.addStep(idle)
            steps
        }

        patterns["Idle"] = idlePattern()

        val rwrSubtle : () -> Steps =
        {
            val steps = Steps()
            steps.addStep( 30, 255u, 35u, 17u, Packet.Pattern.Gradient,
                Color.RED, Color.WHITE, Color.RED )
            steps.addStep( idle )
            steps
        }

        patterns["RWR Subtle"] = rwrSubtle()

        val blueSmooth : () -> Steps =
        {
            val steps = Steps()
            steps.addStep( 30, 255u,  75u,  75u,
                Packet.Pattern.Gradient, Color.BLUE, Color.rgb( 128, 128, 255 ),
                Color.BLUE )
            steps.addStep( idle )
            steps
        }

        patterns["Blue Smooth"] = blueSmooth()

        val rwbParis : () -> Steps = {
            val steps = Steps()
            steps.addStep( 10, 255u, 160u, 160u,
                Packet.Pattern.MiniTwinkle, Color.RED, Color.WHITE, Color.BLUE )
            steps.addStep( idle )
            steps
        }

        patterns["RWB Paris"] = rwbParis()

        val rwgCandy : () -> Steps = {
            val steps = Steps()
            steps.addStep( 30, 127u, 65u, 255u,
                Packet.Pattern.CandyCane, Color.RED, Color.WHITE, Color.GREEN )
            steps.addStep( idle )
            steps
        }

        patterns["RWG Candy"] = rwgCandy()

        val rwrCandy : () -> Steps = {
            val steps = Steps()
            steps.addStep(30, 127u, 100u, 255u,
                Packet.Pattern.CandyCane, Color.RED, Color.WHITE, Color.RED )
            steps.addStep( idle )
            steps
        }

        patterns["RWR Candy"] = rwrCandy()

        val rwgTree : () -> Steps = {
            val steps = Steps()
            steps.addStep(10, 255u, 100u, 255u, Packet.Pattern.Fixed,
                Color.RED, Color.WHITE, Color.GREEN )
            steps.addStep( idle )
            steps
        }

        patterns["RWG Tree"] = rwgTree()

        val rwgMarch : () -> Steps = {
            val steps = Steps()
            steps.addStep(30, 255u, 127u, 8u, Packet.Pattern.March,
                Color.RED, Color.WHITE, Color.GREEN )
            steps.addStep( idle )
            steps
        }

        patterns["RWG March"] = rwgMarch()

        val rwgWipe : () -> Steps = {
            val steps = Steps()
            steps.addStep(30, 255u, 127u, 8u, Packet.Pattern.Wipe,
                Color.RED, Color.WHITE, Color.GREEN )
            steps.addStep( idle )
            steps
        }

        patterns["RWG Wipe"] = rwgWipe()

        val rwgFlicker : () -> Steps = {
            val steps = Steps()
            steps.addStep(10, 255u, 255u, 9u,
                Packet.Pattern.MiniSparkle, Color.RED, Color.WHITE, Color.GREEN )
            steps.addStep( idle )
            steps
        }

        patterns["RWG Flicker"] = rwgFlicker()

        val cga : () -> Steps = {
            val steps = Steps()
            steps.addStep(30, 255u, 100u, 128u,
                Packet.Pattern.MiniTwinkle, Color.CYAN, Color.MAGENTA, Color.YELLOW )
            steps.addStep( idle )
            steps
        }

        patterns["CGA"] = cga()

        val rainbow : () -> Steps = {
            val steps = Steps()
            steps.addStep(10, 255u, 100u, 255u, Packet.Pattern.Rainbow,
                Color.WHITE, Color.WHITE, Color.WHITE )
            steps.addStep( idle )
            steps
        }

        patterns["Rainbow"] = rainbow()

        val strobe : () -> Steps = {
            val steps = Steps()
            steps.addStep(10, 255u, 128u, 255u, Packet.Pattern.Strobe,
                Color.WHITE, Color.WHITE, Color.WHITE )
            steps.addStep( idle )
            steps
        }

        patterns["Strobe"] = strobe()

    }


}