package com.template

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.template.retrofit.UrlAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection


class LoadingActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var preferences1: SharedPreferences
    private lateinit var db: DatabaseReference
    private lateinit var link: String
    private lateinit var link2: String
    private lateinit var userAgent : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading2)

        askNotificationPermission()

        db = Firebase.database.reference

        preferences = getSharedPreferences("linkFirebase", MODE_PRIVATE)
        preferences1 = getSharedPreferences("requestWasSend", MODE_PRIVATE)

        userAgent = WebView(this@LoadingActivity).settings.userAgentString
        val timeZone = TimeZone.getDefault().toString().substringAfter("id=\"").substringBefore('"')



        if(!isConnected) {
            Toast.makeText(this@LoadingActivity, "Check Internet connection", Toast.LENGTH_SHORT).show()
            finish()
        }
        if(preferences1.getBoolean("requestWasSend", false)==false){
            CoroutineScope(Dispatchers.IO).launch {
                getDataFromDb()
            }

           if (preferences.getString("linkFirebase", "link").toString().isEmpty()) finish()
       }
        Toast.makeText(this@LoadingActivity, preferences.getString("linkFirebase", "link").toString(), Toast.LENGTH_SHORT).show()

        link = preferences.getString("linkFirebase", "link").toString() +
                "/?"+
                "packageid=$packageName" +
                "&usserid=${UUID.randomUUID()}" +
                "&getz=$timeZone" +
                "&getr=utm_source=google-play&utm_medium=organic"



        httpscon()
        Thread.sleep(1000)
        Toast.makeText(this@LoadingActivity, link2, Toast.LENGTH_SHORT).show()

//        val build  = AlertDialog.Builder(this@LoadingActivity)
//        build.setMessage(link2).show()




    }

    // Проверяем интернет
    val Context.isConnected: Boolean
        get() {
            return (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetworkInfo?.isConnected == true
        }

    // Запрос разрешения на показ уведомлений
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Thanks!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, ":(", Toast.LENGTH_SHORT).show()
        }
    }

    // Запрос разрешения на показ уведомлений
    private fun askNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            )
            else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    //Чтение из БД
     fun getDataFromDb(){

        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {


                    val editPref: SharedPreferences.Editor = preferences.edit()
                    val editPref1: SharedPreferences.Editor = preferences1.edit()
                    editPref.putString("linkFirebase", dataSnapshot.child("db").child("link").value.toString())
                    editPref1.putBoolean("requestWasSend", true)
                    editPref.commit()
                    editPref1.commit()




                //Toast.makeText(this@LoadingActivity, preferences.getString("linkFirebase", "link").toString(), Toast.LENGTH_SHORT).show()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@LoadingActivity, databaseError.toException().toString(), Toast.LENGTH_SHORT).show()
            }
        }
        db.addValueEventListener(postListener)

    }

    fun retrofit(link1: String) : String{
        var str = ""

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(link1)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val UrlAPI = retrofit.create(UrlAPI::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            val URL_2 = UrlAPI.getURL()
            runOnUiThread {
                str = URL_2.toString()
            }
        }
        return str
    }

    fun httpscon(){
        CoroutineScope(Dispatchers.IO).launch {
            val url = URL(link)
            val connection: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            val br = BufferedReader(InputStreamReader(connection.getInputStream()))

            val line: String =  br.readLine()

            link2=line
        }

    }
}