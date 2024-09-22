package com.fakhry.rnd.bluetooth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fakhry.rnd.bluetooth.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListener()
    }

    private fun initListener() {
        binding.btnToWebView.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            startActivity(intent)
        }
    }
}