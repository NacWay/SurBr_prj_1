package com.template

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAnalytics = Firebase.analytics


        preferences = getSharedPreferences("isEmptyLink", MODE_PRIVATE)

        val arguments = intent.extras
        val res = arguments!!.get("isEmptyLink").toString()

        val editPref: SharedPreferences.Editor = preferences.edit()
        editPref.putString("name", res)
        editPref.commit()


        if (res != "true") {
            if (isConnected)
                startActivity(Intent(this, LoadingActivity::class.java))
        }
    }

    // Проверяем интернет
    val Context.isConnected: Boolean
        get() {
            return (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetworkInfo?.isConnected == true
        }



}