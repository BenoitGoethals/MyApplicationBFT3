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
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpPost
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.util.*

private const val PERMISSION_REQUEST = 10

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager:LocationManager
    private var job:Job? = null
    private lateinit var  intentLOcal: Intent

    private var permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        FuelManager.instance.basePath = "http://demosmushtaq.16mb.com";
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
         //       getLocation()
            } else {
                disableView()

                Toast.makeText(this,"Turned Off",Toast.LENGTH_LONG).show()

            }
        })



    }


    private fun disableView() {
      //  btn_get_location.isEnabled = false
      //  btn_get_location.alpha = 0.5F
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



    private fun enableView() {
      //  btn_get_location.isEnabled = true
        //btn_get_location.alpha = 1F
      //  btn_get_location.setOnClickListener { getLocation()}
        job = GlobalScope.launch (Dispatchers.Main) {

            tv_result.text="Running "+getDeviceName()
            while (!job!!.isCancelled){
               var loc = getLocation()
             //   GlobalScope.launch(){

                    try {

                        Fuel.post("api/post_sample.php", listOf("latitude" to loc?.latitude.toString(), "longitude" to loc?.longitude.toString(),"altitude" to loc?.altitude.toString(),"time" to loc?.time.toString(), "id" to getDeviceName() )).responseJson { request, response, result ->
                            tv_result.text=result.toString()
                        }
                    } catch (e: Exception) {

                    } finally {

                    }


         //       khttp.post(
           //         url = "http://httpbin.org/post",
             //       json = mapOf("latitude" to loc?.latitude.toString(), "longitude" to loc?.longitude.toString(),"altitude" to loc?.altitude.toString(),"time" to loc?.time.toString(), "id" to getDeviceName() ))
                delay(1000)

               //     tv_result.text=loc.toString()
                    txt_latitude.text= loc?.latitude.toString()
                    txt_longitude.text= loc?.longitude.toString()
                    textViewAlt.text=loc?.altitude.toString()
                    textViewDT.text=loc?.time.toString()
        //    }

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

        Log.d("CodeAndroidLocation", provider)
        locationManager.requestLocationUpdates(provider, 5000, 0F, object :
            LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null) {
                    locationPos = location
                    Log.d("CodeAndroidLocation", provider + "  Latitude : " + locationPos!!.latitude)
                    Log.d("CodeAndroidLocation", provider + "  Longitude : " + locationPos!!.longitude)
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
    private suspend fun getLocation(): Location? {
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
                Log.d("CodeAndroidLocation", " Network Latitude : " + locationNetwork!!.latitude)
                Log.d("CodeAndroidLocation", " Network Longitude : " + locationNetwork!!.longitude)
                location=locationNetwork
            }else{
                Log.d("CodeAndroidLocation", " GPS Latitude : " + locationGps!!.latitude)
                Log.d("CodeAndroidLocation", " GPS Longitude : " + locationGps!!.longitude)
                location=locationGps
            }

        }
        else
        {
            Log.d("CodeAndroidLocation", "  Latitude : " + locationGps!!.latitude)
            Log.d("CodeAndroidLocation", "  Longitude : " + locationGps!!.longitude)
        }


        return location
    }

}
