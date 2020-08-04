package com.crazylegend.kotlinextensions.activity

import android.Manifest.permission.WRITE_SETTINGS
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.*
import android.view.View.GONE
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.crazylegend.kotlinextensions.R
import com.crazylegend.kotlinextensions.context.appOpsManager
import com.crazylegend.kotlinextensions.packageutils.buildIsMarshmallowAndUp
import com.crazylegend.kotlinextensions.tryOrElse
import com.crazylegend.kotlinextensions.views.statusBarHeight


/**
 * Created by hristijan on 2/22/19 to long live and prosper !
 */

/**
 * Hide keyboard for an activity.
 */
fun Activity.hideSoftKeyboard() {
    if (currentFocus != null) {
        val inputMethodManager = getSystemService(
                Context
                        .INPUT_METHOD_SERVICE
        ) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
    }
}


/**
 * Returns the Activity's content (root) view.
 */
val Activity.rootView: View
    get() = findViewById(android.R.id.content)

/**
 * Show keyboard for an activity.
 */
fun Activity.showKeyboard(toFocus: View) {
    toFocus.requestFocus()
    val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    // imm.showSoftInput(toFocus, InputMethodManager.SHOW_IMPLICIT);
    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY)
}

/**
 * Returns display density as ...DPI
 */
fun AppCompatActivity.getDisplayDensity(): String {
    val metrics = DisplayMetrics()
    this.windowManager.defaultDisplay.getMetrics(metrics)
    return when (metrics.densityDpi) {
        DisplayMetrics.DENSITY_LOW -> "LDPI"
        DisplayMetrics.DENSITY_MEDIUM -> "MDPI"
        DisplayMetrics.DENSITY_HIGH -> "HDPI"
        DisplayMetrics.DENSITY_XHIGH -> "XHDPI"
        DisplayMetrics.DENSITY_XXHIGH -> "XXHDPI"
        DisplayMetrics.DENSITY_XXXHIGH -> "XXXHDPI"
        else -> "XXHDPI"
    }
}

/**
 * Returns the StatusBarHeight in Pixels
 */
val Activity.getStatusBarHeight: Int
    get() {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        return rect.top
    }

val Activity.displaySizePixels: Point
    get() {
        val display = this.windowManager.defaultDisplay
        return DisplayMetrics()
                .apply {
                    display.getRealMetrics(this)
                }.let {
                    Point(it.widthPixels, it.heightPixels)
                }
    }

/**
 * Set Status Bar Color if Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
fun Activity.setStatusBarColor(@ColorInt color: Int) {
    window.statusBarColor = color
}

/**
 * Set Navigation Bar Color if Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
fun Activity.setNavigationBarColor(@ColorInt color: Int) {
    window.navigationBarColor = color
}

/**
 * Set Navigation Bar Divider Color if Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
 */
@RequiresApi(api = Build.VERSION_CODES.P)
fun Activity.setNavigationBarDividerColor(@ColorInt color: Int) {
    window.navigationBarDividerColor = color
}

/**
 *  Makes the activity enter fullscreen mode.
 */
@UiThread
fun Activity.enterFullScreenMode() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
}


/**
 * Makes the Activity exit fullscreen mode.
 */
@UiThread
fun Activity.exitFullScreenMode() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
}

/**
 * Restarts an activity from itself with a fade animation
 * Keeps its existing extra bundles and has a intentBuilder to accept other parameters
 */
inline fun Activity.restart(intentBuilder: Intent.() -> Unit = {}) {
    val i = Intent(this, this::class.java)
    val oldExtras = intent.extras
    if (oldExtras != null)
        i.putExtras(oldExtras)
    i.intentBuilder()
    startActivity(i)
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out) //No transitions
    finish()
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
}

/**
 * Force restart an entire application
 */
