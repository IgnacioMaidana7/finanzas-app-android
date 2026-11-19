package com.maidana.finanzasapp

import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RetentionNotificationsTest {
    @Test
    fun createsChannelOnLaunchFromCampaign() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("tp6_campaign", "revision_metas")
        ActivityScenario.launch<MainActivity>(intent).use {
            val channel = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(context.getString(R.string.notification_channel_id))
            assertNotNull(channel)
        }
    }

    @Test
    fun registersInstallationForLiveCampaign() {
        val topic = InstrumentationRegistry.getArguments().getString("tp6TestTopic")
        org.junit.Assume.assumeTrue(topic?.startsWith("tp6-prueba-") == true)
        val messaging = FirebaseMessaging.getInstance()
        assertTrue(Tasks.await(messaging.token, 40, TimeUnit.SECONDS).isNotBlank())
        Tasks.await(messaging.subscribeToTopic(requireNotNull(topic)), 40, TimeUnit.SECONDS)
    }
}
