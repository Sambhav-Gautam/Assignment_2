package com.example.assignment_2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
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
    var flightInfo by remember { mutableStateOf("Enter flight number and track") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = flightNumber,
            onValueChange = { flightNumber = it },
            label = { Text("Flight Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (flightNumber.isNotEmpty()) {
                    scope.launch {
                        flightInfo = fetchFlightData(flightNumber)
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

suspend fun fetchFlightData(flightNumber: String): String {
    val apiKey = "bf81ba651f87b9118d67fa562155951b" // Store securely in local.properties
    val url = "https://api.aviationstack.com/v1/flights?access_key=$apiKey&flight_iata=$flightNumber"
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    return withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext "Invalid flight data"

            if (!response.isSuccessful) return@withContext "API error: ${response.code}"

            val jsonObject = JSONObject(responseBody)
            val dataArray = jsonObject.optJSONArray("data") ?: return@withContext "No flight data available"

            if (dataArray.length() == 0) return@withContext "No flight data available"

            val flight = dataArray.getJSONObject(0)

            // Extracting detailed information
            val flightStatus = flight.optString("flight_status", "Unknown")
            val airline = flight.optJSONObject("airline")?.optString("name", "Unknown") ?: "Unknown"
            val flightIATA = flight.optString("flight_iata", "Unknown")

            val departure = flight.optJSONObject("departure")
            val depAirport = departure?.optString("airport", "Unknown") ?: "Unknown"
            val depTime = departure?.optString("estimated", "Unknown") ?: "Unknown"
            val depTerminal = departure?.optString("terminal", "Unknown") ?: "Unknown"
            val depGate = departure?.optString("gate", "Unknown") ?: "Unknown"
            val depDelay = departure?.optString("delay", "0") ?: "0"

            val arrival = flight.optJSONObject("arrival")
            val arrAirport = arrival?.optString("airport", "Unknown") ?: "Unknown"
            val arrTime = arrival?.optString("estimated", "Unknown") ?: "Unknown"
            val arrTerminal = arrival?.optString("terminal", "Unknown") ?: "Unknown"
            val arrGate = arrival?.optString("gate", "Unknown") ?: "Unknown"
            val arrBaggage = arrival?.optString("baggage", "Unknown") ?: "Unknown"

            // Live Tracking Data
            val liveData = flight.optJSONObject("live")
            val altitude = liveData?.optString("altitude", "Unknown") ?: "Unknown"
            val latitude = liveData?.optString("latitude", "Unknown") ?: "Unknown"
            val longitude = liveData?.optString("longitude", "Unknown") ?: "Unknown"
            val speed = liveData?.optString("speed_horizontal", "Unknown") ?: "Unknown"
            val verticalSpeed = liveData?.optString("speed_vertical", "Unknown") ?: "Unknown"
            val heading = liveData?.optString("direction", "Unknown") ?: "Unknown"

            // Formatting Output
            """
            ✈️ Flight Information:
            ───────────────────────────
            📌 Flight: $airline ($flightIATA)
            🚦 Status: $flightStatus
            
            🛫 Departure:
            📍 Airport: $depAirport
            ⏳ Estimated Time: $depTime
            🏢 Terminal: $depTerminal | 🚪 Gate: $depGate
            ⏳ Delay: ${if (depDelay != "0") "$depDelay minutes" else "No delay"}
            
            🛬 Arrival:
            📍 Airport: $arrAirport
            ⏳ Estimated Time: $arrTime
            🏢 Terminal: $arrTerminal | 🚪 Gate: $arrGate
            🎒 Baggage Claim: $arrBaggage
            
            🌍 Live Flight Tracking:
            ───────────────────────────
            📍 Latitude: $latitude | 📍 Longitude: $longitude
            📏 Altitude: ${altitude}m
            🚀 Speed: ${speed} km/h | 🛗 Vertical Speed: ${verticalSpeed} m/s
            🧭 Heading: $heading°
            """.trimIndent()
        } catch (e: Exception) {
            "Error fetching flight data"
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlightTrackerApp()
}
