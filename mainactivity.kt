package com.example.helloworld

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.location.*
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ─── Config ───────────────────────────────────────────────────────────────────
const val WEB_CLIENT_ID = 
const val MAPBOX_TOKEN  = 
const val BACKEND       = 
const val MAX_FRIENDS   = 4

// ─── SharedPreferences keys ───────────────────────────────────────────────────
const val PREFS_NAME         = "LiveLocPrefs"
const val PREF_TRACK_ID      = "trackId"
const val PREF_UID           = "uid"
const val PREF_DISPLAY_NAME  = "displayName"
const val PREF_SAVED_FRIENDS = "savedFriends"
const val PREF_MAP_STYLE     = "mapStyle"
const val PREF_PROFILE_PIC   = "profilePic"

// ─── Foreground service constants ─────────────────────────────────────────────
const val CHANNEL_ID          = "liveloc_bg_channel"
const val NOTIF_ID            = 1001
const val ACTION_STOP_SERVICE = "com.example.helloworld.STOP_SERVICE"
const val CHAT_CHANNEL_ID     = "liveloc_chat_messages"

// ─── Timing ───────────────────────────────────────────────────────────────────
const val PUSH_INTERVAL = 800L
const val POLL_INTERVAL = 600L
private const val GPS_INTERVAL_MS  = 300L
private const val GPS_MIN_DELAY_MS = 150L

// ─── Dark Theme Palette ───────────────────────────────────────────────────────
val DarkBg           = Color(0xFF0A0F0A)
val DarkSurface      = Color(0xFF111811)
val DarkCard         = Color(0xFF161E16)
val DarkCardAlt      = Color(0xFF1C251C)
val DarkBorder       = Color(0xFF243024)
val DarkBorderLight  = Color(0xFF2E3E2E)

val NeonGreen        = Color(0xFF00FF88)
val EmeraldGreen     = Color(0xFF10B981)
val EmeraldDeep      = Color(0xFF059669)
val EmeraldMid       = Color(0xFF34D399)
val EmeraldPale      = Color(0xFF6EE7B7)
val EmeraldGlow      = Color(0xFF00FF88).copy(alpha = 0.15f)

val TextOnDark       = Color(0xFFE8F5E8)
val TextOnDarkMuted  = Color(0xFF6B7F6B)
val TextOnDarkDim    = Color(0xFF3D4F3D)

val GreenOnline      = Color(0xFF00FF88)
val RedRecord        = Color(0xFFFF4757)
val AmberWarning     = Color(0xFFFFC107)

val FriendColors     = listOf(Color(0xFF00FF88), Color(0xFF00C8FF), Color(0xFFFFD700), Color(0xFFFF6B9D))
val FriendColorHex   = listOf("#00FF88", "#00C8FF", "#FFD700", "#FF6B9D")
val FriendLabels     = listOf("Emerald", "Cyan", "Gold", "Pink")

val SkyPrimary       = EmeraldGreen
val SkyLight         = EmeraldMid
val SkyPale          = EmeraldPale
val SkyDeep          = EmeraldDeep
val SheetBg          = DarkBg
val SheetCard        = DarkCard
val SheetBorder      = DarkBorder
val TextPrimary      = TextOnDark
val TextSecondary    = Color(0xFF8FAF8F)
val TextMuted        = TextOnDarkMuted

// ─── Thresholds ───────────────────────────────────────────────────────────────
private const val ACC_GOOD           = 30f
private const val ACC_ACCEPTABLE     = 100f
private const val MIN_DIST           = 1.0
private const val FORCE_MS           = 2_000L
private const val MAX_SPEED_JUMP_MS  = 55.0
private const val ACCURACY_WINDOW    = 8

// ─── Map Style Enum ───────────────────────────────────────────────────────────
enum class MapTheme(val label: String, val icon: String, val mapboxStyle: String) {
    DARK("Dark", "🌙", Style.DARK),
    LIGHT("Street", "🗺️", Style.STANDARD)
}

// ─── HTTP helpers ─────────────────────────────────────────────────────────────
fun httpGet(path: String): JSONObject? = try {
    val conn = URL("$BACKEND$path").openConnection() as HttpURLConnection
    conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.requestMethod = "GET"
    if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().readText()) else null
} catch (_: Exception) { null }

private fun httpGetArray(path: String): JSONArray? = try {
    val conn = URL("$BACKEND$path").openConnection() as HttpURLConnection
    conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.requestMethod = "GET"
    if (conn.responseCode == 200) JSONArray(conn.inputStream.bufferedReader().readText()) else null
} catch (_: Exception) { null }

fun httpPost(path: String, body: JSONObject): JSONObject? = try {
    val conn = URL("$BACKEND$path").openConnection() as HttpURLConnection
    conn.connectTimeout = 8000; conn.readTimeout = 8000
    conn.requestMethod = "POST"; conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
    if (conn.responseCode in 200..299)
        JSONObject(conn.inputStream.bufferedReader().readText()) else null
} catch (_: Exception) { null }

fun httpPatch(path: String, body: JSONObject): JSONObject? = try {
    val conn = URL("$BACKEND$path").openConnection() as HttpURLConnection
    conn.connectTimeout = 8000; conn.readTimeout = 8000
    conn.requestMethod = "PATCH"; conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/json")
    OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
    if (conn.responseCode in 200..299)
        JSONObject(conn.inputStream.bufferedReader().readText()) else null
} catch (_: Exception) { null }

fun httpDelete(path: String): Boolean = try {
    val conn = URL("$BACKEND$path").openConnection() as HttpURLConnection
    conn.connectTimeout = 5000; conn.readTimeout = 5000; conn.requestMethod = "DELETE"
    conn.responseCode in 200..299
} catch (_: Exception) { false }

fun ensureChatNotificationChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(
        CHAT_CHANNEL_ID, "Chat Messages", NotificationManager.IMPORTANCE_DEFAULT
    ).apply { description = "Notifications for new chat messages" }
    manager.createNotificationChannel(channel)
}

fun showChatNotification(ctx: Context, friendName: String, message: String, notificationId: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    val notification = NotificationCompat.Builder(ctx, CHAT_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setContentTitle(friendName.ifBlank { "New message" })
        .setContentText(message.ifBlank { "Sent a new message" })
        .setStyle(NotificationCompat.BigTextStyle().bigText(message.ifBlank { "Sent a new message" }))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(ctx).notify(notificationId, notification)
}

fun accuracyLabel(acc: Float): Pair<String, Color> = when {
    acc < 20f  -> "Excellent" to GreenOnline
    acc < 50f  -> "Good"      to EmeraldGreen
    acc < 100f -> "Fair"      to AmberWarning
    else       -> "Poor"      to RedRecord
}

fun distM(a: Point, b: Point): Double {
    val r    = 6_371_000.0
    val dLat = Math.toRadians(b.latitude()  - a.latitude())
    val dLon = Math.toRadians(b.longitude() - a.longitude())
    val x    = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude())) * cos(Math.toRadians(b.latitude())) *
            sin(dLon / 2).pow(2)
    return r * 2 * asin(sqrt(x))
}

// ─── Kalman Filter ────────────────────────────────────────────────────────────
class KalmanLatLng {
    private val Q_METRES_PER_SECOND = 3.0
    private var latEstimate = 0.0; private var latVariance = Double.MAX_VALUE
    private var lngEstimate = 0.0; private var lngVariance = Double.MAX_VALUE
    private var lastTimestamp = 0L
    val isInitialised: Boolean get() = lastTimestamp != 0L
    fun process(lat: Double, lng: Double, accuracy: Float, timestampMs: Long): Pair<Double, Double> {
        val accMetres = accuracy.toDouble().coerceAtLeast(1.0)
        if (!isInitialised) {
            latEstimate = lat; latVariance = accMetres * accMetres
            lngEstimate = lng; lngVariance = accMetres * accMetres
            lastTimestamp = timestampMs; return lat to lng
        }
        val dtSeconds = ((timestampMs - lastTimestamp) / 1000.0).coerceIn(0.0, 10.0)
        lastTimestamp = timestampMs
        val qContrib = Q_METRES_PER_SECOND * Q_METRES_PER_SECOND * dtSeconds
        latVariance += qContrib; lngVariance += qContrib
        val R = accMetres * accMetres
        val kLat = latVariance / (latVariance + R)
        latEstimate = latEstimate + kLat * (lat - latEstimate); latVariance = (1.0 - kLat) * latVariance
        val kLng = lngVariance / (lngVariance + R)
        lngEstimate = lngEstimate + kLng * (lng - lngEstimate); lngVariance = (1.0 - kLng) * lngVariance
        return latEstimate to lngEstimate
    }
    fun reset() { latVariance = Double.MAX_VALUE; lngVariance = Double.MAX_VALUE; lastTimestamp = 0L }
}

class AccuracyTracker(private val windowSize: Int = ACCURACY_WINDOW) {
    private val buffer = ArrayDeque<Float>(windowSize)
    fun push(acc: Float) { if (buffer.size >= windowSize) buffer.removeFirst(); buffer.addLast(acc) }
    val smoothed: Float get() {
        if (buffer.isEmpty()) return 999f
        var wSum = 0.0; var totalW = 0.0
        buffer.forEachIndexed { i, a -> val w = (i + 1).toDouble() / (a * a).toDouble(); wSum += w * a; totalW += w }
        return if (totalW == 0.0) buffer.last() else (wSum / totalW).toFloat()
    }
    val best: Float get() = buffer.minOrNull() ?: 999f
    fun reset() = buffer.clear()
}

class TeleportGuard {
    private var lastPoint: Point? = null; private var lastMs = 0L
    fun isValid(pt: Point, nowMs: Long): Boolean {
        val prev = lastPoint
        val ok = if (prev == null || lastMs == 0L) true
        else { val dtSec = (nowMs - lastMs) / 1000.0; val d = distM(prev, pt); val s = if (dtSec > 0) d / dtSec else 0.0; s < MAX_SPEED_JUMP_MS }
        if (ok) { lastPoint = pt; lastMs = nowMs }; return ok
    }
    fun reset() { lastPoint = null; lastMs = 0L }
}

object GpsFilter {
    private val kalman          = KalmanLatLng()
    private val accuracyTracker = AccuracyTracker()
    private val teleportGuard   = TeleportGuard()
    fun process(lat: Double, lon: Double, accuracy: Float, timestampMs: Long = System.currentTimeMillis()): Pair<Double, Double>? {
        val pt = Point.fromLngLat(lon, lat)
        if (!teleportGuard.isValid(pt, timestampMs)) return null
        accuracyTracker.push(accuracy)
        return kalman.process(lat, lon, accuracy, timestampMs)
    }
    val smoothedAccuracy: Float get() = accuracyTracker.smoothed
    val bestAccuracy:     Float get() = accuracyTracker.best
    fun reset() { kalman.reset(); accuracyTracker.reset(); teleportGuard.reset() }
}