@RequiresApi(Build.VERSION_CODES.M)
fun Activity.restartApplication() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    val pending = PendingIntent.getActivity(this, 666, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (buildIsMarshmallowAndUp)
        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + 100, pending)
    else
        alarm.setExact(AlarmManager.RTC, System.currentTimeMillis() + 100, pending)
    finish()
    System.exit(0)
}

fun Activity.finishSlideOut() {
    finish()
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
}


inline fun <reified T> Activity.startActivityForResult(
        requestCode: Int,
        bundleBuilder: Bundle.() -> Unit = {},
        intentBuilder: Intent.() -> Unit = {}
) {
    val intent = Intent(this, T::class.java)
    intent.intentBuilder()
    val bundle = Bundle()
    bundle.bundleBuilder()
    startActivityForResult(intent, requestCode, if (bundle.isEmpty) null else bundle)
}


/**
 * Simplify using AlertDialog
 */
inline fun Activity.alert(body: AlertDialog.Builder.() -> AlertDialog.Builder) {
    AlertDialog.Builder(this)
            .body()
            .show()
}

inline fun <reified T : Any> Activity.launchActivity(
        requestCode: Int = -1,
        options: Bundle? = null,
        noinline init: Intent.() -> Unit = {}
) {
    val intent = newIntent<T>(this)
    intent.init()

    startActivityForResult(intent, requestCode, options)

}

inline fun <reified T : Any> Context.launchActivity(
        options: Bundle? = null,
        noinline init: Intent.() -> Unit = {}
) {
    val intent = newIntent<T>(this)
    intent.init()
    startActivity(intent, options)

}

inline fun <reified T : Any> newIntent(context: Context): Intent =
        Intent(context, T::class.java)

fun Activity.setBackgroundColor(@ColorInt color: Int) {
    window.setBackgroundDrawable(ColorDrawable(color))
}


fun FragmentActivity.switchFragment(
        showFragment: Fragment,
        @IdRes containerId: Int,
        transaction: Int = FragmentTransaction.TRANSIT_NONE
) {
    supportFragmentManager.switch(showFragment, containerId, transaction)
}

fun Fragment.hide() {
    this.parentFragmentManager.hide(this)
}

fun Fragment.show() {
    this.parentFragmentManager.show(this)
}

fun Fragment.remove() {
    this.parentFragmentManager.remove(this)
}

fun Fragment.showHide(
        vararg hideFragment: Fragment,
        transaction: Int = FragmentTransaction.TRANSIT_NONE
) {
    this.parentFragmentManager.showHide(this, *hideFragment, transaction = transaction)
}

fun FragmentManager.add(
        addFragment: Fragment,
        @IdRes containerId: Int,
        isHide: Boolean = false,
        isAddStack: Boolean = false,
        tag: String = addFragment::class.java.name
) {
    val ft = this.beginTransaction()
    val fragmentByTag = this.findFragmentByTag(tag)
    if (fragmentByTag != null && fragmentByTag.isAdded) {
        ft.remove(fragmentByTag)
    }
    ft.add(containerId, addFragment, tag)
    if (isHide) ft.hide(addFragment)
    if (isAddStack) ft.addToBackStack(tag)

    ft.commit()
}


fun FragmentManager.add(
        addList: List<Fragment>,
        @IdRes containerId: Int,
        showIndex: Int = 0
) {
    val ft = this.beginTransaction()
    for (i in addList.indices) {
        val addFragment = addList[i]
        val tag = addFragment::class.java.name
        val fragmentByTag = this.findFragmentByTag(tag)
        if (fragmentByTag != null && fragmentByTag.isAdded) {
            ft.remove(fragmentByTag)
        }
        ft.add(containerId, addFragment, tag)

        if (showIndex != i) ft.hide(addFragment)
    }
    ft.commit()
}


fun FragmentManager.hide(vararg hideFragment: Fragment) {
    hide(hideFragment.toList())
}


fun FragmentManager.hide(hideFragment: List<Fragment>) {
    val ft = this.beginTransaction()
    for (fragment in hideFragment) {
        ft.hide(fragment)
    }
    ft.commit()
}


