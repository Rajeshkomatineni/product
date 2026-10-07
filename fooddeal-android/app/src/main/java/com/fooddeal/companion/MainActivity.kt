package com.fooddeal.companion
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

object CompanionState {
    var serverUrl = "https://fooddeal-jai9qh.v2.appdeploy.ai"
    var jobId = ""
    var query = "Chicken Biryani"
    var active = false
}
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,40,32,32) }
        root.addView(TextView(this).apply { text="FoodDeal Companion"; textSize=28f })
        val server=EditText(this).apply { setText(CompanionState.serverUrl); setSingleLine() }
        val job=EditText(this).apply { hint="FoodDeal Job ID"; setSingleLine() }
        val query=EditText(this).apply { hint="Search query"; setText("Chicken Biryani"); setSingleLine() }
        root.addView(server); root.addView(job); root.addView(query)
        root.addView(Button(this).apply { text="Enable Accessibility"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
        root.addView(Button(this).apply {
            text="Start Comparison"
            setOnClickListener {
                val id=job.text.toString().trim()
                if(id.isEmpty()){ Toast.makeText(this@MainActivity,"Enter the FoodDeal Job ID.",Toast.LENGTH_LONG).show(); return@setOnClickListener }
                CompanionState.serverUrl=server.text.toString().trim().removeSuffix("/")
                CompanionState.jobId=id; CompanionState.query=query.text.toString().trim(); CompanionState.active=true
                packageManager.getLaunchIntentForPackage("in.swiggy.android")?.let { startActivity(it) }
                    ?: Toast.makeText(this@MainActivity,"Swiggy is not installed.",Toast.LENGTH_LONG).show()
            }
        })
        root.addView(Button(this).apply { text="Open FoodDeal"; setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CompanionState.serverUrl))) } })
        setContentView(root)
    }
}