package com.shxhzhxx.app

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        list1.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        list1.adapter = MyAdapter()

        list2.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        list2.adapter = MyAdapter()

        list3.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        list3.adapter = MyAdapter()

        open.setOnClickListener { resideLayout.openPane() }
        close.setOnClickListener { resideLayout.closePane() }
        switcher.setOnCheckedChangeListener { _, isChecked ->
            resideLayout.isEnabled = isChecked
        }
        switcher.isChecked = true
    }
}


class MyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        object : RecyclerView.ViewHolder(TextView(parent.context).also { v ->
            v.setPadding(20, 20, 20, 20)
            v.setOnClickListener { Toast.makeText(v.context, v.text, Toast.LENGTH_SHORT).show() }
        }) {

        }

    override fun getItemCount() = 30

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as TextView).text = "item$position"
    }
}