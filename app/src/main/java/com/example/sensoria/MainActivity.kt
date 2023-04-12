package com.example.sensoria

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject
import java.io.File
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {

    //private val PERMISSION_REQUEST_CODE = 200
    private lateinit var buttonAction: Button
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorListener: SensorEventListener
    private lateinit var countTime : TextView
    private lateinit var dataList: MutableList<Data>

    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //QUITA BARRA DE TITULO
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        supportActionBar!!.hide()
        //COMPRUEBA SI HAY INTERNET
        checkInternetConnection()

        // SE LLAMA LA FUNCION ACELEROMETRO LA CUAL HACE LA CHAMBA
        acelemetro()
    }

    fun isInternetAvailable(): Boolean {
        return try {
            val connectionManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectionManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        } catch (e: Exception) {
            false
        }
    }

    fun checkInternetConnection() {
        val isInternetAvailable = isInternetAvailable()
        val message = if (isInternetAvailable) "El dispositivo tiene acceso a Internet" else "El dispositivo no tiene acceso a Internet"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }



    private fun saveDataToFile() {
        val json = JSONObject()
        val dataJsonArray = dataList.map { it.toJson() }
        json.put("data", dataJsonArray)

        val fileName = "data.json"

        val file = File(filesDir, fileName)
        file.writeText(json.toString())
    }

    private fun apiRest(){
        // Crea una instancia de OkHttpClient
        val client = OkHttpClient()

        // Define el tipo de contenido del archivo JSON
        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

        // Lee el archivo JSON del almacenamiento interno
        val fileName = "data.json"
        val file = File(filesDir, fileName)
        val json = file.readText()


        // Crea una instancia de RequestBody con el archivo JSON como carga útil
        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        // Crea una instancia de la clase Request para enviar la solicitud POST
        val request = Request.Builder()
            .url("https://apibackia-ef82s.ondigitalocean.app/api/axis")
            .post(requestBody)
            .build()

        // Envia la solicitud HTTP utilizando la clase Call de OkHttp
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Manejar errores de conexión
                try {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Fallo en la conexión", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "Error al mostrar el Toast", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                // Manejar la respuesta
                val message = when (response.code) {
                    in 200..299 -> "Petición exitosa"
                    in 300..399 -> "Redirección"
                    in 400..499 -> "Error del cliente"
                    in 500..599 -> "Error del servidor"
                    else -> "Error desconocido"
                }

                try {
                    runOnUiThread {
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "Error al mostrar el Toast", e)
                }
            }
        })
    }

    fun acelemetro(){
        //INICIALIZAR VARIABLES CON REFERENCIA EN EL ACTIVITY_MAIN.XML
        buttonAction = findViewById(R.id.buttonAction)
        countTime = findViewById(R.id.countTime)

        // Inicializa la lista de datos Y limpia la lista
        dataList = mutableListOf()
        //var data: Data? = null



        // IMPRIME EN LOGCAT CUANDO SE PRESIONA EL BOTTON
        Log.d("CORRIENDO BUTTON ACCELEROMETER","CORRIENDO.....")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    //SE CARGAN DATOS
                    val data = Data(x,y,z)
                    dataList.add(data)
                    // SE MUESTRA LA INFO VISUALMENTE EN EL LOGCAT
                    Log.d("ACCELEROMETER", "x: $x, y: $y, z: $z")
                    //SE CAMBIA EL CONTADOR Y SE ACTUALIZA A LA VISTA
                    countTime.text = "${++counter}"
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        //SE EJECUTA LA SER PRECIONADO EL BOTTON buttonAction
        buttonAction.setOnClickListener {


            if (buttonAction.text == "Start") {
                counter = 0
                countTime.text = "0"
                sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
                buttonAction.text = "Stop"


            } else {
                sensorManager.unregisterListener(sensorListener)
                buttonAction.text = "Start"
                countTime.text = "0"
                // SE INSTANCIA LA FUNCION PARA EJECUTAR
                saveDataToFile()
                // REQUEST POST PARA EN LA INFO DEL JSON A LA API
                apiRest()

            }
        }



    }

}