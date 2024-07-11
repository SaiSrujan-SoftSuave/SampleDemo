package com.example.geofencinginitialdemo

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.geofencinginitialdemo.Utlis.CUSTOM_INTENT_GEOFENCE
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SampleScreen(modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationInfo by remember {
        mutableStateOf<LocationInfo?>(null)
    }
    var currentLocation by remember {
        mutableStateOf("")
    }

    var geofenceLocation by remember {
        mutableStateOf("")
    }
    val geofenceManager = remember { GeofenceManager(context) }
    var geofenceTransitionEventInfo by remember {
        mutableStateOf("")
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            scope.launch(Dispatchers.IO) {
                geofenceManager.deregisterGeofence()
            }
        }
    }

    // Register a local broadcast to receive activity transition updates
    GeofenceBroadcastReceiver(systemAction = CUSTOM_INTENT_GEOFENCE) { event ->
        geofenceTransitionEventInfo = event
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Text("Current Location", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = {
                scope.launch {
                    val res = async {
                        getCurrentLocation(context)
                    }
                    locationInfo = res.await()
                    Toast.makeText(
                        context,
                        "Fetch Location ${locationInfo?.address}",
                        Toast.LENGTH_SHORT
                    ).show()
                    currentLocation =
                        "Latitude: ${locationInfo?.latitude} Longitude: ${locationInfo?.longitude} \n\nAddress: ${locationInfo?.address}"
                    if(geofenceLocation.isEmpty())geofenceLocation = currentLocation
                }
            }) {
                Text(text = "Fetch Location")
            }
        }

        Text(currentLocation, modifier = Modifier.padding(16.dp))


            Text("Geofence Location", style = MaterialTheme.typography.headlineSmall)
        Row(
            modifier = Modifier.fillMaxWidth() ,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                geofenceManager.addGeofence(
                    key = "TestGeofence",
                    location = Location("").apply {
                        latitude = locationInfo?.latitude!!
                        longitude = locationInfo?.longitude!!
                    })
                Toast.makeText(context, "Created Geofence", Toast.LENGTH_SHORT).show()
            }) {
                Text(text = "Create Geofence")
            }

            Button(onClick = {
                if(geofenceManager.geofenceList.isNotEmpty())geofenceManager.registerGeofence()
                Toast.makeText(context, "Add Geofence", Toast.LENGTH_SHORT).show()
            }) {
                Text(text = "Add Geofence")
            }
        }
        Text(geofenceLocation, modifier = Modifier.padding(16.dp))
        Text(geofenceTransitionEventInfo, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SampleScreenPreview(modifier: Modifier = Modifier) {
    SampleScreen()
}


suspend fun getCurrentLocation(context: Context): LocationInfo? {
    var locationInfo: LocationInfo? = null

    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return null
    }

    withContext(Dispatchers.IO) {
        val loc = await(fusedLocationProviderClient.lastLocation)
        locationInfo = LocationInfo(
            latitude = loc.latitude,
            longitude = loc.longitude,
            address = getPlaceName(context, loc.latitude, loc.longitude)
        )
    }

    return locationInfo
}

fun getPlaceName(
    context: Context,
    latitude: Double,
    longitude: Double,
): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
    if (!addresses.isNullOrEmpty()) {
        val address = addresses[0]
        return address.getAddressLine(0)
    } else {
        return "Unknown location"
    }
}

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String
)



@Composable
fun GeofenceBroadcastReceiver(
    systemAction: String,
    systemEvent: (userActivity: String) -> Unit,
) {
    val TAG = "GeofenceReceiver"
    val context = LocalContext.current
    val currentSystemOnEvent by rememberUpdatedState(systemEvent)

    DisposableEffect(context, systemAction) {
        val intentFilter = IntentFilter(systemAction)
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) } ?: return

                if (geofencingEvent.hasError()) {
                    val errorMessage =
                        GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
                    Log.e(TAG, "onReceive: $errorMessage")
                    return
                }
                val alertString = "Geofence Alert :" +
                        " Trigger ${geofencingEvent.triggeringGeofences}" +
                        " Transition ${geofencingEvent.geofenceTransition}"
                Log.d(
                    TAG,
                    alertString
                )
                currentSystemOnEvent(alertString)
            }
        }
        context.registerReceiver(broadcast, intentFilter)
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
}
