package com.empty.fakeisland

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = IslandPrefs.of(this)
        status = findViewById(R.id.status)

        findViewById<Button>(R.id.btnEnable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<MaterialSwitch>(R.id.swShow).apply {
            isChecked = prefs.getBoolean(IslandPrefs.KEY_ENABLED, true)
            setOnCheckedChangeListener { _, on ->
                prefs.edit { putBoolean(IslandPrefs.KEY_ENABLED, on) }
            }
        }
        findViewById<MaterialSwitch>(R.id.swLandscape).apply {
            isChecked = prefs.getBoolean(IslandPrefs.KEY_HIDE_LANDSCAPE, IslandPrefs.DEF_HIDE_LANDSCAPE)
            setOnCheckedChangeListener { _, on ->
                prefs.edit { putBoolean(IslandPrefs.KEY_HIDE_LANDSCAPE, on) }
            }
        }

        bindField(R.id.seekWidth, R.id.editWidth, R.id.lblWidth, R.string.label_width,
            IslandPrefs.KEY_WIDTH, IslandPrefs.DEF_WIDTH,
            sliderMin = 40f, sliderMax = 400f, hardMin = 1f, hardMax = 1000f)
        bindField(R.id.seekHeight, R.id.editHeight, R.id.lblHeight, R.string.label_height,
            IslandPrefs.KEY_HEIGHT, IslandPrefs.DEF_HEIGHT,
            sliderMin = 16f, sliderMax = 90f, hardMin = 1f, hardMax = 400f)
        bindField(R.id.seekX, R.id.editX, R.id.lblX, R.string.label_x,
            IslandPrefs.KEY_X, IslandPrefs.DEF_X,
            sliderMin = -200f, sliderMax = 200f, hardMin = -1000f, hardMax = 1000f)
        bindField(R.id.seekY, R.id.editY, R.id.lblY, R.string.label_y,
            IslandPrefs.KEY_Y, IslandPrefs.DEF_Y,
            sliderMin = 0f, sliderMax = 150f, hardMin = -100f, hardMax = 1000f)

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            prefs.edit { clear() }
            recreate()
        }
    }

    override fun onResume() {
        super.onResume()
        status.setText(if (isServiceEnabled()) R.string.status_on else R.string.status_off)
    }

    private fun bindField(
        seekId: Int, editId: Int, labelId: Int, labelRes: Int,
        key: String, default: Float,
        sliderMin: Float, sliderMax: Float, hardMin: Float, hardMax: Float,
    ) {
        val seek = findViewById<SeekBar>(seekId)
        val edit = findViewById<EditText>(editId)
        findViewById<TextView>(labelId).text = getString(labelRes)

        val steps = 10
        seek.max = ((sliderMax - sliderMin) * steps).roundToInt()
        var updating = false

        fun save(v: Float) = prefs.edit { putFloat(key, v) }
        fun toProgress(v: Float) =
            ((v.coerceIn(sliderMin, sliderMax) - sliderMin) * steps).roundToInt()

        val initial = prefs.getFloat(key, default).coerceIn(hardMin, hardMax)
        seek.progress = toProgress(initial)
        edit.setText(fmt(initial))

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val v = (sliderMin + progress / steps.toFloat())
                updating = true
                edit.setText(fmt(v))
                updating = false
                save(v)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                val v = parse(s?.toString()) ?: return
                if (v !in hardMin..hardMax) return
                updating = true
                seek.progress = toProgress(v)
                updating = false
                save(v)
            }
        })

        fun commit() {
            val v = (parse(edit.text.toString()) ?: prefs.getFloat(key, default))
                .coerceIn(hardMin, hardMax)
            updating = true
            edit.setText(fmt(v))
            seek.progress = toProgress(v)
            updating = false
            save(v)
        }
        edit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                commit()
                edit.clearFocus()
            }
            false
        }
        edit.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commit() }
    }

    private fun parse(text: String?): Float? =
        text?.trim()?.replace(',', '.')?.toFloatOrNull()

    private fun fmt(v: Float): String {
        val r = (v * 100f).roundToInt() / 100f // up to 2 decimals
        return if (r % 1f == 0f) r.toInt().toString()
        else String.format(Locale.US, "%.2f", r).trimEnd('0')
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val mine = ComponentName(this, IslandAccessibilityService::class.java)
        return enabled.split(":").any { ComponentName.unflattenFromString(it) == mine }
    }
}
