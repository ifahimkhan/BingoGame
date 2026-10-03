package com.fahim.bingonumbercaller.hosting

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.fahim.bingonumbercaller.client.R
import com.fahim.bingonumbercaller.network.EmbeddedGameServer

/**
 * Keeps the host's process in the foreground while the embedded game server runs, so Android
 * doesn't freeze or kill it (dropping every player's socket) when the host switches apps.
 * Holds a Wi-Fi lock and a partial wake lock for the same reason. It does not own the server;
 * [EmbeddedGameServer] starts and stops this service.
 */
class HostingService : Service() {

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            EmbeddedGameServer.stopActive()
            stopSelf()
            return START_NOT_STICKY
        }

        val address = intent?.getStringExtra(EXTRA_ADDRESS).orEmpty()
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(address),
                foregroundServiceType()
            )
        } catch (e: Exception) {
            // Hosting still works while the app is visible; it just won't survive backgrounding
            Log.w(TAG, "Could not enter foreground; hosting is not protected in background", e)
            stopSelf()
            return START_NOT_STICKY
        }
        acquireLocks()
        // Never restart on our own: after process death there is no server to keep alive
        return START_NOT_STICKY
    }

    /** Host swiped the app away: nobody can draw numbers any more, so end the game. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        EmbeddedGameServer.stopActive()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }

    private fun buildNotification(address: String): Notification {
        ensureChannel()

        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            PendingIntent.getActivity(this, REQUEST_OPEN, launch, PendingIntent.FLAG_IMMUTABLE)
        }
        val stopHosting = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, HostingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (address.isNotEmpty()) "Players can join at $address" else "Players can join on your Wi-Fi"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hosting_notification)
            .setContentTitle("Hosting Bingo Live")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp)
            .addAction(0, "Stop hosting", stopHosting)
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("Hosting a game")
            .setDescription("Shown while your phone runs a Bingo Live game for other players")
            .setShowBadge(false)
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun acquireLocks() {
        if (wifiLock?.isHeld != true) {
            val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifi?.createWifiLock(wifiLockMode(), LOCK_TAG)?.apply {
                setReferenceCounted(false)
                acquire()
            }
        }
        if (wakeLock?.isHeld != true) {
            val power = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG)?.apply {
                setReferenceCounted(false)
                // Safety net in case onDestroy never runs; a bingo session rarely lasts this long
                acquire(MAX_WAKE_LOCK_MILLIS)
            }
        }
    }

    // LOW_LATENCY only applies while the screen is on; HIGH_PERF keeps Wi-Fi awake with it off but is a
    // no-op from Android 14, where the Caller screen keeping the display on is what protects the game
    @Suppress("DEPRECATION")
    private fun wifiLockMode(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }

    private fun releaseLocks() {
        wifiLock?.takeIf { it.isHeld }?.release()
        wakeLock?.takeIf { it.isHeld }?.release()
        wifiLock = null
        wakeLock = null
    }

    companion object {
        private const val TAG = "HostingService"
        private const val LOCK_TAG = "BingoLive:hosting"
        private const val CHANNEL_ID = "hosting"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN = 1
        private const val REQUEST_STOP = 2
        private const val ACTION_STOP = "com.fahim.bingonumbercaller.action.STOP_HOSTING"
        private const val EXTRA_ADDRESS = "address"
        private const val MAX_WAKE_LOCK_MILLIS = 4 * 60 * 60 * 1000L

        /** Call from the visible UI only: Android 12+ forbids starting a foreground service from background. */
        fun start(context: Context, address: String) {
            val intent = Intent(context, HostingService::class.java).putExtra(EXTRA_ADDRESS, address)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start hosting service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HostingService::class.java))
        }
    }
}
