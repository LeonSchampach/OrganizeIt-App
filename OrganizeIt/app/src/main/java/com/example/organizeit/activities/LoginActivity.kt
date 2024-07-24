package com.example.organizeit.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.organizeit.MainActivity
import com.example.organizeit.MainActivity.Companion
import com.example.organizeit.R
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var registerResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val resultIntent = Intent()

        registerResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val resultValue: Int? = data?.getIntExtra("id", -1)
                val resultMessage: String? = data?.getStringExtra("msg")

                if (resultMessage != "Cancelled") {
                    resultIntent.putExtra("id", resultValue)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }

    fun btnOpenRegister(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        registerResultLauncher.launch(intent)
    }

    fun btnLogin(view: View) {
        val resultIntent = Intent()

        val mail = findViewById<TextInputEditText>(R.id.tiEmail)
        val password = findViewById<TextInputEditText>(R.id.tiPassword)

        Log.i(TAG, mail.text.toString())
        Log.i(TAG, mail.text.toString())

        val jsonObject = JSONObject()
        jsonObject.put("mail", mail.text)
        jsonObject.put("password", password.text)

        val client = OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/user/login"
        val requestBody = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error at login", e)
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Login successful")
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        val id = jsonObject.getInt("id")
                        resultIntent.putExtra("id", id)
                        runOnUiThread {
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Login failed: ${response.body?.string()}")
                    }
                }
            }
        })
    }

    private fun parseResponse(jsonString: String): Shelf {
        val jsonObject = JSONObject(jsonString)
        val drawersJsonArray = jsonObject.getJSONArray("drawers")

        val drawersList = mutableListOf<Drawer>()

        for (i in 0 until drawersJsonArray.length()) {
            val drawerJson = drawersJsonArray.getJSONObject(i)
            val drawer = Drawer(
                id = drawerJson.getInt("id"),
                name = drawerJson.getString("name"),
                shelfId = drawerJson.getInt("shelfId")
            )
            drawersList.add(drawer)
        }

        val shelf = Shelf(
            id = jsonObject.getInt("id"),
            name = jsonObject.getString("name"),
            room = jsonObject.getString("room"),
            drawers = drawersList
        )

        return shelf
    }
}