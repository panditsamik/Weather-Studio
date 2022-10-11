package com.example.weatherstudio

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherstudio.models.WeatherResponse
import com.example.weatherstudio.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {


    private lateinit var mFusedLocationClient : FusedLocationProviderClient
    private var mProgressDialog : Dialog ? = null


    var tv_main = findViewById<TextView>(R.id.tv_main)
    var tv_main_description = findViewById<TextView>(R.id.tv_main_description)
    var tv_temp = findViewById<TextView>(R.id.tv_temp)
    var tv_sunrise_time = findViewById<TextView>(R.id.tv_sunrise_time)
    var tv_sunset_time = findViewById<TextView>(R.id.tv_sunset_time)
    var tv_humidity = findViewById<TextView>(R.id.tv_humidity)
    var tv_min = findViewById<TextView>(R.id.tv_min)
    var tv_max = findViewById<TextView>(R.id.tv_max)
    var tv_speed = findViewById<TextView>(R.id.tv_speed)
    var tv_name = findViewById<TextView>(R.id.tv_name)
    var tv_country = findViewById<TextView>(R.id.tv_country)
    var iv_main = findViewById<ImageView>(R.id.iv_main)
    var iv_humidity = findViewById<ImageView>(R.id.iv_humidity)
    var iv_min_max = findViewById<ImageView>(R.id.iv_min_max)
    var iv_wind = findViewById<ImageView>(R.id.iv_wind)
    var iv_location = findViewById<ImageView>(R.id.iv_location)
    var iv_sunrise = findViewById<ImageView>(R.id.iv_sunrise)
    var iv_sunset = findViewById<ImageView>(R.id.iv_sunset)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()){
            Toast.makeText(this,"Your location is turned off.Please turn it on.",Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else{
            Dexter.withContext(this@MainActivity)
                .withPermissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                    ,Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object: MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report != null) {
                            if (report.areAllPermissionsGranted()){
                                requestLocationData()
                            }
                        }
                        if (report != null) {
                            if (report.isAnyPermissionPermanentlyDenied){
                                Toast.makeText(this@MainActivity,"You have denied location permission.",Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogforPermission()
                    }
                }).onSameThread().check()
        }
    }

    private fun getLOcationWeatherDetails(latitude : Double,longitude : Double) {
        if (Constants.isNetworkAvailable(this)){
            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service : WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)
            val listCall : Call<WeatherResponse> = service.getWeather(
                latitude,longitude,Constants.METRIC_UNIT,Constants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful){

                        hideProgressDialog()

                        val weatherList : WeatherResponse? = response.body()

                        if (weatherList != null) {
                            setupUI(weatherList)
                        }

                        Log.i("Response Result","$weatherList")
                    }
                    else{
                        val rc = response.code()
                        when(rc){
                            400 ->{
                                Log.e("Error 400","Bad Connection")
                            }
                            404 ->{
                                Log.e("Error 404","Not Found")
                            }
                            else -> {
                                Log.e("Error","Generic Error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideProgressDialog()
                    Log.e("Error", t.message.toString())
                }
            })
        }
        else{
            Toast.makeText(this,"No Internet Connection Available",Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation

            val latitude = mLastLocation!!.latitude
            Log.i("Current Latitude","$latitude")

            val longitude = mLastLocation!!.longitude
            Log.i("Current Longitude","$longitude")
            getLOcationWeatherDetails(latitude,longitude)
        }
    }

    private fun showRationalDialogforPermission() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions.You need to go to application settings.")
            .setPositiveButton("GO TO SETTINGS"){
                _,_ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data = uri
                    startActivity(intent)
                }
                catch (e : ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){
                dialog,_ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun isLocationEnabled() : Boolean {
        // This provides access to the system location service
        val locationmanager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationmanager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh -> {
                requestLocationData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun hideProgressDialog(){
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    private fun setupUI(weatherList : WeatherResponse){
        for (i in weatherList.weather.indices){
            Log.i("Weather Name",weatherList.weather[i].main)

            tv_main.setText(weatherList.weather[i].main)
            tv_main_description.setText(weatherList.weather[i].description)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tv_temp.setText(weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString()))
            }

            tv_humidity.setText(weatherList.main.humidity.toString() + "per cent")
            tv_min.setText(weatherList.main.temp_min.toString() + "min")
            tv_max.setText(weatherList.main.temp_max.toString() + "max")
            tv_speed.setText(weatherList.wind.speed.toString())
            tv_name.setText(weatherList.name)
            tv_country.setText(weatherList.sys.country)

            tv_sunrise_time.setText(unixTime(weatherList.sys.sunrise))
            tv_sunset_time.setText(unixTime(weatherList.sys.sunset))

            iv_humidity.setImageResource(R.drawable.humidity1)
            iv_min_max.setImageResource(R.drawable.temperature1)
            iv_wind.setImageResource(R.drawable.wind1)
            iv_location.setImageResource(R.drawable.location1)
            iv_sunrise.setImageResource(R.drawable.sunrise1)
            iv_sunset.setImageResource(R.drawable.sunset)

            when(weatherList.weather[i].icon){

                "01d" -> iv_main.setImageResource(R.drawable.sunny1)
                "02d" -> iv_main.setImageResource(R.drawable.cloud1)
                "03d" -> iv_main.setImageResource(R.drawable.cloud1)
                "04d" -> iv_main.setImageResource(R.drawable.cloud1)
                "04n" -> iv_main.setImageResource(R.drawable.cloud1)
                "10d" -> iv_main.setImageResource(R.drawable.rain1)
                "11d" -> iv_main.setImageResource(R.drawable.storm1)
                "13d" -> iv_main.setImageResource(R.drawable.snowflake1)
                "01n" -> iv_main.setImageResource(R.drawable.cloud1)
                "02n" -> iv_main.setImageResource(R.drawable.cloud1)
                "03n" -> iv_main.setImageResource(R.drawable.cloud1)
                "10n" -> iv_main.setImageResource(R.drawable.cloud1)
                "11n" -> iv_main.setImageResource(R.drawable.rain1)
                "13n" -> iv_main.setImageResource(R.drawable.snowflake1)

            }
        }
    }

    private fun getUnit(value : String) : String{
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex : Long) : String{
        val date = Date(timex *1000L) // Timestamp is in milliseconds
        val sdf = SimpleDateFormat("HH:mm", Locale.UK)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}