// ─── Data models ──────────────────────────────────────────────────────────────
data class LocationData(
    val point: Point, val accuracy: Float,
    val speedKmh: Float = 0f, val address: String = "Fetching address…"
)

data class FriendLocation(
    val point: Point, val accuracy: Float,
    val speedKmh: Float, val isRecent: Boolean
)

data class SavedFriend(
    val trackId: String, val displayName: String, val email: String,
    val avatarBase64: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("trackId", trackId)
        put("displayName", displayName)
        put("email", email)
        put("avatarBase64", avatarBase64)
    }
    companion object {
        fun fromJson(obj: JSONObject) = SavedFriend(
            trackId      = obj.getString("trackId"),
            displayName  = obj.optString("displayName", ""),
            email        = obj.optString("email", ""),
            avatarBase64 = obj.optString("avatarBase64", "")
        )
    }
}

// ─── Image helpers ────────────────────────────────────────────────────────────
fun uriToBase64(ctx: Context, uri: Uri, maxPx: Int = 256): String? = try {
    val stream = ctx.contentResolver.openInputStream(uri) ?: return null
    val orig   = BitmapFactory.decodeStream(stream)
    stream.close()
    val side   = minOf(orig.width, orig.height)
    val x      = (orig.width  - side) / 2
    val y      = (orig.height - side) / 2
    val cropped = Bitmap.createBitmap(orig, x, y, side, side)
    val scaled  = Bitmap.createScaledBitmap(cropped, maxPx, maxPx, true)
    val out     = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
    Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
} catch (_: Exception) { null }

fun base64ToBitmap(b64: String?): Bitmap? {
    if (b64.isNullOrBlank()) return null
    return try {
        val bytes = Base64.decode(b64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) { null }
}

fun makeCircularAvatarBitmap(src: Bitmap, ringColorHex: String, sizePx: Int = 120): Bitmap {
    val result = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.parseColor(ringColorHex)
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
    val inner = sizePx / 2f - 5f
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    val scaled = Bitmap.createScaledBitmap(src, sizePx - 10, sizePx - 10, true)
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val tempBmp = Bitmap.createBitmap(sizePx - 10, sizePx - 10, Bitmap.Config.ARGB_8888)
    val tempCanvas = Canvas(tempBmp)
    val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    clipPaint.color = android.graphics.Color.BLACK
    tempCanvas.drawCircle(inner, inner, inner, clipPaint)
    clipPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    tempCanvas.drawBitmap(scaled, 0f, 0f, clipPaint)
    canvas.drawBitmap(tempBmp, 5f, 5f, null)
    return result
}

enum class NavTab { MAP, FRIENDS, CHAT, PROFILE }

// ─── Activity ─────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.mapbox.common.MapboxOptions.accessToken = MAPBOX_TOKEN
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent { AppRoot(auth, fusedLocationClient) { locationCallback = it } }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

// ─── Root ─────────────────────────────────────────────────────────────────────
@Composable
fun AppRoot(auth: FirebaseAuth, fused: FusedLocationProviderClient, onCb: (LocationCallback) -> Unit) {
    var user by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
    MaterialTheme(colorScheme = darkColorScheme(
        primary         = EmeraldGreen,
        secondary       = NeonGreen,
        background      = DarkBg,
        surface         = DarkCard,
        onSurface       = TextOnDark,
        onPrimary       = DarkBg,
        surfaceVariant  = DarkCardAlt,
        outline         = DarkBorder
    )) {
        if (user == null) AuthScreen(auth) { user = it }
        else MainApp(fused, onCb, user!!) { auth.signOut(); user = null }
    }
}

// ─── Auth Screen ──────────────────────────────────────────────────────────────
@Composable
fun AuthScreen(auth: FirebaseAuth, onSignedIn: (FirebaseUser) -> Unit) {
    val ctx     = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val acc = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            loading = true
            auth.signInWithCredential(GoogleAuthProvider.getCredential(acc.idToken, null))
                .addOnSuccessListener { r -> loading = false; r.user?.let { onSignedIn(it) } }
                .addOnFailureListener { e -> loading = false; Toast.makeText(ctx, "Auth failed: ${e.message}", Toast.LENGTH_SHORT).show() }
        } catch (e: ApiException) { loading = false; Toast.makeText(ctx, "Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
        Box(Modifier.size(320.dp).align(Alignment.Center).offset(y = (-60).dp)
            .background(Brush.radialGradient(listOf(EmeraldGreen.copy(alpha = 0.08f), Color.Transparent)), CircleShape))
        Column(Modifier.padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(EmeraldDeep, NeonGreen))), Alignment.Center) {
                Text("📍", fontSize = 48.sp)
            }
            Spacer(Modifier.height(32.dp))
            Text("LiveLoc", fontSize = 42.sp, fontWeight = FontWeight.Black, color = TextOnDark, letterSpacing = (-1).sp)
            Spacer(Modifier.height(8.dp))
            Text("Real-time location sharing", fontSize = 15.sp, color = TextOnDarkMuted, letterSpacing = 0.3.sp)
            Spacer(Modifier.height(60.dp))
            if (loading) {
                CircularProgressIndicator(color = EmeraldGreen, strokeWidth = 2.dp)
            } else {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(DarkCard).border(1.dp, DarkBorderLight, RoundedCornerShape(16.dp))
                    .clickable {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(WEB_CLIENT_ID).requestEmail().build()
                        launcher.launch(GoogleSignIn.getClient(ctx, gso).signInIntent)
                    }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("G", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF4285F4))
                        Spacer(Modifier.width(14.dp))
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextOnDark)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen))), contentAlignment = Alignment.Center) {
                    Text("Get Started", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = DarkBg)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("End-to-end encrypted · MongoDB secured", fontSize = 11.sp, color = TextOnDarkDim, textAlign = TextAlign.Center)
        }
    }
}

