package com.example.organizeit.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.organizeit.MainActivity
import com.example.organizeit.R
import com.example.organizeit.models.Drawer
import com.example.organizeit.models.Shelf
import com.example.organizeit.ssl_certificate.TrustAllCertificates
import com.example.organizeit.ssl_certificate.TrustAllHostnames
import com.example.organizeit.ssl_certificate.TrustAllSSLSocketFactory
import com.example.organizeit.util.ConfigUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class DrawerAdapter(private val drawers: MutableList<Drawer>, private val context: Context) : RecyclerView.Adapter<DrawerAdapter.DrawerViewHolder>() {

    private val secure = ConfigUtil.isSecure(context)

    inner class DrawerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val drawerNameInput: EditText = view.findViewById(R.id.drawerNameInput)
        val deleteButton: ImageButton = view.findViewById(R.id.buttonDeleteDrawerInput)

        /*fun bind(drawer: Drawer, context: Context, drawerAdapter: DrawerAdapter) {
            deleteButton.setOnClickListener{
                if (drawer.id != null)
                    deleteDrawer(drawer, context)
            }
        }*/
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_drawer_input, parent, false)
        return DrawerViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrawerViewHolder, position: Int) {
        val drawer = drawers[position]
        holder.drawerNameInput.setText(drawer.name)
        //holder.bind(drawer, context, this)

        holder.deleteButton.setOnClickListener {
            if (drawer.id != null)
                removeDrawerAt(position)
            else
                removeDrawer(position)
        }

        holder.drawerNameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                drawer.name = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun getItemCount(): Int = drawers.size

    fun addDrawer(drawer: Drawer) {
        drawers.add(drawer)
        notifyItemInserted(drawers.size - 1)
    }

    private fun removeDrawerAt(position: Int) {
        deleteDrawer(drawers[position], context)
        //drawers.removeAt(position)
        //notifyItemRemoved(position)
        removeDrawer(position)
    }

    private fun removeDrawer(position: Int) {
        drawers.removeAt(position)
        //notifyItemChanged(position)
        notifyItemRemoved(position)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val drawer = drawers.removeAt(fromPosition)
        drawers.add(toPosition, drawer)
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getDrawers(): MutableList<Drawer> = drawers

    private fun deleteDrawer(drawer: Drawer, context: Context) {
        val client = if (secure) OkHttpClient.Builder()
            .sslSocketFactory(TrustAllSSLSocketFactory.getSocketFactory(), TrustAllCertificates())
            .hostnameVerifier(TrustAllHostnames())
            .build() else OkHttpClient()
        val apiUrl = "${ConfigUtil.getApiBaseUrl(context)}/drawer/deleteDrawer?id=${drawer.id}"

        val request = Request.Builder()
            .url(apiUrl)
            .delete() // Empty DELETE body
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                (context as? MainActivity)?.runOnUiThread {
                    Toast.makeText(context, "Error deleting drawer", Toast.LENGTH_SHORT).show()
                    Log.e("ShelfAdapter", "Error deleting drawer", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    (context as? MainActivity)?.runOnUiThread {
                        Toast.makeText(context, "Error deleting drawer", Toast.LENGTH_SHORT).show()
                        Log.e("ShelfAdapter", "Error deleting drawer: ${response.code}")
                    }
                } else {
                    (context as? MainActivity)?.runOnUiThread {
                    }
                }
            }
        })
    }
}

