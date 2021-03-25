package com.example.pixellights.ui.preset

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.pixellights.R
import com.example.pixellights.PixelLightsViewModel
import kotlinx.android.synthetic.main.fragment_preset.*
import kotlinx.coroutines.InternalCoroutinesApi

class PresetFragment : Fragment() {

    private val pixelViewModel: PixelLightsViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_preset, container, false)

        println("Value from intensity is ${pixelViewModel.intensityValue}")
        println("Value from rate is ${pixelViewModel.rateValue}")
        return root
    }

    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        println("Value from intensity is ${pixelViewModel.intensityValue}")
        println("Value from rate is ${pixelViewModel.rateValue}")

        Log.w("Created Preset","Gatt address: ${pixelViewModel.bluetoothGatt?.device?.address}")

        // Set up the buttons.
        val idleButton = idle as Button
        idleButton.setOnClickListener {
            pixelViewModel.usePattern("Idle")
        }

        val warningButton = warning as Button
        warningButton.setOnClickListener {
            pixelViewModel.usePattern("Warning")
        }

        val exitButton = exit as Button
        exitButton.setOnClickListener {
            pixelViewModel.usePattern("Exit")
        }

        val rwrSubtleButton = rwr_subtle as Button
        rwrSubtleButton.setOnClickListener {
            pixelViewModel.usePattern("RWR Subtle")
        }

        val rwbParisButton = rwb_paris_usa as Button
        rwbParisButton.setOnClickListener {
            pixelViewModel.usePattern("RWB Paris")
        }

        val blueSmoothButton = blue_smooth as Button
        blueSmoothButton.setOnClickListener {
            pixelViewModel.usePattern("Blue Smooth")
        }

        val rwrCandyButton = rwr_candy as Button
        rwrCandyButton.setOnClickListener {
            pixelViewModel.usePattern("RWR Candy")
        }

        val rwgCandyButton = rwg_candy as Button
        rwgCandyButton.setOnClickListener {
            pixelViewModel.usePattern("RWG Candy")
        }

        val rwgTreeButton = rwg_tree as Button
        rwgTreeButton.setOnClickListener {
            pixelViewModel.usePattern("RWG Tree")
        }

        val rwgMarchButton = rwg_march as Button
        rwgMarchButton.setOnClickListener {
            pixelViewModel.usePattern("RWG March")
        }

        val rwgWipeButton = rwg_wipe as Button
        rwgWipeButton.setOnClickListener {
            pixelViewModel.usePattern("RWG Wipe")
        }

        val rwgFlickerButton = rwg_flicker as Button
        rwgFlickerButton.setOnClickListener {
            pixelViewModel.usePattern("RWG Flicker")
        }

        val cgaButton = cga as Button
        cgaButton.setOnClickListener {
            pixelViewModel.usePattern("CGA")
        }

        val rainbowButton = rainbow as Button
        rainbowButton.setOnClickListener {
            pixelViewModel.usePattern("Rainbow")
        }

        val strobeButton = strobe as Button
        strobeButton.setOnClickListener {
            pixelViewModel.usePattern("Strobe")
        }

    }
}