fun FragmentManager.show(showFragment: Fragment) {
    val ft = this.beginTransaction()
    ft.show(showFragment)
    ft.commit()
}


fun FragmentManager.remove(vararg removeFragment: Fragment) {
    val ft = this.beginTransaction()
    for (fragment in removeFragment) {
        ft.remove(fragment)
    }
    ft.commit()
}


fun FragmentManager.removeTo(removeTo: Fragment, isIncludeSelf: Boolean = false) {
    val ft = this.beginTransaction()
    val fragments = this.getFragmentManagerFragments()
    for (i in (fragments.size - 1)..0) {
        val fragment = fragments[i]
        if (fragment == removeTo && isIncludeSelf) {
            ft.remove(fragment)
            break
        }
        ft.remove(fragment)
    }
    ft.commit()
}


fun FragmentManager.removeAll() {
    val frg = getFragmentManagerFragments()
    if (frg.isEmpty()) return

    val ft = this.beginTransaction()
    for (fragment in frg) {
        ft.remove(fragment)
    }
    ft.commit()
}


fun FragmentManager.showHide(
        showFragment: Fragment,
        vararg hideFragment: Fragment,
        transaction: Int = FragmentTransaction.TRANSIT_NONE
) {
    val ft = this.beginTransaction().setTransition(transaction)

    ft.show(showFragment)
    for (fragment in hideFragment) {
        if (fragment != showFragment) {
            ft.hide(fragment)
        }
    }

    ft.commit()
}


fun FragmentManager.replace(
        fragment: Fragment,
        @IdRes containerId: Int,
        isAddStack: Boolean = false,
        tag: String = fragment::class.java.name
) {
    val ft = this.beginTransaction()

    ft.replace(containerId, fragment, tag)
    if (isAddStack) ft.addToBackStack(tag)

    ft.commit()
}


fun FragmentManager.switch(
        showFragment: Fragment,
        @IdRes containerId: Int,
        transaction: Int = FragmentTransaction.TRANSIT_NONE
) {
    val ft = this.beginTransaction().setTransition(transaction)

    val tag = showFragment::class.java.name
    val fragmentByTag = this.findFragmentByTag(tag)
    if (fragmentByTag != null && fragmentByTag.isAdded) {
        ft.show(fragmentByTag)
    } else {
        ft.add(containerId, showFragment, tag)
    }

    for (tempF in this.getFragmentManagerFragments()) {
        if (tempF != fragmentByTag) {
            ft.hide(tempF)
        }
    }
    ft.commit()
}

fun FragmentManager.getTopFragment(): Fragment? {
    val frg = getFragmentManagerFragments()
    return frg.ifEmpty { return null }[frg.size - 1]
}

fun FragmentManager.getFragmentManagerFragments(): List<Fragment> {
    return this.fragments
}

inline fun <reified T : Fragment> FragmentManager.findFragment(): Fragment? {
    return this.findFragmentByTag(T::class.java.name)
}

