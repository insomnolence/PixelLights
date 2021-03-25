package com.example.pixellights.ui.manual

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.pixellights.Packet
import com.example.pixellights.PixelLightsViewModel
import com.example.pixellights.R
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*


class ManualFragment : Fragment() {

    private val pixelViewModel: PixelLightsViewModel by activityViewModels()
    private lateinit var thumbView: View
    //private val pixelViewModel: PixelLightsViewModel by activity?.viewModels<PixelLightsViewModel>()

    class ColorView : androidx.appcompat.widget.AppCompatImageView {

        var userPoint: Point = Point(0, 0)
        private lateinit var dotPaint: Paint
        var colorValue : Int = Color.RED

        constructor(context: Context) : super(context)
        {
            initView()
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
        {
            initView()
        }

        private fun initView()
        {
            // Set up the Paint for the selection circle
            dotPaint = Paint()
            dotPaint.color = Color.BLACK
            dotPaint.style = Paint.Style.STROKE
            dotPaint.strokeWidth = 10F
        }

        override fun onDraw(canvas: Canvas?)
        {
            super.onDraw(canvas)
            canvas?.drawCircle(userPoint.x.toFloat(), userPoint.y.toFloat(), 20F, dotPaint)
        }

        private fun getBitmapForView( ) : Bitmap
        {
            val bitmap: Bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            this.draw(canvas)
            return bitmap
        }

        override fun onTouchEvent(event: MotionEvent) : Boolean
        {
            if (event.action == MotionEvent.ACTION_DOWN ||
                event.action == MotionEvent.ACTION_MOVE) {

                val bitmap = getBitmapForView()

                var x = event.x.toInt()
                var y = event.y.toInt()

                if ( x < 0 )
                {
                    x = 0
                }
                else if ( x >= bitmap.width)
                {
                    x = bitmap.width - 1
                }

                if ( y < 0 )
                {
                    y = 0
                }
                else if ( y >= bitmap.height )
                {
                    y = bitmap.height - 1
                }

                val pixel = bitmap.getPixel(x, y)

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if ( r != 0 && b != 0 && g != 0)
                {
                    // Don't update unless it's a valid color
                    userPoint.x = x
                    userPoint.y = y
                    colorValue = pixel
                    invalidate()
                }

                /* Another way to get color by calculating HSV given the ColorWheel
                   image . . . not sure on accuracy.

                val centerX = width / 2
                val centerY = height / 2

                var angle = atan2(event.x - centerX, centerY - event.y)
                if ( angle < 0 )
                {
                    angle += 2* PI.toFloat()
                }

                val hue = 360 * angle / (2 * PI.toFloat())
                val radius = sqrt( (event.x - centerX).pow(2) + (event.y - centerY).pow(2))

                var sat = radius / ( width/2)
                if ( sat > 1 ) {
                    sat = 1f
                }

                val hsv = floatArrayOf(hue, sat, 1f)
                val testColor = Color.HSVToColor(hsv)
               */

            }

            return true
        }

        fun setColorPoint(color : Int) {

            // Since we are looking at pixels in the bitmap, there is no guarantee that the
            // exact color integer is going to be found. Simply converting to Red, Green, Blue
            // will find a number of matches. To find the "closest" one, sort them and pull the
            // Point from the middle ( or as close to it ) of the values and use that.

            // todo: Possibly use actual math and HSV to RBG and X,Y algorithms for the magic

            val pixelMap : SortedMap<Int, Point> = sortedMapOf()
            val bitmap = getBitmapForView()
            for (x in 0 until bitmap.width) {
                for (y in 0 until bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)

                    if (pixel != 0 && Color.red(pixel) == Color.red(color) &&
                        Color.green(pixel) == Color.green(color) &&
                        Color.blue(pixel) == Color.blue(color)
                    ) {
                        pixelMap[pixel] = Point(x,y)
                    }
                }
            }

            val num = pixelMap.count()
            userPoint = if ( num > 0) {

                val mapKeys = pixelMap.keys.toIntArray()

                val printMedian = pixelMap[mapKeys[num / 2]]!!

                Log.w("color point", "Median: $printMedian")

                printMedian
            } else {
                Point(width/2, height/2)
            }
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_manual, container, false)

        thumbView = inflater.inflate(R.layout.seekbar_custom_thumb, container, false)

        return root
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intensity = view.findViewById<SeekBar>(R.id.intensity)
        println("Trying to set intensity value as ${pixelViewModel.intensityValue}")
        intensity.progress = pixelViewModel.convertToProgress(pixelViewModel.intensityValue)
        intensity.thumb = getThumb(intensity.progress)

        intensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.thumb = getThumb(progress)

                pixelViewModel.intensityValue = pixelViewModel.convertFromProgress(progress)
                //pixelViewModel.processPacketInformation()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                pixelViewModel.processPacketInformation(true)
            }
        })

        val rate = view.findViewById<SeekBar>(R.id.rate)
        rate.progress = pixelViewModel.convertToProgress(pixelViewModel.rateValue)
        rate.thumb = getThumb(rate.progress)

        rate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.thumb = getThumb(progress)

                pixelViewModel.rateValue = pixelViewModel.convertFromProgress(progress)
                println("Should have set rate to $progress, but viewModel has ${pixelViewModel.rateValue}")
                //pixelViewModel.processPacketInformation()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                pixelViewModel.processPacketInformation(true)
            }
        })

        val level = view.findViewById<SeekBar>(R.id.level)
        level.progress = pixelViewModel.convertToProgress(pixelViewModel.levelValue)
        level.thumb = getThumb(level.progress)

        level.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                seekBar?.thumb = getThumb(progress)

                pixelViewModel.levelValue = pixelViewModel.convertFromProgress(progress)
                //pixelViewModel.processPacketInformation()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                pixelViewModel.processPacketInformation()
            }
        })

        val color1 = view.findViewById<ColorView>(R.id.color1)
        color1.invalidate()

        color1.doOnLayout {

            if ( pixelViewModel.colorPoint1 == null)
            {
                color1.setColorPoint(pixelViewModel.color1)
            }
            else {
                color1.userPoint = pixelViewModel.colorPoint1!!
            }


            color1.invalidate()
        }

        color1.setOnTouchListener { v, event ->
            v.onTouchEvent(event)

            if (color1.userPoint != pixelViewModel.colorPoint1) {
                pixelViewModel.colorPoint1 = Point(color1.userPoint.x, color1.userPoint.y)
                pixelViewModel.color1 = color1.colorValue
            }

            if (event?.action == MotionEvent.ACTION_UP) {
                pixelViewModel.processPacketInformation()
            }

            true
        }

        val color2 = view.findViewById<ColorView>(R.id.color2)
        color2.invalidate()

        color2.doOnLayout {

            if ( pixelViewModel.colorPoint2 == null)
            {
                color2.setColorPoint(pixelViewModel.color2)
            }
            else {
                color2.userPoint = pixelViewModel.colorPoint2!!
            }


            color2.invalidate()
        }

        color2.setOnTouchListener { v, event ->
            v.onTouchEvent(event)

            if (color2.userPoint != pixelViewModel.colorPoint2) {
                pixelViewModel.colorPoint2 = Point(color2.userPoint.x, color2.userPoint.y)
                pixelViewModel.color2 = color2.colorValue
            }

            if (event?.action == MotionEvent.ACTION_UP) {
                pixelViewModel.processPacketInformation()
            }

            true
        }

        val color3 = view.findViewById<ColorView>(R.id.color3)
        color3.invalidate()

        color3.doOnLayout {

            if ( pixelViewModel.colorPoint3 == null)
            {
                color3.setColorPoint(pixelViewModel.color3)
            }
            else {
                color3.userPoint = pixelViewModel.colorPoint3!!
            }


            color3.invalidate()
        }

        color3.setOnTouchListener { v, event ->
            v.onTouchEvent(event)

            if (color3.userPoint != pixelViewModel.colorPoint3) {
                pixelViewModel.colorPoint3 = Point(color3.userPoint.x, color3.userPoint.y)
                pixelViewModel.color3 = color3.colorValue
            }

            if (event?.action == MotionEvent.ACTION_UP) {
                pixelViewModel.processPacketInformation()
            }

            true
        }

        val patternSelect = view.findViewById<Spinner>(R.id.pattern_spinner)
        //patternSelect.setSelection(pixelViewModel.patternValue.ordinal)

        patternSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Call process packet for this change to be sent
                pixelViewModel.patternValue = Packet.Pattern.values()[position]
                pixelViewModel.processPacketInformation()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }
    }

    fun getThumb(progress: Int): Drawable {
        (thumbView.findViewById(R.id.barProgress) as TextView).text = progress.toString() + ""
        thumbView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(
            thumbView.measuredWidth,
            thumbView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        thumbView.layout(0, 0, thumbView.measuredWidth, thumbView.measuredHeight)
        thumbView.draw(canvas)
        return BitmapDrawable(resources, bitmap)
    }


}
