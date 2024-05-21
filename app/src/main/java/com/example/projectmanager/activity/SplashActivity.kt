package com.example.projectmanager.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.example.projectmanager.R
import com.example.projectmanager.firebase.FireStoreClass

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val splashTV = findViewById<TextView>(R.id.splashTV)

        val typeface: Typeface = Typeface.createFromAsset(assets, "cassandra.ttf")
        splashTV.typeface = typeface

        Handler().postDelayed({
            val currentUserID = FireStoreClass().getCurrentUserid()
            if (currentUserID.isNotEmpty()) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
            else {
                startActivity(Intent(this@SplashActivity, IntroActivity::class.java))
            }
            finish()
        }, 2500)
    }
}