// ─── Main App ─────────────────────────────────────────────────────────────────
@Composable
fun MainApp(
    fused: FusedLocationProviderClient,
    onCb: (LocationCallback) -> Unit,
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var myLocation   by remember { mutableStateOf<LocationData?>(null) }
    var rawPoint     by remember { mutableStateOf<Point?>(null) }
    var rawAccuracy  by remember { mutableStateOf(0f) }
    var rawSpeedMs   by remember { mutableStateOf(0f) }
    var gpsAvailable by remember { mutableStateOf(true) }

    var myTrackId    by remember { mutableStateOf(prefs.getString(PREF_TRACK_ID, null) ?: "Loading…") }
    var trackIdReady by remember { mutableStateOf(prefs.contains(PREF_TRACK_ID)) }
    var isConnected  by remember { mutableStateOf(false) }
    var bgTrackingOn by remember { mutableStateOf(false) }

    var pathPoints    by remember { mutableStateOf<List<Point>>(emptyList()) }
    var isRecording   by remember { mutableStateOf(false) }
    var sessionId     by remember { mutableStateOf<String?>(null) }
    var lastRecPoint  by remember { mutableStateOf<Point?>(null) }
    var lastRecTimeMs by remember { mutableStateOf(0L) }

    val trackedIds      = remember { mutableStateListOf<String>() }
    val friendLocations = remember { mutableStateMapOf<String, FriendLocation?>() }

    var savedFriends by remember { mutableStateOf<List<SavedFriend>>(emptyList()) }

    // ── FIX 1: Use a version counter to force avatar recomposition ─────────────
    // This ensures the ProfileTab avatar re-renders immediately after upload.
    var myAvatarBase64 by remember {
        mutableStateOf(prefs.getString(PREF_PROFILE_PIC, "") ?: "")
    }
    var myAvatarVersion by remember { mutableStateOf(0) }  // bump after each upload

    val friendAvatars = remember { mutableStateMapOf<String, String>() }

    var showFriendDialog by remember { mutableStateOf(false) }
    var showChat         by remember { mutableStateOf(false) }
    var chatWithTrackId  by remember { mutableStateOf<String?>(null) }
    var chatWithName     by remember { mutableStateOf<String?>(null) }
    var viewingProfile   by remember { mutableStateOf<SavedFriend?>(null) }

    var mapTheme by remember {
        mutableStateOf(
            if (prefs.getString(PREF_MAP_STYLE, MapTheme.DARK.name) == MapTheme.LIGHT.name)
                MapTheme.LIGHT else MapTheme.DARK
        )
    }
    var activeTab by remember { mutableStateOf(NavTab.MAP) }

    var hasPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    var hasBgPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    var hasNotifPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val permLauncher      = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPerm = it }
    val bgPermLauncher    = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasBgPerm = granted
        if (granted) { ContextCompat.startForegroundService(ctx, Intent(ctx, LocationForegroundService::class.java)); bgTrackingOn = true }
        else Toast.makeText(ctx, "Select 'Allow all the time' for background tracking", Toast.LENGTH_LONG).show()
    }
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasNotifPerm = granted }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val b64 = uriToBase64(ctx, uri, 256) ?: return@launch
            withContext(Dispatchers.Main) {
                myAvatarBase64 = b64
                myAvatarVersion++  // FIX 1: Force recomposition of all avatar composables
            }
            prefs.edit().putString(PREF_PROFILE_PIC, b64).apply()
            // FIX 1: Upload to server so others see updated avatar
            val result = httpPost("/api/user/${user.uid}/avatar", JSONObject().apply { put("avatarBase64", b64) })
            if (result == null) {
                // Retry once on failure
                kotlinx.coroutines.delay(2000)
                httpPost("/api/user/${user.uid}/avatar", JSONObject().apply { put("avatarBase64", b64) })
            }
        }
    }

    LaunchedEffect(Unit) {
        ensureChatNotificationChannel(ctx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm)
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        val json = prefs.getString(PREF_SAVED_FRIENDS, null)
        if (json != null) {
            try {
                val arr  = JSONArray(json)
                val list = mutableListOf<SavedFriend>()
                for (i in 0 until arr.length()) list.add(SavedFriend.fromJson(arr.getJSONObject(i)))
                savedFriends = list
            } catch (_: Exception) {}
        }
    }

    fun persistSavedFriends(list: List<SavedFriend>) {
        val arr = JSONArray(); list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(PREF_SAVED_FRIENDS, arr.toString()).apply()
        scope.launch(Dispatchers.IO) {
            val backendArr = JSONArray()
            list.forEach { sf -> backendArr.put(JSONObject().apply {
                put("trackId", sf.trackId); put("displayName", sf.displayName)
                put("email", sf.email); put("avatarBase64", sf.avatarBase64)
            })}
            httpPost("/api/user/${user.uid}/saved-friends", JSONObject().apply { put("savedFriends", backendArr) })
        }
    }

    fun addSavedFriend(sf: SavedFriend) {
        if (savedFriends.any { it.trackId == sf.trackId }) return
        val updated = savedFriends + sf; savedFriends = updated; persistSavedFriends(updated)
    }

    fun removeSavedFriend(trackId: String) {
        val updated = savedFriends.filter { it.trackId != trackId }
        savedFriends = updated; persistSavedFriends(updated)
        if (trackId in trackedIds) { trackedIds.remove(trackId); friendLocations.remove(trackId) }
    }

    LaunchedEffect(user.uid) {
        withContext(Dispatchers.IO) {
            isConnected = httpGet("/api/health")?.optString("status") == "OK"
            val userDoc = httpGet("/api/user/${user.uid}")
            if (userDoc != null && userDoc.has("trackId")) {
                val remoteTrackId = userDoc.getString("trackId")
                myTrackId = remoteTrackId; trackIdReady = true
                prefs.edit().putString(PREF_TRACK_ID, remoteTrackId).putString(PREF_UID, user.uid).putString(PREF_DISPLAY_NAME, user.displayName ?: "").apply()

                val remoteAvatar = userDoc.optString("avatarBase64", "")
                // FIX 1: Always prefer local avatar if it was set; sync from remote only if local is blank
                if (remoteAvatar.isNotBlank() && myAvatarBase64.isBlank()) {
                    withContext(Dispatchers.Main) {
                        myAvatarBase64 = remoteAvatar
                        myAvatarVersion++
                    }
                    prefs.edit().putString(PREF_PROFILE_PIC, remoteAvatar).apply()
                }

                val remoteFriends = userDoc.optJSONArray("friends")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
                val remoteSaved   = userDoc.optJSONArray("savedFriends")
                if (remoteSaved != null && remoteSaved.length() > 0) {
                    val list = mutableListOf<SavedFriend>()
                    for (i in 0 until remoteSaved.length()) list.add(SavedFriend.fromJson(remoteSaved.getJSONObject(i)))
                    withContext(Dispatchers.Main) {
                        savedFriends = list
                        prefs.edit().putString(PREF_SAVED_FRIENDS, JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()).apply()
                    }
                }
                withContext(Dispatchers.Main) {
                    remoteFriends.forEach { fid -> if (fid !in trackedIds) { trackedIds.add(fid); friendLocations[fid] = null } }
                    trackedIds.filter { it !in remoteFriends }.forEach { fid -> trackedIds.remove(fid); friendLocations.remove(fid) }
                }
                httpPost("/api/user/upsert", JSONObject().apply {
                    put("uid", user.uid); put("trackId", remoteTrackId)
                    put("displayName", user.displayName ?: ""); put("email", user.email ?: "")
                })
            } else {
                val localCached = prefs.getString(PREF_TRACK_ID, null)
                val trackId = localCached ?: (httpPost("/api/track/generate", JSONObject())?.optString("trackId") ?: "TRK-${UUID.randomUUID().toString().take(8).uppercase()}")
                httpPost("/api/user/upsert", JSONObject().apply {
                    put("uid", user.uid); put("trackId", trackId)
                    put("displayName", user.displayName ?: ""); put("email", user.email ?: "")
                })
                withContext(Dispatchers.Main) { myTrackId = trackId; trackIdReady = true }
                prefs.edit().putString(PREF_TRACK_ID, trackId).putString(PREF_UID, user.uid).putString(PREF_DISPLAY_NAME, user.displayName ?: "").apply()
            }
        }
    }

    // ── FIX 2: Poll who is tracking me and auto-add them to savedFriends ──────
    // Every 15 seconds, check if any new users have added my trackId to their friends list.
    // The backend doesn't have a dedicated "who tracks me" endpoint, so we use a workaround:
    // We check conversations — if someone I haven't saved messaged me, we fetch their profile.
    // Also periodically re-fetch our own user doc to pick up remotely added friends.
    LaunchedEffect(trackIdReady, myTrackId) {
        if (!trackIdReady) return@LaunchedEffect
        while (true) {
            withContext(Dispatchers.IO) {
                // Re-fetch own user doc to pick up any new remote friends
                val userDoc = httpGet("/api/user/${user.uid}") ?: return@withContext
                val remoteFriends = userDoc.optJSONArray("friends")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
                withContext(Dispatchers.Main) {
                    remoteFriends.forEach { fid ->
                        if (fid !in trackedIds) {
                            trackedIds.add(fid)
                            friendLocations[fid] = null
                        }
                    }
                }

                // Also fetch profiles for any tracked IDs we don't have saved yet
                val idsNeedingProfile = trackedIds.toList().filter { id ->
                    val existing = savedFriends.find { it.trackId == id }
                    existing == null || existing.displayName.isBlank()
                }
                idsNeedingProfile.forEach { id ->
                    val j = httpGet("/api/user/by-trackid/$id") ?: return@forEach
                    if (!j.has("trackId")) return@forEach
                    val sf = SavedFriend(
                        trackId      = j.getString("trackId"),
                        displayName  = j.optString("displayName", "").ifBlank { id },
                        email        = j.optString("email", ""),
                        avatarBase64 = j.optString("avatarBase64", "")
                    )
                    withContext(Dispatchers.Main) {
                        val merged = savedFriends.toMutableList()
                        val idx = merged.indexOfFirst { it.trackId == sf.trackId }
                        if (idx < 0) merged.add(sf) else merged[idx] = sf
                        savedFriends = merged
                        if (sf.avatarBase64.isNotBlank()) friendAvatars[sf.trackId] = sf.avatarBase64
                        persistSavedFriends(merged)
                    }
                }

                // FIX 2: Refresh friend avatars from server to catch their profile pic updates
                trackedIds.toList().forEach { id ->
                    val j = httpGet("/api/user/by-trackid/$id") ?: return@forEach
                    val remoteAvatar = j.optString("avatarBase64", "")
                    if (remoteAvatar.isNotBlank()) {
                        val currentAvatar = friendAvatars[id] ?: ""
                        if (remoteAvatar != currentAvatar) {
                            withContext(Dispatchers.Main) {
                                friendAvatars[id] = remoteAvatar
                                // Update in savedFriends too
                                val merged = savedFriends.toMutableList()
                                val idx = merged.indexOfFirst { it.trackId == id }
                                if (idx >= 0) merged[idx] = merged[idx].copy(avatarBase64 = remoteAvatar)
                                savedFriends = merged
                                persistSavedFriends(merged)
                            }
                        }
                    }
                }
            }
            delay(15_000L)
        }
    }

    val trackedSnapshot = trackedIds.toList()

    LaunchedEffect(trackedSnapshot) {
        trackedSnapshot.forEach { id ->
            val existingAvatar = savedFriends.find { it.trackId == id }?.avatarBase64 ?: ""
            if (existingAvatar.isNotBlank()) { friendAvatars[id] = existingAvatar; return@forEach }
            withContext(Dispatchers.IO) {
                val j = httpGet("/api/user/by-trackid/$id") ?: return@withContext
                val b64 = j.optString("avatarBase64", "")
                if (b64.isNotBlank()) withContext(Dispatchers.Main) { friendAvatars[id] = b64 }
            }
        }
    }

    // Own location push loop
    LaunchedEffect(trackIdReady, bgTrackingOn) {
        if (!trackIdReady) return@LaunchedEffect
        while (true) {
            if (!bgTrackingOn) {
                val pt = rawPoint
                if (pt != null) {
                    withContext(Dispatchers.IO) {
                        val ok = httpPost("/api/location/update", JSONObject().apply {
                            put("trackId", myTrackId); put("lat", pt.latitude()); put("lng", pt.longitude())
                            put("accuracy", rawAccuracy.toDouble()); put("speed", rawSpeedMs.toDouble())
                        }) != null
                        withContext(Dispatchers.Main) { isConnected = ok }
                    }
                }
            }
            delay(PUSH_INTERVAL)
        }
    }

    val lastNotifiedChatTs = remember { mutableStateMapOf<String, Long>() }
    var chatNotifBootstrapped by remember { mutableStateOf(false) }
    LaunchedEffect(trackIdReady, myTrackId, hasNotifPerm) {
        if (!trackIdReady) return@LaunchedEffect
        while (true) {
            val conversations = withContext(Dispatchers.IO) {
                httpGetArray("/api/chat/conversations/$myTrackId")?.let { parseConversations(it) } ?: emptyList()
            }
            if (!chatNotifBootstrapped) {
                conversations.forEach { c -> lastNotifiedChatTs[c.conversationId] = c.lastTimestamp }
                chatNotifBootstrapped = true
            } else {
                conversations.forEach { c ->
                    val previousTs = lastNotifiedChatTs[c.conversationId] ?: 0L
                    if (c.lastTimestamp > previousTs && (c.unread[myTrackId] ?: 0) > 0) {
                        val friendId   = c.participants.firstOrNull { it != myTrackId } ?: "Friend"
                        val friendName = c.names[friendId].orEmpty().ifBlank { friendId }
                        showChatNotification(ctx, friendName, c.lastMessage, c.conversationId.hashCode())
                    }
                    lastNotifiedChatTs[c.conversationId] = c.lastTimestamp
                }
            }
            delay(2_500L)
        }
    }

    @SuppressLint("MissingPermission")
    fun bootstrapLastLocation() {
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && rawPoint == null) {
                val acc = loc.accuracy.coerceAtLeast(1f)
                val spd = if (loc.hasSpeed() && loc.speed >= 0) loc.speed else 0f
                val smoothed = GpsFilter.process(loc.latitude, loc.longitude, acc, System.currentTimeMillis()) ?: return@addOnSuccessListener
                val smoothPt = Point.fromLngLat(smoothed.second, smoothed.first)
                rawPoint = smoothPt; rawAccuracy = GpsFilter.smoothedAccuracy; rawSpeedMs = spd
                myLocation = LocationData(smoothPt, GpsFilter.smoothedAccuracy, spd * 3.6f)
                scope.launch { val addr = withContext(Dispatchers.IO) { geocode(ctx, smoothPt.latitude(), smoothPt.longitude()) }; myLocation = myLocation?.copy(address = addr) }
            }
        }
    }

    LaunchedEffect(hasPerm) {
        if (hasPerm) {
            bootstrapLastLocation()
            startGps(fused = fused, onCb = onCb,
                onAvailability = { available -> if (!available) GpsFilter.reset(); gpsAvailable = available },
                onLoc = { pt, smoothAcc, spdMs ->
                    rawPoint = pt; rawAccuracy = smoothAcc; rawSpeedMs = spdMs
                    myLocation = myLocation?.copy(point = pt, accuracy = smoothAcc, speedKmh = spdMs * 3.6f)
                        ?: LocationData(pt, smoothAcc, spdMs * 3.6f)
                    scope.launch { val addr = withContext(Dispatchers.IO) { geocode(ctx, pt.latitude(), pt.longitude()) }; myLocation = myLocation?.copy(address = addr) }
                    if (isRecording && sessionId != null && smoothAcc < ACC_ACCEPTABLE) {
                        val now  = System.currentTimeMillis(); val good = smoothAcc < ACC_GOOD
                        val moved  = lastRecPoint == null || distM(lastRecPoint!!, pt) > MIN_DIST
                        val forced = (now - lastRecTimeMs) > FORCE_MS
                        if (good || moved || forced) {
                            lastRecTimeMs = now; lastRecPoint = pt; pathPoints = pathPoints + pt
                            val sid = sessionId!!
                            scope.launch(Dispatchers.IO) { httpPost("/api/session/$sid/point", JSONObject().apply { put("lat", pt.latitude()); put("lng", pt.longitude()) }) }
                        }
                    }
                }
            )
        } else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(trackedSnapshot, showFriendDialog) {
        if (trackedSnapshot.isEmpty() || showFriendDialog) return@LaunchedEffect
        while (true) {
            coroutineScope {
                trackedSnapshot.forEach { id ->
                    launch(Dispatchers.IO) {
                        val j = httpGet("/api/location/$id") ?: return@launch
                        val lat = j.optDouble("lat", Double.NaN)
                        val lng = j.optDouble("lng", Double.NaN)
                        if (!lat.isNaN() && !lng.isNaN()) {
                            val newLoc = FriendLocation(
                                Point.fromLngLat(lng, lat),
                                j.optDouble("accuracy", 0.0).toFloat(),
                                (j.optDouble("speed", 0.0) * 3.6).toFloat(),
                                j.optBoolean("isRecent", false)
                            )
                            withContext(Dispatchers.Main) { friendLocations[id] = newLoc }
                        }
                    }
                }
            }
            delay(POLL_INTERVAL)
        }
    }

    LaunchedEffect(trackedSnapshot, user.uid) {
        val idsNeedingProfile = trackedSnapshot.filter { id ->
            val existing = savedFriends.find { it.trackId == id }
            existing == null || existing.displayName.isBlank() || existing.email.isBlank()
        }
        if (idsNeedingProfile.isEmpty()) return@LaunchedEffect
        val fetched = withContext(Dispatchers.IO) {
            idsNeedingProfile.mapNotNull { id ->
                val j = httpGet("/api/user/by-trackid/$id") ?: return@mapNotNull null
                if (!j.has("trackId")) return@mapNotNull null
                SavedFriend(
                    trackId      = j.getString("trackId"),
                    displayName  = j.optString("displayName", "").ifBlank { j.getString("trackId") },
                    email        = j.optString("email", ""),
                    avatarBase64 = j.optString("avatarBase64", "")
                )
            }
        }
        if (fetched.isEmpty()) return@LaunchedEffect
        val merged = savedFriends.toMutableList()
        fetched.forEach { incoming ->
            val idx = merged.indexOfFirst { it.trackId == incoming.trackId }
            if (idx < 0) merged.add(incoming)
            else {
                val current = merged[idx]
                merged[idx] = current.copy(
                    displayName  = if (incoming.displayName.isNotBlank()) incoming.displayName else current.displayName,
                    email        = if (incoming.email.isNotBlank()) incoming.email else current.email,
                    avatarBase64 = incoming.avatarBase64.ifBlank { current.avatarBase64 }
                )
            }
            if (incoming.avatarBase64.isNotBlank()) friendAvatars[incoming.trackId] = incoming.avatarBase64
        }
        if (merged != savedFriends) { savedFriends = merged; persistSavedFriends(merged) }
    }

    fun persistFriends(newList: List<String>) {
        scope.launch(Dispatchers.IO) {
            httpPost("/api/user/${user.uid}/friends", JSONObject().apply { put("friends", JSONArray().apply { newList.forEach { put(it) } }) })
        }
    }

    fun addFriend(id: String, profile: SavedFriend? = null) {
        if (trackedIds.size >= MAX_FRIENDS || id in trackedIds) return
        trackedIds.add(id); friendLocations[id] = null; persistFriends(trackedIds.toList())
        if (profile != null) addSavedFriend(profile)
    }

    fun fetchFriendProfile(trackId: String, fallback: SavedFriend? = null): SavedFriend? {
        val normalized = trackId.trim().uppercase()
        val j = httpGet("/api/user/by-trackid/$normalized")
        if (j != null && j.has("trackId")) {
            return SavedFriend(
                trackId      = j.getString("trackId"),
                displayName  = j.optString("displayName", "").ifBlank { j.getString("trackId") },
                email        = j.optString("email", ""),
                avatarBase64 = j.optString("avatarBase64", "")
            )
        }
        return fallback?.copy(trackId = normalized, displayName = fallback.displayName.ifBlank { normalized })
    }

    fun removeFriend(id: String) {
        trackedIds.remove(id); friendLocations.remove(id); persistFriends(trackedIds.toList())
    }

    fun toggleTrackSavedFriend(trackId: String) {
        if (trackId in trackedIds) removeFriend(trackId) else addFriend(trackId)
    }

    val onStartRecording: () -> Unit = {
        val sid = UUID.randomUUID().toString()
        sessionId = sid; pathPoints = emptyList(); lastRecPoint = null; lastRecTimeMs = 0L
        GpsFilter.reset(); isRecording = true
        val startTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        scope.launch(Dispatchers.IO) { httpPost("/api/session/start", JSONObject().apply { put("sessionId", sid); put("uid", user.uid); put("trackId", myTrackId); put("startTime", startTime) }) }
    }

    val onStopRecording: () -> Unit = {
        isRecording = false
        val sid     = sessionId
        val endTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        if (sid != null) scope.launch(Dispatchers.IO) { httpPatch("/api/session/$sid/end", JSONObject().apply { put("endTime", endTime) }) }
        sessionId = null; lastRecPoint = null
    }

    fun startBgTracking() {
        if (!hasPerm) { Toast.makeText(ctx, "Grant location permission first", Toast.LENGTH_SHORT).show(); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBgPerm) bgPermLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else { ContextCompat.startForegroundService(ctx, Intent(ctx, LocationForegroundService::class.java)); bgTrackingOn = true }
    }
    fun stopBgTracking() { ctx.stopService(Intent(ctx, LocationForegroundService::class.java)); bgTrackingOn = false }

    if (!hasPerm) { PermScreen { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }; return }

    if (showChat) {
        ChatEntryPoint(
            myTrackId          = myTrackId,
            myName             = user.displayName ?: myTrackId,
            myAvatarBase64     = myAvatarBase64,
            friendAvatars      = friendAvatars,
            trackedIds         = trackedIds.toList(),
            initialChatTrackId = chatWithTrackId,
            initialChatName    = chatWithName,
            onBack             = { showChat = false; chatWithTrackId = null; chatWithName = null }
        )
        return
    }

    if (viewingProfile != null) {
        val vp = viewingProfile!!
        UserProfileScreen(
            profile = vp, isTracking = vp.trackId in trackedIds,
            liveLocation = friendLocations[vp.trackId],
            isAlreadySaved = savedFriends.any { it.trackId == vp.trackId },
            canTrack = vp.trackId in trackedIds || trackedIds.size < MAX_FRIENDS,
            onBack = { viewingProfile = null },
            onToggleTrack = { toggleTrackSavedFriend(it) },
            onAddFriend = { sf -> addSavedFriend(sf) },
            onRemoveFriend = { removeSavedFriend(it) },
            onOpenChat = { sf -> chatWithTrackId = sf.trackId; chatWithName = sf.displayName.ifBlank { sf.trackId }; viewingProfile = null; showChat = true }
        )
        return
    }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        Box(Modifier.fillMaxSize().padding(bottom = 80.dp)) {
            when (activeTab) {
                NavTab.MAP -> MapScreen(
                    myLocation = myLocation, myTrackId = myTrackId, trackIdReady = trackIdReady,
                    isConnected = isConnected, gpsAvailable = gpsAvailable,
                    pathPoints = pathPoints, isRecording = isRecording,
                    trackedIds = trackedIds, friendLocations = friendLocations,
                    myAvatarBase64 = myAvatarBase64,
                    myAvatarVersion = myAvatarVersion,  // FIX 1: pass version to force recompose
                    friendAvatars = friendAvatars,
                    savedFriends = savedFriends,         // FIX 4: pass savedFriends for map popups
                    onStartRecording = onStartRecording, onStopRecording = onStopRecording,
                    bgTrackingOn = bgTrackingOn,
                    onStartBgTracking = { startBgTracking() }, onStopBgTracking = { stopBgTracking() },
                    mapTheme = mapTheme,
                    onMapThemeChange = { theme -> mapTheme = theme; prefs.edit().putString(PREF_MAP_STYLE, theme.name).apply() }
                )
                NavTab.FRIENDS -> FriendsTab(
                    savedFriends = savedFriends, trackedIds = trackedIds.toList(),
                    friendLocations = friendLocations,
                    onShowFriendDialog = { showFriendDialog = true },
                    onToggleTrack = { toggleTrackSavedFriend(it) },
                    onRemoveSavedFriend = { removeSavedFriend(it) },
                    onViewProfile = { sf -> viewingProfile = sf },
                    onOpenChatWith = { sf -> chatWithTrackId = sf.trackId; chatWithName = sf.displayName.ifBlank { sf.trackId }; showChat = true }
                )
                NavTab.CHAT -> ChatEntryPoint(
                    myTrackId      = myTrackId,
                    myName         = user.displayName ?: myTrackId,
                    myAvatarBase64 = myAvatarBase64,
                    friendAvatars  = friendAvatars,
                    trackedIds     = trackedIds.toList(),
                    onBack         = { activeTab = NavTab.MAP }
                )
                NavTab.PROFILE -> ProfileTab(
                    user = user, myTrackId = myTrackId, trackIdReady = trackIdReady,
                    myLocation = myLocation, isConnected = isConnected, gpsAvailable = gpsAvailable,
                    bgTrackingOn = bgTrackingOn,
                    myAvatarBase64 = myAvatarBase64,
                    myAvatarVersion = myAvatarVersion,  // FIX 1
                    onPickProfilePicture = { imagePickerLauncher.launch("image/*") },
                    onStartBgTracking = { startBgTracking() }, onStopBgTracking = { stopBgTracking() },
                    onStartRecording = onStartRecording, onStopRecording = onStopRecording,
                    isRecording = isRecording, pathPoints = pathPoints,
                    onSignOut = onSignOut
                )
            }
        }
        DarkBottomNav(activeTab = activeTab, onTabSelected = { activeTab = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showFriendDialog) {
        FriendDialog(
            currentIds = trackedIds.toList(), savedFriends = savedFriends, myUid = user.uid,
            onDismiss = { showFriendDialog = false },
            onTrackNow = { sf ->
                scope.launch(Dispatchers.IO) {
                    val resolved = fetchFriendProfile(sf.trackId, sf)
                    withContext(Dispatchers.Main) {
                        if (resolved == null) { Toast.makeText(ctx, "Could not load this friend profile", Toast.LENGTH_SHORT).show(); return@withContext }
                        if (trackedIds.size >= MAX_FRIENDS) { Toast.makeText(ctx, "Maximum $MAX_FRIENDS friends reached", Toast.LENGTH_SHORT).show(); return@withContext }
                        addFriend(resolved.trackId.trim().uppercase(), resolved); showFriendDialog = false
                    }
                }
            },
            onAddAndTrack = { sf ->
                scope.launch(Dispatchers.IO) {
                    val resolved = fetchFriendProfile(sf.trackId, sf) ?: sf
                    withContext(Dispatchers.Main) { addSavedFriend(resolved); addFriend(resolved.trackId, resolved); showFriendDialog = false }
                }
            },
            onViewProfile = { sf -> showFriendDialog = false; viewingProfile = sf }
        )
    }
}

// ─── Dark Bottom Navigation Bar ───────────────────────────────────────────────
@Composable
fun DarkBottomNav(activeTab: NavTab, onTabSelected: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        Triple(NavTab.MAP, "Map", "📍"),
        Triple(NavTab.FRIENDS, "Friends", "👥"),
        Triple(NavTab.CHAT, "Chat", "💬"),
        Triple(NavTab.PROFILE, "Profile", "👤")
    )
    Box(modifier = modifier.fillMaxWidth().height(80.dp)
        .background(Brush.verticalGradient(listOf(Color.Transparent, DarkSurface)))) {
        Surface(Modifier.fillMaxWidth().height(72.dp).align(Alignment.BottomCenter),
            color = DarkSurface, shadowElevation = 0.dp, tonalElevation = 0.dp) {
            Box(Modifier.fillMaxSize().border(width = 1.dp,
                brush = Brush.horizontalGradient(listOf(Color.Transparent, DarkBorderLight, DarkBorderLight, Color.Transparent)),
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    items.forEach { (tab, label, emoji) ->
                        NavItem(emoji = emoji, label = label, isSelected = activeTab == tab, onClick = { onTabSelected(tab) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(emoji: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1f else 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "nav_scale")
    Column(modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))),
            contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isSelected) EmeraldGreen else TextOnDarkMuted, letterSpacing = 0.3.sp)
    }
}

// ─── Avatar composable — FIX 1: key on avatarVersion to force full recompose ──
@Composable
fun AvatarCircle(
    base64: String,
    fallbackInitial: String,
    size: Int = 56,
    isTracking: Boolean = false,
    isLive: Boolean = false,
    avatarVersion: Int = 0  // FIX 1: bump this to force Compose to re-decode the bitmap
) {
    // FIX 1: key on both base64 AND avatarVersion so remember() re-runs when version changes
    val bitmap = remember(base64, avatarVersion) { base64ToBitmap(base64) }
    Box(
        Modifier.size(size.dp).clip(CircleShape)
            .background(if (isTracking) Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen))
            else Brush.linearGradient(listOf(DarkCardAlt, DarkBorderLight)))
            .border(2.dp, if (isLive) GreenOnline.copy(alpha = 0.6f) else Color.Transparent, CircleShape),
        Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(fallbackInitial, fontSize = (size * 0.38f).sp, fontWeight = FontWeight.ExtraBold,
                color = if (isTracking) DarkBg else TextOnDark)
        }
    }
}

