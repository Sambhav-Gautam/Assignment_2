package com.example.assignment_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlightTrackerApp()
        }
    }
}

@Composable
fun FlightTrackerApp() {
    var flightNumber by remember { mutableStateOf("") }
    var flightInfo by remember { mutableStateOf("Enter a flight number (callsign) and press 'Track Flight'") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = flightNumber,
            onValueChange = { flightNumber = it },
            label = { Text("Flight Number (Callsign)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (flightNumber.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        val data = fetchOpenSkyFlightData(flightNumber)
                        // Update UI on the main thread
                        flightInfo = data ?: "Flight not found or API error"
                    }
                } else {
                    flightInfo = "Please enter a valid flight number"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Track Flight")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = flightInfo)
    }
}

fun fetchOpenSkyFlightData(flightNumber: String): String? {
    val url = "https://opensky-network.org/api/states/all"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    return try {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
        if (!response.isSuccessful || responseBody.isNullOrEmpty()) return "Invalid flight data"

        val jsonResponse = JSONObject(responseBody)
        val statesArray = jsonResponse.getJSONArray("states")

        // Iterate over state vectors and look for a callsign that matches the flight number.
        for (i in 0 until statesArray.length()) {
            val state = statesArray.getJSONArray(i)
            val callsign = state.optString(1)?.trim()?.uppercase() ?: ""
            if (callsign.contains(flightNumber.trim().uppercase())) {
                // Extract relevant flight data
                val originCountry = state.optString(2, "N/A")
                val longitude = state.optDouble(5, Double.NaN)
                val latitude = state.optDouble(6, Double.NaN)
                val altitude = state.optDouble(7, Double.NaN)
                val velocity = state.optDouble(9, Double.NaN)
                val onGround = state.optBoolean(8, false)

                return """
                    Flight: $callsign
                    Origin Country: $originCountry
                    Location: Lat $latitude, Lon $longitude
                    Altitude: ${if (altitude.isNaN()) "N/A" else altitude}
                    Velocity: ${if (velocity.isNaN()) "N/A" else velocity}
                    On Ground: $onGround
                """.trimIndent()
            }
        }
        "No matching flight found for: $flightNumber"
    } catch (e: Exception) {
        "Error fetching flight data: ${e.localizedMessage}"
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlightTrackerApp()
}
