package io.github.pastdaking.rmblr.quick

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.github.pastdaking.rmblr.MainActivity

/**
 * An invisible hop between a widget or the tile and the recording service.
 *
 * Android only lets an app start a microphone foreground service from the background if
 * something visible started it. A widget tap and a tile tap both end up here, this window
 * is on screen for a frame, starts the service and goes away. If the microphone is not
 * allowed yet it asks once, then carries on.
 */
class QuickDictateActivity : ComponentActivity() {

    private val askForMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else {
            Toast.makeText(this, "Allow the microphone to dictate", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) begin() else askForMic.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun begin() {
        val clean = intent.getBooleanExtra(QuickDictationService.EXTRA_CLEAN, false)
        val phase = QuickDictationState.snapshot.value.phase
        if (phase == QuickPhase.RECORDING) {
            // A second tap while recording is a stop.
            startService(QuickDictationService.stopIntent(this))
        } else {
            ContextCompat.startForegroundService(this, QuickDictationService.startIntent(this, clean))
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