inline var Context.sleepDuration: Int
    @RequiresPermission(WRITE_SETTINGS)
    set(value) {
        Settings.System.putInt(
                this.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                value
        )
    }
    get() {
        return try {
            Settings.System.getInt(
                    this.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            -123
        }
    }


@SuppressLint("ObsoleteSdkInt")
fun Activity.hideBottomBar() {
    if (Build.VERSION.SDK_INT < 19) { // lower api
        val v = this.window.decorView
        v.systemUiVisibility = GONE
    } else {
        //for new api versions.
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions
    }
}

/**
 * Sets the screen brightness. Call this before setContentView.
 * 0 is dimmest, 1 is brightest. Default value is 1
 */
fun Activity.brightness(brightness: Float = 1f) {
    val params = window.attributes
    params.screenBrightness = brightness // range from 0 - 1 as per docs
    window.attributes = params
    window.addFlags(WindowManager.LayoutParams.FLAGS_CHANGED)
}


fun <T> Activity.extra(key: String): Lazy<T?> {
    return lazy(LazyThreadSafetyMode.NONE) {
        @Suppress("UNCHECKED_CAST")
        intent.extras?.get(key) as T
    }
}

fun <T> Activity.extraOrNull(key: String): Lazy<T?> {
    return lazy(LazyThreadSafetyMode.NONE) {
        @Suppress("UNCHECKED_CAST")
        intent.extras?.get(key) as? T?
    }
}


@SuppressLint("ObsoleteSdkInt")
fun Activity.showBottomBar() {
    if (Build.VERSION.SDK_INT < 19) { // lower api
        val v = this.window.decorView
        v.systemUiVisibility = View.VISIBLE
    } else {
        //for new api versions.
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_VISIBLE
        decorView.systemUiVisibility = uiOptions
    }
}

val View.hasNotch: Boolean
    @RequiresApi(Build.VERSION_CODES.P)
    get() {
        return rootWindowInsets?.displayCutout != null
    }


fun Activity.lockOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
}

fun Activity.lockCurrentScreenOrientation() {
    requestedOrientation = when (resources.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }
}

fun Activity.unlockScreenOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
}

inline val Activity.isInMultiWindow: Boolean
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isInMultiWindowMode
        } else {
            false
        }
    }

/**
 * Iterate through fragments attached to FragmentManager and pop's one after the other.
 */
fun AppCompatActivity.popWholeBackStack() {
    val fragmentManager = this.supportFragmentManager
    for (i in 0 until fragmentManager.fragments.size) {
        fragmentManager.popBackStack()
    }
}

fun Activity.getBitmapFromUri(uri: Uri): Bitmap? {
    return contentResolver.openInputStream(uri)?.use {
        return@use BitmapFactory.decodeStream(it)
    }
}

fun Activity.makeSceneTransitionAnimation(view: View, transitionName: String): ActivityOptionsCompat =
        ActivityOptionsCompat.makeSceneTransitionAnimation(this, view, transitionName)

fun Context.asActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext as Activity
    else -> throw IllegalStateException("Context $this NOT contains activity!")
}

fun Context.asFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is Activity -> throw IllegalStateException("Context $this NOT supported Activity")
    is ContextWrapper -> baseContext as FragmentActivity
    else -> throw IllegalStateException("Context $this NOT contains activity!")
}

inline fun <reified T : Any> Activity.launchActivityAndFinish() {
    val intent = newIntent<T>(this)
    startActivity(intent)
    finish()
}


fun AppCompatActivity.setupToolbar(
        toolbar: Toolbar,
        displayHomeAsUpEnabled: Boolean = true,
        displayShowHomeEnabled: Boolean = true,
        displayShowTitleEnabled: Boolean = false,
        showUpArrowAsCloseIcon: Boolean = false,
        @DrawableRes closeIconDrawableRes: Int? = null
) {
    setSupportActionBar(toolbar)
    supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled)
        setDisplayShowHomeEnabled(displayShowHomeEnabled)
        setDisplayShowTitleEnabled(displayShowTitleEnabled)

        if (showUpArrowAsCloseIcon && closeIconDrawableRes != null) {
            setHomeAsUpIndicator(AppCompatResources.getDrawable(this@setupToolbar, closeIconDrawableRes))
        }
    }
}

fun AppCompatActivity.onSupportNavigateUpGoBack(): Boolean {
    onBackPressed()
    return true
}

fun isKeyboardSubmit(actionId: Int, event: KeyEvent?): Boolean =
        actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_ENTER)


fun Activity.enableFullScreen() {
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
}


