package com.example.SpendMeter

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.animation.AlphaAnimation
import androidx.appcompat.app.AppCompatActivity
import com.example.SpendMeter.databinding.ActivityWelcomeScreenBinding

class WelcomeScreen : AppCompatActivity() {
    private val binding: ActivityWelcomeScreenBinding by lazy {
        ActivityWelcomeScreenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // ✅ Fade-In Animation for Text
        val fadeInText = AlphaAnimation(0f, 1f)
        fadeInText.duration = 1500
        binding.WelcomeText.startAnimation(fadeInText)

        // ✅ Gradient Color Effect for "Welcome" Text
        val welcomeText = "Welcome"
        val spannableString = SpannableString(welcomeText)
        spannableString.setSpan(ForegroundColorSpan(Color.parseColor("#DA8181")), 0, 5, 0)
        spannableString.setSpan(
            ForegroundColorSpan(Color.parseColor("#8F8686")),
            5,
            welcomeText.length,
            0
        )
        binding.WelcomeText.text = spannableString

        // ✅ Auto Navigate to Login Screen
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, Login::class.java))
            finish()
        }, 3000) // 3 seconds delay
    }
}
