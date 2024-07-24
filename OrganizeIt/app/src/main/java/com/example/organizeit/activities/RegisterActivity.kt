package com.example.organizeit.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.organizeit.R

class RegisterActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RegisterActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


    }

    fun btnReturn(view: View) {
        val resultIntent = Intent()

        resultIntent.putExtra("id", -1)
        resultIntent.putExtra("msg", "Cancelled")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}