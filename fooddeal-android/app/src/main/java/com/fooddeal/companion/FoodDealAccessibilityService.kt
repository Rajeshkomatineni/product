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
    private val handler=Handler(Looper.getMainLooper())
    private val swiggy="in.swiggy.android"
    private val zomato="com.application.zomato"
    private var swiggyDone=false
    private var zomatoDone=false
    private var lastEvent=0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo=serviceInfo.apply {
            eventTypes=AccessibilityServiceInfo.DEFAULT
            feedbackType=AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event:android.view.accessibility.AccessibilityEvent?) {
        if(!CompanionState.active || event==null) return
        val pkg=event.packageName?.toString() ?: return
        if(pkg!=swiggy && pkg!=zomato) return
        if(System.currentTimeMillis()-lastEvent<1200) return
        lastEvent=System.currentTimeMillis()
        val root=rootInActiveWindow ?: return
        val prices=extractPrices(collectText(root))
        if(pkg==swiggy && !swiggyDone) {
            if(prices.isNotEmpty()) {
                swiggyDone=true
                postObservation("Swiggy",prices.minOrNull()!!)
                handler.postDelayed({ launch(zomato) },2500)
            } else trySearch(root)
        } else if(pkg==zomato && !zomatoDone) {
            if(prices.isNotEmpty()) {
                zomatoDone=true
                postObservation("Zomato",prices.minOrNull()!!)
                handler.postDelayed({ completeJob() },1200)
            } else trySearch(root)
        }
    }

    private fun trySearch(root:AccessibilityNodeInfo) {
        val fields=mutableListOf<AccessibilityNodeInfo>()
        collectEditTexts(root,fields)
        val field=fields.firstOrNull() ?: return
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args=Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,CompanionState.query)
        }
        field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args)
        handler.postDelayed({ field.performAction(AccessibilityNodeInfo.ACTION_IME_ENTER) },400)
    }

    private fun launch(pkg:String) { packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) } }

    private fun collectText(node:AccessibilityNodeInfo):String {
        val out=StringBuilder()
        fun walk(n:AccessibilityNodeInfo?) {
            if(n==null) return
            n.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { out.append(it).append('\n') }
            for(i in 0 until n.childCount) walk(n.getChild(i))
        }
        walk(node)
        return out.toString()
    }

    private fun collectEditTexts(node:AccessibilityNodeInfo?,out:MutableList<AccessibilityNodeInfo>) {
        if(node==null) return
        if(node.className?.toString()=="android.widget.EditText") out.add(node)
        for(i in 0 until node.childCount) collectEditTexts(node.getChild(i),out)
    }

    private fun extractPrices(text:String):List<Double> {
        val p=Pattern.compile("""(?:₹|Rs\\.?|INR)\\s*([0-9]{1,5}(?:[,.][0-9]{1,2})?)""",Pattern.CASE_INSENSITIVE)
        val m=p.matcher(text)
        val result=mutableListOf<Double>()
        while(m.find()) {
            val v=m.group(1)?.replace(",","")?.toDoubleOrNull() ?: continue
            if(v in 20.0..10000.0) result.add(v)
        }
        return result.distinct().sorted()
    }

    private fun postObservation(platform:String,price:Double) {
        thread {
            val body=String.format(
                """{"jobId":"%s","platform":"%s","item":"%s","finalPrice":%.2f,"foodPrice":%.2f,"restaurant":"Observed from visible UI"}""",
                esc(CompanionState.jobId),esc(platform),esc(CompanionState.query),price,price
            )
            post(CompanionState.serverUrl+"/api/observations",body)
        }
    }

    private fun completeJob() {
        thread {
            post(CompanionState.serverUrl+"/api/jobs/"+esc(CompanionState.jobId)+"/complete","{}")
            CompanionState.active=false
        }
    }

    private fun post(url:String,body:String) {
        try {
            val c=URL(url).openConnection() as HttpURLConnection
            c.requestMethod="POST"; c.connectTimeout=10000; c.readTimeout=10000; c.doOutput=true
            c.setRequestProperty("Content-Type","application/json")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            c.inputStream.use { it.readBytes() }
            c.disconnect()
        } catch(_:Exception) {}
    }

    private fun esc(s:String)=s.replace("\\","\\\\").replace(""","\\"")
    override fun onInterrupt() {}
}