// ─── Map Screen — FIX 4: Friend popup card with photo always visible ──────────
@Composable
fun MapScreen(
    myLocation: LocationData?, myTrackId: String, trackIdReady: Boolean,
    isConnected: Boolean, gpsAvailable: Boolean,
    pathPoints: List<Point>, isRecording: Boolean,
    trackedIds: List<String>,
    friendLocations: Map<String, FriendLocation?>,
    myAvatarBase64: String = "",
    myAvatarVersion: Int = 0,
    friendAvatars: Map<String, String> = emptyMap(),
    savedFriends: List<SavedFriend> = emptyList(),  // FIX 4
    bgTrackingOn: Boolean,
    onStartRecording: () -> Unit, onStopRecording: () -> Unit,
    onStartBgTracking: () -> Unit, onStopBgTracking: () -> Unit,
    mapTheme: MapTheme = MapTheme.DARK,
    onMapThemeChange: (MapTheme) -> Unit = {}
) {
    val viewport  = rememberMapViewportState { setCameraOptions { zoom(4.0); center(Point.fromLngLat(78.9629, 20.5937)) } }
    val myCentred = remember { mutableStateOf(false) }
    var previousTrackedIds by remember { mutableStateOf(trackedIds) }
    var pendingFriendFocusId by remember { mutableStateOf<String?>(null) }

    val overlayBg     = if (mapTheme == MapTheme.DARK) DarkCard.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f)
    val overlayBorder = if (mapTheme == MapTheme.DARK) DarkBorderLight else Color(0xFFDDDDDD)
    val overlayText   = if (mapTheme == MapTheme.DARK) TextOnDark else Color(0xFF1A1A1A)
    val overlayMuted  = if (mapTheme == MapTheme.DARK) TextOnDarkMuted else Color(0xFF777777)

    LaunchedEffect(myLocation) {
        val d = myLocation ?: return@LaunchedEffect
        if (!myCentred.value) { viewport.flyTo(CameraOptions.Builder().center(d.point).zoom(16.0).build()); myCentred.value = true }
    }
    LaunchedEffect(trackedIds) {
        val added = trackedIds.filter { it !in previousTrackedIds }
        if (added.isNotEmpty()) pendingFriendFocusId = added.last()
        previousTrackedIds = trackedIds
    }
    LaunchedEffect(pendingFriendFocusId, friendLocations) {
        val id  = pendingFriendFocusId ?: return@LaunchedEffect
        val loc = friendLocations[id] ?: return@LaunchedEffect
        viewport.flyTo(CameraOptions.Builder().center(loc.point).zoom(16.5).build())
        pendingFriendFocusId = null
    }

    Box(Modifier.fillMaxSize()) {
        MapboxMap(modifier = Modifier.fillMaxSize(), mapViewportState = viewport,
            style = { MapStyle(style = mapTheme.mapboxStyle) }) {

            if (pathPoints.size >= 2)
                PolylineAnnotation(points = pathPoints, lineColorString = "#00FF88", lineWidth = 4.0, lineOpacity = 0.9)
            pathPoints.forEachIndexed { i, pt ->
                if (i % 5 == 0) CircleAnnotation(point = pt, circleRadius = 4.0, circleColorString = "#00FF88", circleStrokeWidth = 1.5, circleStrokeColorString = "#0A0F0A")
            }

            trackedIds.forEachIndexed { slot, id ->
                val loc = friendLocations[id] ?: return@forEachIndexed
                val hex = FriendColorHex[slot % FriendColorHex.size]
                CircleAnnotation(point = loc.point, circleRadius = 22.0, circleColorString = hex, circleOpacity = 0.18, circleStrokeWidth = 0.0)
                CircleAnnotation(point = loc.point, circleRadius = 11.0, circleColorString = hex,
                    circleStrokeWidth = 3.0, circleStrokeColorString = "#0A0F0A",
                    circleOpacity = if (loc.isRecent) 1.0 else 0.5)
            }

            myLocation?.let { d ->
                val col = if (isRecording) "#FF4757" else "#00FF88"
                CircleAnnotation(point = d.point, circleRadius = 26.0, circleColorString = col, circleOpacity = 0.15, circleStrokeWidth = 0.0)
                CircleAnnotation(point = d.point, circleRadius = 12.0, circleColorString = col, circleStrokeWidth = 3.0, circleStrokeColorString = "#0A0F0A")
            }
        }

        // ── Top bar ────────────────────────────────────────────────────────────
        Row(Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 16.dp, end = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(overlayBg)
                .border(1.dp, overlayBorder, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (isConnected) GreenOnline else TextOnDarkMuted))
                Text(if (isConnected) "Live" else "Offline", fontSize = 12.sp, color = if (isConnected) GreenOnline else overlayMuted, fontWeight = FontWeight.Bold)
                if (myLocation != null) {
                    Text("·", color = overlayMuted, fontSize = 12.sp)
                    val (_, color) = accuracyLabel(myLocation.accuracy)
                    Text("±${myLocation.accuracy.toInt()}m", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(overlayBg)
                .border(1.dp, overlayBorder, RoundedCornerShape(20.dp)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MapTheme.entries.forEach { theme ->
                    val isSelected = mapTheme == theme
                    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { onMapThemeChange(theme) }.padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(theme.icon, fontSize = 12.sp)
                            Text(theme.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal, color = if (isSelected) DarkBg else overlayMuted)
                        }
                    }
                }
            }
        }

        if (isRecording) {
            Row(Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
                .clip(RoundedCornerShape(20.dp)).background(RedRecord.copy(alpha = 0.15f))
                .border(1.dp, RedRecord.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(RedRecord))
                Text("Recording · ${pathPoints.size} pts", fontSize = 11.sp, color = RedRecord, fontWeight = FontWeight.Bold)
            }
        }

        // ── FIX 4: Friend popup cards — persistent, always visible ────────────
        // Rendered as a scrollable row at the top-right area so they don't obscure the map centre.
        // Each tracked friend with a known location gets a compact card.
        if (trackedIds.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 72.dp, start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trackedIds.forEachIndexed { slot, id ->
                    val loc = friendLocations[id]
                    val friendProfile = savedFriends.find { it.trackId == id }
                    val friendColor = FriendColors[slot % FriendColors.size]
                    val friendName = friendProfile?.displayName?.ifBlank { id } ?: id
                    val avatarB64 = friendAvatars[id] ?: friendProfile?.avatarBase64 ?: ""
                    FriendMapPopupCard(
                        trackId      = id,
                        name         = friendName,
                        avatarBase64 = avatarB64,
                        color        = friendColor,
                        isLive       = loc?.isRecent == true,
                        speedKmh     = loc?.speedKmh ?: 0f,
                        overlayBg    = overlayBg,
                        overlayBorder = overlayBorder,
                        overlayText  = overlayText,
                        overlayMuted = overlayMuted,
                        onTap        = {
                            loc?.let {
                                viewport.flyTo(CameraOptions.Builder().center(it.point).zoom(17.0).build())
                            }
                        }
                    )
                }
            }
        }

        // ── Bottom right buttons ───────────────────────────────────────────────
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(overlayBg).border(1.dp, overlayBorder, CircleShape)
                .clickable { myLocation?.let { d -> viewport.setCameraOptions { center(d.point); zoom(16.0) } } }, Alignment.Center) {
                Icon(Icons.Default.LocationOn, null, tint = EmeraldGreen, modifier = Modifier.size(22.dp))
            }
            Box(Modifier.size(52.dp).clip(CircleShape).background(if (isRecording) RedRecord else overlayBg)
                .border(1.dp, if (isRecording) RedRecord else overlayBorder, CircleShape)
                .clickable { if (isRecording) onStopRecording() else onStartRecording() }, Alignment.Center) {
                Text(if (isRecording) "⏹" else "⏺", fontSize = 20.sp)
            }
        }

        myLocation?.let { loc ->
            Surface(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
                shape = RoundedCornerShape(16.dp), color = overlayBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, overlayBorder)) {
                Column(Modifier.padding(12.dp)) {
                    Text("MY LOCATION", fontSize = 9.sp, color = overlayMuted, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("%.5f, %.5f".format(loc.point.latitude(), loc.point.longitude()), fontSize = 11.sp, color = overlayText, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    if (loc.speedKmh > 0.5f) Text("%.1f km/h".format(loc.speedKmh), fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── FIX 4: Friend popup card composable for map ──────────────────────────────
@Composable
fun FriendMapPopupCard(
    trackId: String,
    name: String,
    avatarBase64: String,
    color: Color,
    isLive: Boolean,
    speedKmh: Float,
    overlayBg: Color,
    overlayBorder: Color,
    overlayText: Color,
    overlayMuted: Color,
    onTap: () -> Unit
) {
    val bitmap = remember(avatarBase64) { base64ToBitmap(avatarBase64) }
    val shortName = name.split(" ").first().take(12)

    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable { onTap() },
        shape = RoundedCornerShape(14.dp),
        color = overlayBg,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isLive) color.copy(alpha = 0.7f) else overlayBorder
        )
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar or colored initial circle
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                    brush = if (bitmap != null) {
                        Brush.linearGradient(
                            listOf(color.copy(alpha = 0.15f), color.copy(alpha = 0.15f))
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    }
                )
                    .border(1.5.dp, color.copy(alpha = 0.6f), CircleShape),
                Alignment.Center
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkBg
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    shortName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = overlayText,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isLive) GreenOnline else overlayMuted)
                    )
                    Text(
                        if (isLive) {
                            if (speedKmh > 0.5f) "%.0f km/h".format(speedKmh) else "Live"
                        } else "Offline",
                        fontSize = 10.sp,
                        color = if (isLive) GreenOnline else overlayMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            // Tap-to-focus indicator
            Text("📍", fontSize = 10.sp)
        }
    }
}