fun Activity?.hideKeyboardForced() {
    this?.currentFocus?.let { currentFocus ->
        try {
            (currentFocus.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(currentFocus.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        } catch (e: Exception) {
        }
    }
}

fun Activity?.hideKeyboard() {
    (this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
            .toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
}

fun Activity?.showKeyboard() {
    (this?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
            .toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun Activity.keepScreenOn() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun Activity.keepScreenOFF() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

val Activity.isImmersiveModeEnabled
    get() = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY == window.decorView.systemUiVisibility

operator fun Window.plusAssign(flags: Int) {
    addFlags(flags)
}

operator fun Window.minusAssign(flags: Int) {
    clearFlags(flags)
}

fun Activity.setTransparentStatusBarFlags() {
    setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
    window.statusBarColor = Color.TRANSPARENT
}

fun Activity.setWindowFlag(bits: Int, on: Boolean) {
    val winParams = window.attributes
    if (on) {
        winParams.flags = winParams.flags or bits
    } else {
        winParams.flags = winParams.flags and bits.inv()
    }
    window.attributes = winParams
}

var Activity.isLightNavigationBar
    @RequiresApi(Build.VERSION_CODES.O)
    get() = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR == window.decorView.systemUiVisibility
    set(enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return

        if (enabled) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }
    }


var Activity.isLightStatusBar
    @RequiresApi(Build.VERSION_CODES.M)
    get() = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR == window.decorView.systemUiVisibility
    set(enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return

        if (enabled) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }


/**
 * An extension to `postponeEnterTransition` which will resume after a timeout.
 */
fun Activity.postponeEnterTransition(timeout: Long) {
    postponeEnterTransition()
    window.decorView.postDelayed({ startPostponedEnterTransition() }, timeout)
}


fun Activity.fixSoftInputLeaks() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
    val leakViews = arrayOf("mLastSrvView", "mCurRootView", "mServedView", "mNextServedView")
    for (leakView in leakViews) {
        try {
            val leakViewField = InputMethodManager::class.java.getDeclaredField(leakView)
                    ?: continue
            if (!leakViewField.isAccessible) {
                leakViewField.isAccessible = true
            }
            val obj = leakViewField.get(imm) as? View ?: continue
            if (obj.rootView === this.window.decorView.rootView) {
                leakViewField.set(imm, null)
            }
        } catch (ignore: Throwable) { /**/
        }
    }
}

fun Activity.screenShot(removeStatusBar: Boolean = false): Bitmap? {
    val dm = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(dm)

    val bmp = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.RGB_565)
    val canvas = Canvas(bmp)
    window.decorView.draw(canvas)


    return if (removeStatusBar) {
        val statusBarHeight = statusBarHeight
        Bitmap.createBitmap(
                bmp,
                0,
                statusBarHeight,
                dm.widthPixels,
                dm.heightPixels - statusBarHeight
        )
    } else {
        Bitmap.createBitmap(bmp, 0, 0, dm.widthPixels, dm.heightPixels)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun Activity.screenShot(removeStatusBar: Boolean = false, listener: (Int, Bitmap) -> Unit) {

    val rect = Rect()
    windowManager.defaultDisplay.getRectSize(rect)

    if (removeStatusBar) {
        val statusBarHeight = this.statusBarHeight

        rect.set(rect.left, rect.top + statusBarHeight, rect.right, rect.bottom)
    }
    val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)

    PixelCopy.request(this.window, rect, bitmap, {
        listener(it, bitmap)
    }, Handler(this.mainLooper))
}


/**
 * Set the action bar title
 * @param title String res of the title
 */
fun AppCompatActivity.setToolbarTitle(@StringRes title: Int) {
    supportActionBar?.setTitle(title)
}


/**
 * Set the action bar title
 * @param title String value of the title
 */
fun AppCompatActivity.setToolbarTitle(title: String) {
    supportActionBar?.title = title
}

fun Activity.hasPipPermission(): Boolean {
    val appOps = appOpsManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        } else {
            appOps?.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        }
    } else {
        tryOrElse(false) { supportsPictureInPicture }
    }
}