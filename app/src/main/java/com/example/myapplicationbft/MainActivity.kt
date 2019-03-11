package com.example.myapplicationbft


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import android.widget.Toast
import com.github.kittinunf.fuel.core.FuelManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*

import android.os.Handler
import android.os.Message
import me.piruin.geok.Datum
import me.piruin.geok.LatLng
import java.text.SimpleDateFormat


private const val PERMISSION_REQUEST = 10

class MainActivity : AppCompatActivity() {


    private lateinit var rabService:RabbitService


    private lateinit var locationManager:LocationManager
    private var job:Job? = null
    private lateinit var  intentLOcal: Intent

    private var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)

        rabService= RabbitService()


        val incomingMessageHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                val message = msg.getData().getString("msg")
               // val tv = findViewById<View>(R.id.textView) as TextView
                val now = Date()
                val ft = SimpleDateFormat("hh:mm:ss")
               // tv.append(ft.format(now) + ' '.toString() + message + '\n'.toString())
            }
        }
       // rabService.subscribe(incomingMessageHandler)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        toggleButtonStartStop.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                Toast.makeText(this,"Turned On",Toast.LENGTH_LONG).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkPermission(permissions)) {
                        enableView()
                    } else {
                        requestPermissions(permissions, PERMISSION_REQUEST)
                    }
                } else {
                    enableView()
                }
            } else {
                disableView()

                Toast.makeText(this,"Turned Off",Toast.LENGTH_LONG).show()

            }
        })



    }


    private fun disableView() {

        job!!.cancel()
        tv_result.text="Stopped"

        Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show()
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else {
            capitalize(manufacturer) + " " + model
        }
    }


    private fun capitalize(s: String?): String {
        if (s == null || s.length == 0) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first) + s.substring(1)
        }
    }



    private  var int:Int=0
    private fun enableView() {

        job = GlobalScope.launch (Dispatchers.Main) {

            tv_result.text="Running "+getDeviceName()
            while (!job!!.isCancelled){
               var loc = getLocation()

                ++int
               var utm=  LatLng(loc?.longitude!!.toDouble(),loc?.latitude.toDouble(),loc?.alt.toDouble()).toUtm(Datum.WSG48)
                        rabService.publishToAMQP(loc)
                tv_result.text= "Send $int ${utm.toString()}"

                delay(500)


                    txt_latitude.text= loc?.latitude
                    txt_longitude.text= loc?.longitude
                    textViewAlt.text=loc?.alt
                    textViewDT.text=loc?.date


            }
        }




        Toast.makeText(this, "Running", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermission(permissionArray: Array<String>): Boolean {
        var allSuccess = true
        for (i in permissionArray.indices) {
            if (checkCallingOrSelfPermission(permissionArray[i]) == PackageManager.PERMISSION_DENIED)
                allSuccess = false
        }
        return allSuccess
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) {
            var allSuccess = true
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    allSuccess = false
                    val requestAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(permissions[i])
                    if (requestAgain) {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Go to settings and enable the permission", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            if (allSuccess)
                enableView()

        }
    }





    @SuppressLint("MissingPermission")
    private fun getPosition(provider:String): Location? {
        var locationPos: Location? = null
        locationManager.requestLocationUpdates(provider, 5000, 0F, object :
            LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null) {
                    locationPos = location
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }
            override fun onProviderEnabled(provider: String?) {

            }
            override fun onProviderDisabled(provider: String?) {

            }

        }

        )

        val localGpsLocation = locationManager.getLastKnownLocation(provider)
        if (localGpsLocation != null)
            return localGpsLocation
        else
            return locationPos
    }


    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): LocationBft? {
        var locationGps: Location? = null
        var locationNetwork: Location? = null
        var location:Location?=null
        var   hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        var  hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (hasGps) {
            locationGps=getPosition(LocationManager.GPS_PROVIDER)
            location=locationGps
        }
        if (hasNetwork) {
            locationNetwork=getPosition(LocationManager.NETWORK_PROVIDER)
            location=locationNetwork
        }

        if(locationGps!= null && locationNetwork!= null){
            if(locationGps!!.accuracy > locationNetwork!!.accuracy){

                location=locationNetwork
            }else{

                location=locationGps
            }

        }

        return LocationBft(location?.latitude.toString(),location?.longitude.toString(),location?.altitude.toString(),Date().toString(),getDeviceName())
    }

    override fun onDestroy() {
        super.onDestroy()
        rabService.onDestroy()

    }
}
