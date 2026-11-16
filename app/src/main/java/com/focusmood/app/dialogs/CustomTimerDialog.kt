package com.focusmood.app.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.focusmood.app.R

class CustomTimerDialog(
    context: Context,
    private val currentFocus: Int,
    private val currentShort: Int,
    private val currentLong: Int,
    private val onSave: (focus: Int, short: Int, long: Int) -> Unit
) : Dialog(context) {

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_custom_timer)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etFocus = findViewById<EditText>(R.id.etCustomFocus)
        val etShort = findViewById<EditText>(R.id.etCustomShort)
        val etLong = findViewById<EditText>(R.id.etCustomLong)
        val btnClose = findViewById<TextView>(R.id.btnDialogClose)
        val btnDefaults = findViewById<Button>(R.id.btnResetDefaults)
        val btnSave = findViewById<Button>(R.id.btnSaveCustomTimer)

        etFocus.setText(currentFocus.toString())
        etShort.setText(currentShort.toString())
        etLong.setText(currentLong.toString())

        // Presets for Focus
        findViewById<Button>(R.id.presetF15).setOnClickListener { etFocus.setText("15") }
        findViewById<Button>(R.id.presetF25).setOnClickListener { etFocus.setText("25") }
        findViewById<Button>(R.id.presetF45).setOnClickListener { etFocus.setText("45") }
        findViewById<Button>(R.id.presetF60).setOnClickListener { etFocus.setText("60") }
        findViewById<Button>(R.id.presetF90).setOnClickListener { etFocus.setText("90") }

        // Presets for Short Break
        findViewById<Button>(R.id.presetS3).setOnClickListener { etShort.setText("3") }
        findViewById<Button>(R.id.presetS5).setOnClickListener { etShort.setText("5") }
        findViewById<Button>(R.id.presetS10).setOnClickListener { etShort.setText("10") }

        // Presets for Long Break
        findViewById<Button>(R.id.presetL15).setOnClickListener { etLong.setText("15") }
        findViewById<Button>(R.id.presetL20).setOnClickListener { etLong.setText("20") }
        findViewById<Button>(R.id.presetL30).setOnClickListener { etLong.setText("30") }

        btnClose.setOnClickListener { dismiss() }

        btnDefaults.setOnClickListener {
            etFocus.setText("25")
            etShort.setText("5")
            etLong.setText("15")
        }

        btnSave.setOnClickListener {
            val focus = (etFocus.text.toString().toIntOrNull() ?: 25).coerceIn(1, 180)
            val short = (etShort.text.toString().toIntOrNull() ?: 5).coerceIn(1, 60)
            val long = (etLong.text.toString().toIntOrNull() ?: 15).coerceIn(1, 60)
            onSave(focus, short, long)
            dismiss()
        }
    }
}
