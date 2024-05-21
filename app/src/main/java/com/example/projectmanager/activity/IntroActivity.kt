package com.example.projectmanager.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.example.projectmanager.R

class IntroActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_intro)

        val introName = findViewById<TextView?>(R.id.intro_name)
        val btnSignUp = findViewById<Button?>(R.id.btn_signup_intro)
        val btnSignIn = findViewById<Button?>(R.id.btn_signin_intro)

        val typeface: Typeface = Typeface.createFromAsset(assets, "cassandra.ttf")
        introName.typeface = typeface

        btnSignUp.setOnClickListener {
            startActivity(Intent(this@IntroActivity, SignUpActivity::class.java))
        }

        btnSignIn.setOnClickListener {
            startActivity(Intent(this@IntroActivity, SignInActivity::class.java))
        }
    }
}