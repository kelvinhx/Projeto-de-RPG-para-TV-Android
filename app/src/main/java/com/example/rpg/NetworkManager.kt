package com.example.rpg

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

data class NetworkDiagnosticInfo(
    val isOnline: Boolean = false,
    val connectionType: String = "Nenhuma",
    val pingMs: Long = -1L,
    val dnsOk: Boolean = false,
    val localIp: String = "Desconhecido",
    val isTesting: Boolean = false
)

class NetworkManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkInfo = MutableStateFlow(NetworkDiagnosticInfo())
    val networkInfo: StateFlow<NetworkDiagnosticInfo> = _networkInfo.asStateFlow()

    init {
        registerNetworkCallback()
        CoroutineScope(Dispatchers.IO).launch {
            runFullDiagnostic()
        }
    }

    private fun registerNetworkCallback() {
        try {
            val builder = android.net.NetworkRequest.Builder()
            connectivityManager.registerNetworkCallback(
                builder.build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        Log.d("NetworkManager", "Network available.")
                        updateOnlineStatus()
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        Log.d("NetworkManager", "Network lost.")
                        updateOnlineStatus()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        super.onCapabilitiesChanged(network, networkCapabilities)
                        updateOnlineStatus()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("NetworkManager", "Failed to register network callback: ${e.message}", e)
        }
    }

    private fun updateOnlineStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val type = when {
            capabilities == null -> "Nenhuma"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Cabo (Ethernet)"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Dados Móveis"
            else -> "Outra"
        }

        val ip = getLocalIpAddress()

        _networkInfo.value = _networkInfo.value.copy(
            isOnline = hasInternet,
            connectionType = type,
            localIp = ip
        )
    }

    suspend fun runFullDiagnostic(): NetworkDiagnosticInfo = withContext(Dispatchers.IO) {
        _networkInfo.value = _networkInfo.value.copy(isTesting = true)
        
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        var hasDns = false
        var latency = -1L
        
        val startDns = System.currentTimeMillis()
        try {
            val address = InetAddress.getByName("generativelanguage.googleapis.com")
            hasDns = address.hostAddress.isNotEmpty()
            latency = System.currentTimeMillis() - startDns
        } catch (e: Exception) {
            Log.e("NetworkManager", "DNS resolution failed: ${e.message}")
        }

        var isEndpointReachable = false
        if (hasDns) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=${BuildConfig.GEMINI_API_KEY}")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.requestMethod = "GET"
                val code = connection.responseCode
                isEndpointReachable = code == 200 || code == 400 || code == 403 || code == 401
            } catch (e: Exception) {
                Log.e("NetworkManager", "API Endpoint check failed: ${e.message}")
            }
        }

        val type = when {
            capabilities == null -> "Nenhuma"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Cabo (Ethernet)"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Dados Móveis"
            else -> "Outra"
        }

        val updated = NetworkDiagnosticInfo(
            isOnline = isEndpointReachable || (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)),
            connectionType = type,
            pingMs = latency,
            dnsOk = hasDns,
            localIp = getLocalIpAddress(),
            isTesting = false
        )
        
        _networkInfo.value = updated
        updated
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("NetworkManager", "Error getting local IP address: ${ex.message}")
        }
        return "Desconhecido"
    }
}