// ─── Friends Tab ──────────────────────────────────────────────────────────────
@Composable
fun FriendsTab(
    savedFriends: List<SavedFriend>, trackedIds: List<String>,
    friendLocations: Map<String, FriendLocation?>,
    onShowFriendDialog: () -> Unit, onToggleTrack: (String) -> Unit,
    onRemoveSavedFriend: (String) -> Unit, onViewProfile: (SavedFriend) -> Unit,
    onOpenChatWith: (SavedFriend) -> Unit
) {
    val liveCount = trackedIds.count { id -> friendLocations[id]?.isRecent == true }
    Column(Modifier.fillMaxSize().background(DarkBg)) {
        Box(Modifier.fillMaxWidth().background(DarkSurface).padding(horizontal = 20.dp, vertical = 20.dp)) {
            Column {
                Text("Friends", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextOnDark)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${savedFriends.size} contacts", fontSize = 13.sp, color = TextOnDarkMuted)
                    if (liveCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(GreenOnline)); Spacer(Modifier.width(4.dp))
                            Text("$liveCount live", fontSize = 13.sp, color = GreenOnline, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.CenterEnd).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen)))
                .clickable(enabled = trackedIds.size < MAX_FRIENDS) { onShowFriendDialog() }
                .padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("+ Add", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = DarkBg)
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp)) {
            if (savedFriends.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👥", fontSize = 56.sp); Spacer(Modifier.height(16.dp))
                        Text("No friends yet", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextOnDark)
                        Spacer(Modifier.height(8.dp))
                        Text("Add a friend's Track ID to get started", fontSize = 13.sp, color = TextOnDarkMuted, textAlign = TextAlign.Center)
                    }
                }
            } else {
                savedFriends.forEach { sf ->
                    DarkFriendProfileCard(sf = sf, isTracking = sf.trackId in trackedIds, liveLocation = friendLocations[sf.trackId],
                        onViewProfile = { onViewProfile(sf) }, onToggleTrack = { onToggleTrack(sf.trackId) }, onOpenChat = { onOpenChatWith(sf) })
                    Spacer(Modifier.height(12.dp))
                }
                val untitledTracked = trackedIds.filter { id -> savedFriends.none { it.trackId == id } }
                if (untitledTracked.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp)); DarkSectionLabel("TRACKING WITHOUT PROFILE"); Spacer(Modifier.height(10.dp))
                    untitledTracked.forEachIndexed { idx, id ->
                        DarkFriendProfileCard(sf = SavedFriend(trackId = id, displayName = "", email = ""), isTracking = true,
                            liveLocation = friendLocations[id], onViewProfile = {}, onToggleTrack = { onToggleTrack(id) }, onOpenChat = {},
                            slotOverride = trackedIds.indexOf(id).takeIf { it >= 0 } ?: idx)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ─── Profile Tab — FIX 1: accept myAvatarVersion to force recompose ───────────
@Composable
fun ProfileTab(
    user: FirebaseUser, myTrackId: String, trackIdReady: Boolean,
    myLocation: LocationData?, isConnected: Boolean, gpsAvailable: Boolean,
    bgTrackingOn: Boolean, myAvatarBase64: String,
    myAvatarVersion: Int = 0,  // FIX 1
    onPickProfilePicture: () -> Unit,
    onStartBgTracking: () -> Unit, onStopBgTracking: () -> Unit,
    onStartRecording: () -> Unit, onStopRecording: () -> Unit,
    isRecording: Boolean, pathPoints: List<Point>, onSignOut: () -> Unit
) {
    val ctx = LocalContext.current
    val (accLabel, accColor) = myLocation?.let { accuracyLabel(it.accuracy) } ?: ("—" to TextOnDarkMuted)
    // FIX 1: key on both base64 and version so bitmap re-decodes immediately after upload
    val myBitmap = remember(myAvatarBase64, myAvatarVersion) { base64ToBitmap(myAvatarBase64) }

    Column(Modifier.fillMaxSize().background(DarkBg).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(DarkSurface, DarkBg))).padding(horizontal = 20.dp, vertical = 28.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(Modifier.size(88.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(EmeraldDeep, NeonGreen)))
                        .border(3.dp, EmeraldGreen.copy(alpha = 0.5f), CircleShape)
                        .clickable { onPickProfilePicture() }, Alignment.Center) {
                        if (myBitmap != null) {
                            androidx.compose.foundation.Image(bitmap = myBitmap.asImageBitmap(), contentDescription = "Profile picture", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Text((user.displayName?.firstOrNull() ?: "U").toString().uppercase(), color = DarkBg, fontWeight = FontWeight.Black, fontSize = 34.sp)
                        }
                    }
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen))).border(2.dp, DarkBg, CircleShape).clickable { onPickProfilePicture() }, Alignment.Center) {
                        Text("📷", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Tap to change photo", fontSize = 10.sp, color = TextOnDarkMuted)
                Spacer(Modifier.height(8.dp))
                Text(user.displayName ?: "User", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextOnDark)
                Text(user.email ?: "", fontSize = 13.sp, color = TextOnDarkMuted)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(if (isConnected) EmeraldGreen.copy(alpha = 0.12f) else DarkCard).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (isConnected) GreenOnline else TextOnDarkMuted))
                    Text(if (isConnected) "Online" else "Offline", fontSize = 12.sp, color = if (isConnected) GreenOnline else TextOnDarkMuted, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.TopEnd)) {
                Text("Sign out", color = TextOnDarkMuted, fontSize = 12.sp)
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            DarkSectionLabel("YOUR TRACK ID"); Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = DarkCard, border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderLight)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (trackIdReady) myTrackId else "Loading…", modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 17.sp, color = EmeraldGreen, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    var copied by remember { mutableStateOf(false) }
                    if (trackIdReady) {
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (copied) EmeraldGreen.copy(alpha = 0.2f) else DarkCardAlt)
                            .clickable { val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; cm.setPrimaryClip(android.content.ClipData.newPlainText("Track ID", myTrackId)); copied = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(if (copied) "✓ Copied" else "Copy", fontSize = 13.sp, color = if (copied) EmeraldGreen else TextOnDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            DarkSectionLabel("MY LOCATION"); Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = DarkCard, border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderLight)) {
                Column(Modifier.padding(16.dp)) {
                    val addressText = when { myLocation == null -> "Acquiring GPS fix…"; !gpsAvailable -> "${myLocation!!.address} (last known)"; else -> myLocation!!.address }
                    Text(addressText, fontSize = 13.sp, color = TextOnDark, lineHeight = 20.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkMiniStat("LAT", myLocation?.point?.latitude()?.let { "%.5f°".format(it) } ?: "—", Modifier.weight(1f))
                        DarkMiniStat("LNG", myLocation?.point?.longitude()?.let { "%.5f°".format(it) } ?: "—", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkMiniStat("SPEED", myLocation?.speedKmh?.let { if (it < 0.5f) "0.0 km/h" else "%.1f km/h".format(it) } ?: "—", Modifier.weight(1f))
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = DarkCardAlt) {
                            Column(Modifier.padding(12.dp)) {
                                Text("ACCURACY", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = TextOnDarkMuted)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(myLocation?.accuracy?.let { "±${it.toInt()}m" } ?: "—", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (gpsAvailable) accColor else TextOnDarkMuted, fontFamily = FontFamily.Monospace)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (gpsAvailable) accLabel else "Stale", fontSize = 11.sp, color = if (gpsAvailable) accColor else TextOnDarkMuted, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            DarkSectionLabel("BACKGROUND TRACKING"); Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (bgTrackingOn) EmeraldGreen.copy(alpha = 0.4f) else DarkBorderLight)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (bgTrackingOn) "Sharing in background" else "Background sharing off", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (bgTrackingOn) EmeraldGreen else TextOnDark)
                        Spacer(Modifier.height(3.dp))
                        Text(if (bgTrackingOn) "Friends can see you when app is closed" else "Enable to keep sharing when app is closed", fontSize = 12.sp, color = TextOnDarkMuted, lineHeight = 17.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = bgTrackingOn, onCheckedChange = { if (it) onStartBgTracking() else onStopBgTracking() },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBg, checkedTrackColor = EmeraldGreen, uncheckedThumbColor = TextOnDarkMuted, uncheckedTrackColor = DarkCardAlt))
                }
            }
            Spacer(Modifier.height(20.dp))
            DarkSectionLabel("PATH RECORDING"); Spacer(Modifier.height(10.dp))
            if (isRecording) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = RedRecord.copy(alpha = 0.10f), border = androidx.compose.foundation.BorderStroke(1.dp, RedRecord.copy(alpha = 0.3f))) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(RedRecord)); Spacer(Modifier.width(10.dp))
                        Text("Recording  •  ${pathPoints.size} points", fontSize = 13.sp, color = RedRecord, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp))
                .background(if (isRecording) RedRecord.copy(alpha = 0.15f) else DarkCard)
                .border(1.dp, if (isRecording) RedRecord else DarkBorderLight, RoundedCornerShape(16.dp))
                .clickable { if (isRecording) onStopRecording() else onStartRecording() }, contentAlignment = Alignment.Center) {
                Text(if (isRecording) "⏹  Stop Recording" else "⏺  Start Recording Path", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (isRecording) RedRecord else TextOnDark)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Dark Friend Profile Card — FIX 3: always show avatar in cards ────────────
@Composable
fun DarkFriendProfileCard(
    sf: SavedFriend, isTracking: Boolean, liveLocation: FriendLocation?,
    onViewProfile: () -> Unit, onToggleTrack: () -> Unit, onOpenChat: () -> Unit,
    slotOverride: Int = 0
) {
    val hasProfile = sf.displayName.isNotBlank()
    val isLive     = liveLocation?.isRecent == true

    Surface(modifier = Modifier.fillMaxWidth().then(if (hasProfile) Modifier.clickable { onViewProfile() } else Modifier),
        shape = RoundedCornerShape(20.dp), color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.5.dp,
            when { isLive -> EmeraldGreen.copy(alpha = 0.5f); isTracking -> AmberWarning.copy(alpha = 0.35f); else -> DarkBorderLight })) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // FIX 3: Pass avatarBase64 directly from sf — it's always populated now
                AvatarCircle(
                    base64 = sf.avatarBase64,
                    fallbackInitial = sf.displayName.firstOrNull()?.toString()?.uppercase() ?: sf.trackId.firstOrNull()?.toString() ?: "?",
                    size = 56, isTracking = isTracking, isLive = isLive
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    if (hasProfile) {
                        Text(sf.displayName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextOnDark)
                        Text(sf.email.ifBlank { "No email" }, fontSize = 11.sp, color = TextOnDarkMuted)
                    }
                    Text(sf.trackId, fontSize = if (hasProfile) 10.sp else 14.sp,
                        color = EmeraldGreen.copy(if (hasProfile) 0.7f else 1f),
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(when { isLive -> GreenOnline; isTracking -> AmberWarning; else -> TextOnDarkMuted }))
                        Spacer(Modifier.width(5.dp))
                        Text(when { isLive -> "Live now"; isTracking -> "Tracking"; else -> "Not tracked" }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = when { isLive -> GreenOnline; isTracking -> AmberWarning; else -> TextOnDarkMuted })
                    }
                }
                Box(Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (isTracking) RedRecord.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.12f))
                    .border(1.dp, if (isTracking) RedRecord.copy(alpha = 0.4f) else EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable { onToggleTrack() }.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(if (isTracking) "Untrack" else "Track", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (isTracking) RedRecord else EmeraldGreen)
                }
            }
            if (hasProfile) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(DarkBorderLight))
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(EmeraldGreen.copy(alpha = 0.10f))
                    .border(1.dp, EmeraldGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .clickable { onOpenChat() }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 14.sp); Spacer(Modifier.width(6.dp))
                        Text("Message ${sf.displayName.split(" ").first()}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldGreen)
                    }
                }
            }
        }
    }
}

