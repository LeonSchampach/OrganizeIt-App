package com.example.organizeit.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.organizeit.R
import com.example.organizeit.activities.LoginActivity.Companion
import com.example.organizeit.util.ConfigUtil
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


    }

    fun btnRegister(view: View) {
        val resultIntent = Intent()

        val firstname = findViewById<TextInputEditText>(R.id.tiFirstname)
        val lastname = findViewById<TextInputEditText>(R.id.tiLastname)
        val mail = findViewById<TextInputEditText>(R.id.tiEmail)
        val password = findViewById<TextInputEditText>(R.id.tiPassword)
        val passwordRepeat = findViewById<TextInputEditText>(R.id.tiPasswordRepeat)

        if (password.text.toString() == passwordRepeat.text.toString()) {
            val jsonObject = JSONObject()
            jsonObject.put("firstname", firstname.text)
            jsonObject.put("lastname", lastname.text)
            jsonObject.put("mail", mail.text)
            jsonObject.put("password", password.text)

            val client = OkHttpClient()
            val apiUrl = "${ConfigUtil.getApiBaseUrl(this)}/user/register"
            val requestBody = jsonObject.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Error at register", e)
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Register successful",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG, "Register successful")
                        }

                        val responseBody = response.body.string()
                        val jsonResponseObject = JSONObject(responseBody)
                        val id = jsonResponseObject.getInt("id")
                        resultIntent.putExtra("id", id)
                        runOnUiThread {
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Register failed",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e(TAG, "Register failed: ${response.body.string()}")
                        }
                    }
                }
            })
        }
        else
            Toast.makeText(this, "Passwords don't match!", Toast.LENGTH_SHORT).show()

    }

    fun btnReturn() {
        val resultIntent = Intent()

        resultIntent.putExtra("id", -1)
        resultIntent.putExtra("msg", "Cancelled")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}