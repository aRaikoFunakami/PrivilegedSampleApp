package com.example.privilegedsampleapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.NumberPicker
import android.os.Handler
import android.os.Looper
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.CarPropertyManager.CarPropertyEventCallback
import android.car.hardware.CarPropertyValue
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager
    private lateinit var numberPicker: NumberPicker // NumberPickerをプロパティとして保持

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // NumberPicker init
        numberPicker = findViewById<NumberPicker>(R.id.numberPicker) ?: return

        numberPicker.minValue = 16
        numberPicker.maxValue = 32
        numberPicker.value = getTemperature().toInt()

        // CarProperty init
        val handler = Handler(Looper.getMainLooper())
        car = Car.createCar(this, handler)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        // Register CarPropertyManager listener
        carPropertyManager.registerCallback(
            carPropertyEventCallback,
            VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )

        // NumberPicker listener
        numberPicker.setOnValueChangedListener { _, _, newVal ->
            setTemperature(newVal.toFloat())
        }

        // BroadcastReceiverの登録
        val intentFilter = IntentFilter().apply {
            addAction("com.example.privilegedsampleapp.ACTION_GET_TEMPERATURE")
            addAction("com.example.privilegedsampleapp.ACTION_SET_TEMPERATURE")
        }
        registerReceiver(temperatureReceiver, intentFilter, Context.RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(temperatureReceiver)
        carPropertyManager.unregisterCallback(carPropertyEventCallback)
    }

    private val temperatureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                "com.example.privilegedsampleapp.ACTION_GET_TEMPERATURE" -> {
                    val targetTemp = getTemperature()
                    val resultIntent = Intent("com.example.privilegedsampleapp.RESULT_TEMPERATURE")
                    resultIntent.putExtra("TEMPERATURE_VALUE", targetTemp)
                    Log.d("MainActivity", "TEMPERATURE_VALUE: $targetTemp")
                    sendBroadcast(resultIntent)
                }
                "com.example.privilegedsampleapp.ACTION_SET_TEMPERATURE" -> {
                    val newValue = intent.getFloatExtra("TEMPERATURE_VALUE", -1f)
                    if (newValue in 16f..32f) {
                        setTemperature(newValue)
                        Log.d("MainActivity", "Temperature value: $newValue")
                        updateNumberPickerValue(newValue.toInt()) // NumberPickerの値を更新
                    } else {
                        Log.e("MainActivity", "Invalid temperature value: $newValue")
                    }
                }
                else -> Log.e("MainActivity", "Unknown action: ${intent.action}")
            }
        }
    }

    private fun getTemperature(): Float {
        return try {
            carPropertyManager.getFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x44).also {
                Log.d("MainActivity", "Temperature retrieved: $it")
            }
        } catch (e: Exception) {
            Log.e("CarProperty", "Error retrieving temperature: ${e.message}")
            -1f // Invalid value
        }
    }

    private fun setTemperature(value: Float) {
        try {
            // right temperature
            carPropertyManager.setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x44, value)
            // left temperature
            // carPropertyManager.setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x31, value)
            Log.d("MainActivity", "Temperature set to: $value")
        } catch (e: Exception) {
            Log.e("CarProperty", "Error setting temperature: ${e.message}")
        }
    }

    // NumberPicker
    private fun updateNumberPickerValue(value: Int) {
        runOnUiThread {
            if (value in numberPicker.minValue..numberPicker.maxValue) {
                numberPicker.value = value
            } else {
                Log.e("MainActivity", "Value $value out of NumberPicker range")
            }
        }
    }

    // CarPropertyManager listeners
    private val carPropertyEventCallback = object : CarPropertyEventCallback {
        // Update the value of the NumberPicker when the interior temperature setting is changed.
        override fun onChangeEvent(event: CarPropertyValue<*>) {
            if (event.propertyId == VehiclePropertyIds.HVAC_TEMPERATURE_SET) {
                val newValue = event.value as Float
                Log.d("MainActivity", "HVAC_TEMPERATURE_SET changed: $newValue")
                updateNumberPickerValue(newValue.toInt()) // NumberPickerの値を更新

                // Broadcast the temperature to Apps
                val resultIntent = Intent("com.example.privilegedsampleapp.TEMPERATURE_VALUE")
                resultIntent.putExtra("TEMPERATURE_VALUE", newValue)
                Log.d("MainActivity", "Broadcasting TEMPERATURE_VALUE with RESULT_VALUE: $newValue")
                sendBroadcast(resultIntent)
            }
        }

        override fun onErrorEvent(propertyId: Int, zone: Int) {
            if (propertyId == VehiclePropertyIds.HVAC_TEMPERATURE_SET) {
                Log.e("MainActivity", "Error occurred in HVAC_TEMPERATURE_SET property.")
            }
        }
    }
}