// ─── User Profile Screen ──────────────────────────────────────────────────────
@Composable
fun UserProfileScreen(
    profile: SavedFriend, isTracking: Boolean, liveLocation: FriendLocation?,
    isAlreadySaved: Boolean, canTrack: Boolean,
    onBack: () -> Unit, onToggleTrack: (String) -> Unit,
    onAddFriend: (SavedFriend) -> Unit, onRemoveFriend: (String) -> Unit,
    onOpenChat: (SavedFriend) -> Unit
) {
    val isLive   = liveLocation?.isRecent == true
    val myBitmap = remember(profile.avatarBase64) { base64ToBitmap(profile.avatarBase64) }

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(DarkSurface, DarkBg))).padding(bottom = 32.dp)) {
                Box(Modifier.padding(top = 52.dp, start = 16.dp).size(40.dp).clip(CircleShape).background(DarkCard).clickable { onBack() }, Alignment.Center) {
                    Icon(Icons.Default.ArrowBack, null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.align(Alignment.BottomCenter).padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(96.dp).clip(CircleShape).background(Brush.linearGradient(listOf(EmeraldDeep, NeonGreen))).border(3.dp, EmeraldGreen.copy(alpha = 0.5f), CircleShape), Alignment.Center) {
                        if (myBitmap != null) {
                            androidx.compose.foundation.Image(bitmap = myBitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        } else {
                            Text(profile.displayName.firstOrNull()?.toString()?.uppercase() ?: "?", fontSize = 42.sp, fontWeight = FontWeight.Black, color = DarkBg)
                        }
                        if (isLive) Box(Modifier.size(20.dp).align(Alignment.BottomEnd).offset((-4).dp, (-4).dp).clip(CircleShape).background(GreenOnline).border(2.dp, DarkBg, CircleShape))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(profile.displayName.ifBlank { "Unknown" }, fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextOnDark)
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = DarkCard) {
                        Text(profile.trackId, modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen, fontFamily = FontFamily.Monospace, letterSpacing = 1.2.sp)
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                        .then(if (isTracking) Modifier.background(RedRecord.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        else Modifier.background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen)), RoundedCornerShape(14.dp)))
                        .border(1.dp, if (isTracking) RedRecord.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable(enabled = canTrack) { onToggleTrack(profile.trackId) }, contentAlignment = Alignment.Center) {
                        Text(if (isTracking) "⏹  Untrack" else "📍  Track", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (isTracking) RedRecord else DarkBg)
                    }
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (isAlreadySaved) DarkCard else EmeraldGreen.copy(alpha = 0.15f))
                        .border(1.dp, if (isAlreadySaved) DarkBorderLight else EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { if (isAlreadySaved) onRemoveFriend(profile.trackId) else onAddFriend(profile) }, contentAlignment = Alignment.Center) {
                        Text(if (isAlreadySaved) "✓  In Contacts" else "＋  Add Friend", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (isAlreadySaved) TextOnDarkMuted else EmeraldGreen)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
                    .background(EmeraldGreen.copy(alpha = 0.10f)).border(1.dp, EmeraldGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable { onOpenChat(profile) }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💬", fontSize = 18.sp); Spacer(Modifier.width(10.dp))
                        Text("Message ${profile.displayName.split(" ").first().ifBlank { "Friend" }}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = EmeraldGreen)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = DarkCard, border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderLight)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(when { isLive -> EmeraldGreen.copy(0.12f); isTracking -> AmberWarning.copy(0.12f); else -> DarkCardAlt }), Alignment.Center) {
                                Text(when { isLive -> "🟢"; isTracking -> "🟡"; else -> "⚫" }, fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(when { isLive -> "Live Now"; isTracking -> "Being Tracked"; else -> "Not Tracked" }, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = when { isLive -> GreenOnline; isTracking -> AmberWarning; else -> TextOnDarkMuted })
                                Text(when { isLive -> "Location updating in real-time"; isTracking -> "Last known location available"; else -> "Add to tracking to see location" }, fontSize = 11.sp, color = TextOnDarkMuted)
                            }
                        }
                        if (profile.email.isNotBlank()) {
                            Spacer(Modifier.height(16.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(DarkBorderLight)); Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(DarkCardAlt), Alignment.Center) { Text("✉", fontSize = 16.sp) }
                                Spacer(Modifier.width(12.dp))
                                Column { Text("EMAIL", fontSize = 10.sp, color = TextOnDarkMuted, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp); Text(profile.email, fontSize = 13.sp, color = TextOnDark) }
                            }
                        }
                        Spacer(Modifier.height(16.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(DarkBorderLight)); Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(DarkCardAlt), Alignment.Center) { Text("🔑", fontSize = 16.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column { Text("TRACK ID", fontSize = 10.sp, color = TextOnDarkMuted, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp); Text(profile.trackId, fontSize = 13.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                        }
                    }
                }
                if (isTracking && liveLocation != null) {
                    Spacer(Modifier.height(16.dp))
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = DarkCard,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isLive) EmeraldGreen.copy(0.3f) else DarkBorderLight)) {
                        Column(Modifier.padding(20.dp)) {
                            DarkSectionLabel("LIVE LOCATION"); Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DarkMiniStat("LAT", "%.5f°".format(liveLocation.point.latitude()), Modifier.weight(1f))
                                DarkMiniStat("LNG", "%.5f°".format(liveLocation.point.longitude()), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DarkMiniStat("SPEED", if (liveLocation.speedKmh < 0.5f) "0.0 km/h" else "%.1f km/h".format(liveLocation.speedKmh), Modifier.weight(1f))
                                DarkMiniStat("ACCURACY", "±${liveLocation.accuracy.toInt()} m", Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Friend Dialog — FIX 3: show avatar from lookup result ───────────────────
@Composable
fun FriendDialog(
    currentIds: List<String>, savedFriends: List<SavedFriend>, myUid: String,
    onDismiss: () -> Unit, onTrackNow: (SavedFriend) -> Unit,
    onAddAndTrack: (SavedFriend) -> Unit, onViewProfile: (SavedFriend) -> Unit
) {
    var idInput         by remember { mutableStateOf("") }
    var lookupResult    by remember { mutableStateOf<SavedFriend?>(null) }
    var lookupState     by remember { mutableStateOf("idle") }
    var lookupRequestId by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val kb    = LocalSoftwareKeyboardController.current

    val idNorm         = idInput.trim().uppercase()
    val alreadyTracked = idNorm in currentIds
    val slotsLeft      = MAX_FRIENDS - currentIds.size
    val canTrackById   = idNorm.isNotBlank() && !alreadyTracked && slotsLeft > 0 && lookupState == "found" && lookupResult != null

    fun doIdLookup() {
        if (idNorm.isBlank()) return
        val requestedId = idNorm; lookupRequestId += 1; val requestId = lookupRequestId
        lookupState = "loading"; lookupResult = null
        scope.launch(Dispatchers.IO) {
            val result = httpGet("/api/user/by-trackid/$requestedId")
            withContext(Dispatchers.Main) {
                if (requestId != lookupRequestId || idInput.trim().uppercase() != requestedId) return@withContext
                if (result != null && result.has("trackId")) {
                    val resolvedTrackId = result.getString("trackId")
                    lookupResult = SavedFriend(
                        trackId      = resolvedTrackId,
                        displayName  = result.optString("displayName", "").ifBlank { resolvedTrackId },
                        email        = result.optString("email", ""),
                        // FIX 3: ensure avatarBase64 is captured from lookup so it shows in the card
                        avatarBase64 = result.optString("avatarBase64", "")
                    )
                    lookupState = "found"
                } else lookupState = "notfound"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = DarkCard, modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderLight)) {
            Column(Modifier.padding(24.dp)) {
                Text("Add Friend", fontWeight = FontWeight.Black, fontSize = 22.sp, color = TextOnDark)
                Text("Enter a Track ID to find a friend", fontSize = 13.sp, color = TextOnDarkMuted)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = idInput, onValueChange = { idInput = it.uppercase(); lookupRequestId += 1; lookupState = "idle"; lookupResult = null },
                        placeholder = { Text("TRK-XXXXXXXX", color = TextOnDarkMuted) }, isError = alreadyTracked,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldGreen, unfocusedBorderColor = DarkBorderLight, focusedContainerColor = DarkCardAlt, unfocusedContainerColor = DarkCardAlt, focusedTextColor = TextOnDark, unfocusedTextColor = TextOnDark, cursorColor = EmeraldGreen),
                        singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { kb?.hide(); doIdLookup() }))
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen)))
                        .clickable(enabled = idNorm.isNotBlank() && lookupState != "loading") { kb?.hide(); doIdLookup() }, Alignment.Center) {
                        if (lookupState == "loading") CircularProgressIndicator(Modifier.size(20.dp), color = DarkBg, strokeWidth = 2.dp)
                        else Text("🔍", fontSize = 18.sp)
                    }
                }
                if (lookupState == "found" && lookupResult != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = DarkCardAlt, border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            // FIX 3: Avatar now shows because avatarBase64 is properly captured above
                            AvatarCircle(
                                base64 = lookupResult!!.avatarBase64,
                                fallbackInitial = lookupResult!!.displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                                size = 44, isTracking = true
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(lookupResult!!.displayName.ifBlank { "Unknown" }, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextOnDark)
                                Text(lookupResult!!.trackId, fontSize = 10.sp, color = EmeraldGreen, fontFamily = FontFamily.Monospace)
                                if (lookupResult!!.email.isNotBlank()) {
                                    Text(lookupResult!!.email, fontSize = 10.sp, color = TextOnDarkMuted)
                                }
                            }
                        }
                    }
                }
                if (lookupState == "notfound") {
                    Spacer(Modifier.height(12.dp))
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = AmberWarning.copy(alpha = 0.10f), border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.3f))) {
                        Row(Modifier.padding(12.dp)) { Text("⚠ No account found for this Track ID", fontSize = 12.sp, color = AmberWarning, fontWeight = FontWeight.SemiBold) }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(DarkCardAlt).border(1.dp, DarkBorderLight, RoundedCornerShape(14.dp)).clickable { onDismiss() }, Alignment.Center) {
                        Text("Cancel", color = TextOnDarkMuted, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (canTrackById) Brush.linearGradient(listOf(EmeraldDeep, EmeraldGreen)) else Brush.linearGradient(listOf(DarkCardAlt, DarkCardAlt)))
                        .clickable(enabled = canTrackById) { lookupResult?.let { onTrackNow(it) } }, Alignment.Center) {
                        Text(if (lookupState == "found") "Add Friend" else "Search First", fontWeight = FontWeight.ExtraBold, color = if (canTrackById) DarkBg else TextOnDarkMuted)
                    }
                }
            }
        }
    }
}

