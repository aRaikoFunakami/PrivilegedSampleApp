package com.example.privilegedsampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.TextView
import android.widget.NumberPicker

import android.os.Handler
import android.os.Looper
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.util.Log
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // NumberPicker and TextView
        val numberPicker = findViewById<NumberPicker>(R.id.numberPicker) ?: return
        val selectedNumberText = findViewById<TextView>(R.id.selectedNumberText) ?: return

        // NumberPicker
        numberPicker.minValue = 16
        numberPicker.maxValue = 32
        numberPicker.value = 22
        selectedNumberText.text = String.format(Locale.getDefault(), "Selected Number: %d", numberPicker.value)

        // CarProperty
        val handler = Handler(Looper.getMainLooper())
        car = Car.createCar(this, handler)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        // get the value of the right set temperature and display it in TextView.
        // it needs privilege: CONTROL_CAR_CLIMATE
        // mAreaId=0x44 : Temperature setting display on the right
        // mAreaId=0x31 : Temperature setting display on the left
        try {
            val targetTemp: Float = carPropertyManager.getFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x44)
            Log.d("getFloatProperty HVAC_TEMPERATURE_SET: ", targetTemp.toString())
            numberPicker.value = targetTemp.toInt()
            selectedNumberText.text = String.format(Locale.getDefault(), "Selected Number: %d", numberPicker.value)
        } catch (e: SecurityException) {
            Log.e("CarProperty", "Permission denied for HVAC_TEMPERATURE_SET: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("CarProperty", "Invalid property ID or area ID for HVAC_TEMPERATURE_SET: ${e.message}")
        }

        // NumberPicker
        numberPicker.apply {
            setOnValueChangedListener { _, _, newVal ->
                selectedNumberText.text = String.format(Locale.getDefault(), "Selected Number: %d", newVal)
                try {
                    // set a temperature value to right and left
                    carPropertyManager.setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x44, newVal.toFloat())
                    carPropertyManager.setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x31, newVal.toFloat())
                    Log.d("setFloatProperty HVAC_TEMPERATURE_SET", "Value successfully set to: $newVal")
                } catch (e: SecurityException) {
                    Log.e("CarProperty", "Permission denied for setting HVAC_TEMPERATURE_SET: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    Log.e("CarProperty", "Invalid property ID or area ID for setting HVAC_TEMPERATURE_SET: ${e.message}")
                }
            }
        }
    }
}