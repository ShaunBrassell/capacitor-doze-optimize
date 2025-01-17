package io.github.asephermann.plugins.dozeoptimize

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin


@CapacitorPlugin(name = "DozeOptimize")
class DozeOptimizePlugin : Plugin() {

    /**
     * return true if in App's Battery settings "Not optimized" and false if "Optimizing battery use"
     */
    @PluginMethod
    fun isIgnoringBatteryOptimizations(call: PluginCall) {

        val packageName: String = activity.applicationContext.packageName

        val ret = JSObject()

        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = pm.isIgnoringBatteryOptimizations(packageName)

                ret.put("isIgnoring", isIgnoring)
                ret.put("messages", isIgnoring.toString())
            } else {
                ret.put("isIgnoring", false)
                ret.put("messages", "BATTERY_OPTIMIZATIONS Not available.")
            }
        } catch (e: Exception) {
            ret.put("isIgnoring", false)
            ret.put("messages", "IsIgnoringBatteryOptimizations: failed N/A\n$e")
        }

        call.resolve(ret)
    }

    @PluginMethod
    fun requestOptimizationsMenu(call: PluginCall) {

        val packageName: String = activity.applicationContext.packageName

        val ret = JSObject()

        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                val intent = Intent()

                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = pm.isIgnoringBatteryOptimizations(packageName)
                if (isIgnoring) {
                    intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    ret.put("messages", "ignored")
                } else {
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
//                    https://stackoverflow.com/questions/55794316/action-ignore-battery-optimization-settings-need-intent-to-all-apps-list?rq=1
//                    including the line "intent.setData(Uri.parse("package:" + packageName));" can get your application blocked by google play. use it without this line –
//                    kfir
//                    Nov 12, 2019 at 11:44
                    ret.put("messages", "Optimizations Requested Successfully")
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)

//                if (isIgnoring) {
//                    val res: Resources = context.resources
//                    val applicationName: CharSequence = res.getText(
//                        res.getIdentifier(
//                            "app_name",
//                            "string", context.packageName
//                        )
//                    )
//
//                    val alertDialog: AlertDialog = AlertDialog.Builder(context).create()
//                    alertDialog.setTitle("Battery Usage")
//                    alertDialog.setMessage("Battery optimization -> All apps -> $applicationName -> Don't optimize")
//                    alertDialog.setButton(
//                        AlertDialog.BUTTON_POSITIVE, "OK"
//                    ) { _, _ -> context.startActivity(intent) }
//                    alertDialog.setButton(
//                        AlertDialog.BUTTON_NEUTRAL, "CANCEL"
//                    ) { dialog, _ -> dialog.dismiss() }
//                    alertDialog.show()
//                } else {
                    context.startActivity(intent)
//                }

                ret.put("isRequested", true)
            } else {
                ret.put("isRequested", false)
                ret.put("messages", "BATTERY_OPTIMIZATIONS Not available.")
            }
        } catch (e: Exception) {
            ret.put("isRequested", false)
            ret.put("messages", "RequestOptimizationsMenu: failed N/A\n$e")
        }

        call.resolve(ret)
    }

    @PluginMethod
    fun isIgnoringDataSaver(call: PluginCall) {
        val ret = JSObject()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                var isIgnoring = false
                val connMgr =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                when (connMgr.restrictBackgroundStatus) {
                    RESTRICT_BACKGROUND_STATUS_ENABLED ->
                        // The app is whitelisted. Wherever possible,
                        // the app should use less data in the foreground and background.
                        isIgnoring = false
                    RESTRICT_BACKGROUND_STATUS_WHITELISTED, RESTRICT_BACKGROUND_STATUS_DISABLED ->                             // Data Saver is disabled. Since the device is connected to a
                        // metered network, the app should use less data wherever possible.
                        isIgnoring = true
                }
                ret.put("isIgnoring", isIgnoring)
                ret.put("messages", isIgnoring.toString())
            } else {
                ret.put("isIgnoring", false)
                ret.put("messages", "DATA_SAVER Not available.")
            }
        } catch (e: Exception) {
            ret.put("isIgnoring", false)
            ret.put("messages", "IsIgnoringDataSaver: failed N/A\n$e")
        }

        call.resolve(ret)
    }

    @PluginMethod
    fun requestDataSaverMenu(call: PluginCall) {

        val packageName: String = activity.applicationContext.packageName

        val ret = JSObject()

        try {
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                val connMgr =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                when (connMgr.restrictBackgroundStatus) {
                    RESTRICT_BACKGROUND_STATUS_ENABLED -> {// 3
                        // The app is whitelisted. Wherever possible,
                        // the app should use less data in the foreground and background.

                        intent.action = Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.data = Uri.parse("package:$packageName")
                        context.startActivity(intent)

                        ret.put("isRequested", true)
                        ret.put("messages", "requested")
                    }
                    RESTRICT_BACKGROUND_STATUS_WHITELISTED, // 2
                    RESTRICT_BACKGROUND_STATUS_DISABLED -> {// 1
                        // Data Saver is disabled. Since the device is connected to a
                        // metered network, the app should use less data wherever possible.

                        ret.put("isRequested", false)
                        ret.put("messages", "not requested")
                    }
                }
            } else {
                ret.put("isRequested", false)
                ret.put("messages", "DATA_SAVER Not available.")
            }
        } catch (e: Exception) {
            ret.put("isRequested", false)
            ret.put("messages", "RequestDataSaverMenu failed: N/A\n$e")
        }

        call.resolve(ret)
    }
}