// ─── Permission Screen ────────────────────────────────────────────────────────
@Composable
fun PermScreen(onRequest: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkBg), Alignment.Center) {
        Box(Modifier.size(300.dp).background(Brush.radialGradient(listOf(EmeraldGreen.copy(alpha = 0.08f), Color.Transparent)), CircleShape))
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(EmeraldDeep, NeonGreen))), Alignment.Center) { Text("📍", fontSize = 40.sp) }
            Spacer(Modifier.height(24.dp))
            Text("Location Access", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextOnDark, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Grant location access to see your position and share with friends.", fontSize = 14.sp, color = TextOnDarkMuted, textAlign = TextAlign.Center, lineHeight = 22.sp)
            Spacer(Modifier.height(36.dp))
            Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(EmeraldDeep, NeonGreen))).clickable { onRequest() }, Alignment.Center) {
                Text("Grant Permission", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = DarkBg)
            }
        }
    }
}

// ─── Reusable dark UI components ──────────────────────────────────────────────
@Composable
fun DarkSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(EmeraldGreen))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = TextOnDarkMuted)
    }
}

@Composable
fun DarkMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = DarkCardAlt, border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderLight)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = TextOnDarkMuted)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextOnDark, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable fun SectionLabel(text: String) = DarkSectionLabel(text)
@Composable fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) = DarkMiniStat(label, value, modifier)

