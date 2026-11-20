package com.maidana.finanzasapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.maidana.finanzasapp.notifications.RetentionCampaign
import com.maidana.finanzasapp.notifications.createRetentionChannel
import com.maidana.finanzasapp.ui.theme.FinanzasAppTheme

class MainActivity : ComponentActivity() {
    private var pendingCampaign by mutableStateOf<RetentionCampaign?>(null)
    // This ComponentActivity uses Compose and does not depend on FragmentActivity.
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createRetentionChannel(this)
        pendingCampaign = RetentionCampaign.from(
            if (savedInstanceState == null) intent.getStringExtra(RetentionCampaign.EXTRA_CAMPAIGN)
            else savedInstanceState.getString(RetentionCampaign.EXTRA_CAMPAIGN)
        )
        enableEdgeToEdge()
        setContent {
            FinanzasAppTheme {
                FinanzasApp(
                    notificationDestination = pendingCampaign?.screen?.route,
                    onNotificationOpened = ::finishNotificationOpen,
                    onNotificationSettings = ::openNotificationSettings
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        RetentionCampaign.from(intent.getStringExtra(RetentionCampaign.EXTRA_CAMPAIGN))
            ?.let { pendingCampaign = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(RetentionCampaign.EXTRA_CAMPAIGN, pendingCampaign?.key)
        super.onSaveInstanceState(outState)
    }

    private fun finishNotificationOpen() {
        val campaign = pendingCampaign ?: return
        pendingCampaign = null
        intent.removeExtra(RetentionCampaign.EXTRA_CAMPAIGN)
        FirebaseAnalytics.getInstance(this).logEvent("tp6_push_open", Bundle().apply {
            putString("campaign_type", campaign.key)
        })
    }

    private fun openNotificationSettings() {
        val preferences = getPreferences(MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED &&
            (!preferences.getBoolean("notification_permission_requested", false) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS))
        ) {
            preferences.edit().putBoolean("notification_permission_requested", true).apply()
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            })
        }
    }
}
