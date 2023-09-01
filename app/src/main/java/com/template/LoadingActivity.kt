package com.template

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase



class LoadingActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var link: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading2)

        askNotificationPermission()

        db = Firebase.database.reference

        getDataFromDb()



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

                link=dataSnapshot.child("db").child("link").value.toString()
                if (link.isNotEmpty()){
                    intent.putExtra("isEmptyLink", "true")
                    startActivity(intent)
                    finish()
                }
                /*
                link.observe(this@LoadingActivity, Observer { newValue ->
                    dataSnapshot.child("db").child("link").value.toString()
                    if (link.value!!.isEmpty()) finish()
                })

                 */
                Toast.makeText(this@LoadingActivity, dataSnapshot.child("db").child("link").value.toString(), Toast.LENGTH_SHORT).show()

            }
            override fun onCancelled(databaseError: DatabaseError) {

                Toast.makeText(this@LoadingActivity, databaseError.toException().toString(), Toast.LENGTH_SHORT).show()
            }
        }
        db.addValueEventListener(postListener)
    }
}