// ─── FriendCard legacy ────────────────────────────────────────────────────────
@Composable
fun FriendCard(id: String, slot: Int, color: Color, label: String, loc: FriendLocation?, savedName: String? = null, onRemove: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (loc?.isRecent == true) color.copy(alpha = 0.40f) else DarkBorderLight)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)), Alignment.Center) {
                    Text(savedName?.firstOrNull()?.toString()?.uppercase() ?: "${slot + 1}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (!savedName.isNullOrBlank()) { Text(savedName, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextOnDark); Spacer(Modifier.height(1.dp)) }
                    Text(id, fontSize = if (savedName.isNullOrBlank()) 14.sp else 11.sp, fontWeight = FontWeight.ExtraBold, color = if (savedName.isNullOrBlank()) TextOnDark else TextOnDarkMuted, fontFamily = FontFamily.Monospace)
                }
                Box(Modifier.size(32.dp).clip(CircleShape).background(DarkCardAlt).clickable { onRemove() }, Alignment.Center) {
                    Text("✕", fontSize = 13.sp, color = TextOnDarkMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SavedFriendsSection(savedFriends: List<SavedFriend>, trackedIds: List<String>, onToggleTrack: (String) -> Unit, onRemoveSavedFriend: (String) -> Unit, onViewProfile: (SavedFriend) -> Unit, onOpenChatWith: (SavedFriend) -> Unit) {
    savedFriends.forEach { sf ->
        DarkFriendProfileCard(sf = sf, isTracking = sf.trackId in trackedIds, liveLocation = null,
            onViewProfile = { onViewProfile(sf) }, onToggleTrack = { onToggleTrack(sf.trackId) }, onOpenChat = { onOpenChatWith(sf) })
        Spacer(Modifier.height(10.dp))
    }
}

// ─── GPS ──────────────────────────────────────────────────────────────────────
@SuppressLint("MissingPermission")
fun startGps(fused: FusedLocationProviderClient, onCb: (LocationCallback) -> Unit, onAvailability: (Boolean) -> Unit, onLoc: (Point, Float, Float) -> Unit) {
    val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, GPS_INTERVAL_MS)
        .setMinUpdateIntervalMillis(GPS_MIN_DELAY_MS)
        .setWaitForAccurateLocation(false)
        .setMaxUpdateDelayMillis(GPS_INTERVAL_MS)
        .build()

    val cb = object : LocationCallback() {
        override fun onLocationAvailability(availability: LocationAvailability) {
            if (!availability.isLocationAvailable) GpsFilter.reset()
            onAvailability(availability.isLocationAvailable)
        }
        override fun onLocationResult(result: LocationResult) {
            val loc = result.locations
                .filter { it.accuracy > 0 && it.accuracy <= ACC_ACCEPTABLE }
                .minByOrNull { it.accuracy }
                ?: result.locations.minByOrNull { it.accuracy }
                ?: return
            val nowMs = System.currentTimeMillis()
            val spdMs = if (loc.hasSpeed() && loc.speed >= 0) loc.speed else 0f
            val smoothed = GpsFilter.process(loc.latitude, loc.longitude, loc.accuracy, nowMs) ?: return
            onLoc(Point.fromLngLat(smoothed.second, smoothed.first), GpsFilter.smoothedAccuracy, spdMs)
        }
    }
    onCb(cb); fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
}

// ─── Geocoder ─────────────────────────────────────────────────────────────────
fun geocode(ctx: Context, lat: Double, lon: Double): String = try {
    val list = Geocoder(ctx, Locale.getDefault()).getFromLocation(lat, lon, 1)
    if (!list.isNullOrEmpty()) {
        val a = list[0]
        listOfNotNull(a.subLocality ?: a.thoroughfare, a.locality ?: a.subAdminArea, a.adminArea, a.countryName).joinToString(", ")
    } else "Address not found"
} catch (_: Exception) { "Address unavailable" }

// ─── MyFriendsScreen ─────────────────────────────────────────────────────────
@Composable
fun MyFriendsScreen(savedFriends: List<SavedFriend>, trackedIds: List<String>, friendLocations: Map<String, FriendLocation?>, onBack: () -> Unit, onViewProfile: (SavedFriend) -> Unit, onToggleTrack: (String) -> Unit, onRemoveSavedFriend: (String) -> Unit, onOpenChat: (SavedFriend) -> Unit) {
    Box(Modifier.fillMaxSize().background(DarkBg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().background(DarkSurface).padding(top = 52.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(DarkCard).clickable { onBack() }, Alignment.Center) {
                        Icon(Icons.Default.ArrowBack, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("My Friends", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextOnDark)
                        Text("${savedFriends.size} contacts", fontSize = 12.sp, color = TextOnDarkMuted)
                    }
                }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                savedFriends.forEach { sf ->
                    DarkFriendProfileCard(sf = sf, isTracking = sf.trackId in trackedIds, liveLocation = friendLocations[sf.trackId],
                        onViewProfile = { onViewProfile(sf) }, onToggleTrack = { onToggleTrack(sf.trackId) }, onOpenChat = { onOpenChat(sf) })
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
