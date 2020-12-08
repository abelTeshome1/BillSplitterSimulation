package com.example.feedthekitty

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

// screen after login
class StartScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yourtabs)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val m : MenuInflater = menuInflater
        m.inflate(R.menu.exmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        val i = Intent(this,DashBoard::class.java)
        startActivity(i)


        return true
    }



}