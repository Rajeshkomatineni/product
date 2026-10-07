package com.fooddeal.companion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import kotlin.concurrent.thread

class FoodDealAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val swiggy = "in.swiggy.android"
    private val zomato = "com.application.zomato"
    private var swiggyDone = false
    private var zomatoDone = false
    private var lastEvent = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityServiceInfo.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityServiceInfo.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityServiceInfo.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 250L
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        if (!CompanionState.active || event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != swiggy && pkg != zomato) return
        if (System.currentTimeMillis() - lastEvent < 1200) return
        lastEvent = System.currentTimeMillis()

        val root = rootInActiveWindow ?: return
        val visibleText = collectText(root)
        val prices = extractPrices(visibleText)

        if (pkg == swiggy && !swiggyDone) {
            if (prices.isNotEmpty()) {
                swiggyDone = true
                postObservation("Swiggy", prices.minOrNull()!!)
                handler.postDelayed({ launch(zomato) }, 2500)
            } else {
                trySearch(root)
            }
        } else if (pkg == zomato && !zomatoDone) {
            if (prices.isNotEmpty()) {
                zomatoDone = true
                postObservation("Zomato", prices.minOrNull()!!)
                handler.postDelayed({ completeJob() }, 1200)
            } else {
                trySearch(root)
            }
        }
    }

    private fun trySearch(root: AccessibilityNodeInfo) {
        val fields = mutableListOf<AccessibilityNodeInfo>()
        collectEditTexts(root, fields)
        val field = fields.firstOrNull() ?: return
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                CompanionState.query
            )
        }
        field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        handler.postDelayed({
            root.findAccessibilityNodeInfosByText("Search")
                .firstOrNull { it.isClickable }
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }, 500)
    }

    private fun launch(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    private fun collectText(node: AccessibilityNodeInfo): String {
        val out = StringBuilder()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            n.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let {
                out.append(it).append('\n')
            }
            for (i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(node)
        return out.toString()
    }

    private fun collectEditTexts(node: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.className?.toString() == "android.widget.EditText") out.add(node)
        for (i in 0 until node.childCount) collectEditTexts(node.getChild(i), out)
    }

    private fun extractPrices(text: String): List<Double> {
        val matcher = Pattern.compile(
            """(?:₹|Rs\.?|INR)\s*([0-9]{1,5}(?:[,.][0-9]{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        ).matcher(text)
        val values = mutableListOf<Double>()
        while (matcher.find()) {
            val value = matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: continue
            if (value in 20.0..10000.0) values.add(value)
        }
        return values.distinct().sorted()
    }

    private fun postObservation(platform: String, price: Double) {
        thread {
            val body = String.format(
                """{"jobId":"%s","platform":"%s","item":"%s","finalPrice":%.2f,"foodPrice":%.2f,"restaurant":"Observed from visible UI"}""",
                escapeJson(CompanionState.jobId),
                escapeJson(platform),
                escapeJson(CompanionState.query),
                price,
                price
            )
            postJson(CompanionState.serverUrl + "/api/observations", body)
        }
    }

    private fun completeJob() {
        thread {
            postJson(CompanionState.serverUrl + "/api/jobs/" + escapeJson(CompanionState.jobId) + "/complete", "{}")
            CompanionState.active = false
        }
    }

    private fun postJson(url: String, body: String) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            connection.inputStream.use { it.readBytes() }
            connection.disconnect()
        } catch (_: Exception) {}
    }

    private fun escapeJson(value: String) =
        value.replace("\\", "\\\\").replace(""", "\"")

    override fun onInterrupt() {}
}
