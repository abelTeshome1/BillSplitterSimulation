package com.example.feedthekitty

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.FragmentActivity
import com.example.feedthekitty.R.color.selected
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

class PaymentTabs  : AppCompatActivity() {
    lateinit var ownedButton: View
    lateinit var tabsYouOweButton: View
    lateinit var user: String
    lateinit var listView: ListView
    lateinit var ownedAdapter: tabListViewAdapter
    lateinit var partOfAdapter: tabListViewAdapter

    private var mAuth: FirebaseAuth? = null
    private var ownedList = arrayListOf<uidAndTab>()
    private var partOfList = arrayListOf<uidAndTab>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paymenttabs)
        ownedAdapter = tabListViewAdapter(
            this,
            R.layout.tab_badge_view,
            ownedList
        )
        partOfAdapter = tabListViewAdapter(
            this,
            R.layout.tab_badge_view,
            partOfList
        )
        initializeViews()

        mAuth = FirebaseAuth.getInstance()

        user = mAuth!!.currentUser!!.email.toString()
        synchronized(this) {
            retrieveOwnedTabs()
            retrivePartOfTabs()
        }




        listView.adapter = ownedAdapter

        listView.onItemClickListener = AdapterView.OnItemClickListener{parent, view, position, id ->
            val obj = parent.adapter.getItem(position) as uidAndTab
            val intent = Intent(this, TabDetailActivity::class.java)
            intent.putExtra("eventName", obj.tab.eventName)
            intent.putExtra("owner", obj.tab.owner)
            intent.putExtra("description", obj.tab.description)
            intent.putExtra("balance", obj.tab.balance)
            intent.putExtra("paidUsers", obj.tab.paidUsers)
            intent.putExtra("totalRequested", obj.tab.totalRequested)
            intent.putExtra("users", obj.tab.users)
            intent.putExtra("tabId", obj.uid)
            intent.putExtra("open", obj.tab.open)
            Log.i(TAG, obj.tab.eventName + " " + obj.tab.users)
            startActivity(intent)
        }

        tabsYouOweButton.setOnClickListener(){
            listView.adapter = partOfAdapter
            tabsYouOweButton.setBackgroundColor(resources.getColor(R.color.selected))
            ownedButton.setBackgroundColor(resources.getColor(R.color.notSelected))
        }

        ownedButton.setOnClickListener(){
            listView.adapter = ownedAdapter
            tabsYouOweButton.setBackgroundColor(resources.getColor(R.color.notSelected))
            ownedButton.setBackgroundColor(resources.getColor(R.color.selected))
        }

    }

    private fun initializeViews(){
        ownedButton = findViewById<Button>(R.id.select_owned_tabs)
        tabsYouOweButton = findViewById<Button>(R.id.select_part_of_tabs)
        listView = findViewById<ListView>(R.id.list)

        ownedAdapter.notifyDataSetChanged()

        ownedButton.isPressed = true
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

    private fun refreshOwned(tab:Tab, i: String){
        Log.i(TAG, "Entered Owned")
        ownedList.add(uidAndTab(tab, i))
        ownedAdapter.notifyDataSetChanged()
    }

    private fun refreshPartOf(tab:Tab, i:String){
        Log.i(TAG, "Entered Owned")
        partOfList.add(uidAndTab(tab, i))
        partOfAdapter.notifyDataSetChanged()
    }

    private fun retrieveOwnedTabs(){
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("Users")
        val converterRef = database.getReference("emailToUid")
        val tabReference = database.getReference("Tabs")
        var tabs = ""
        //changes the .'s to ^'s as the database cannot have data sets with .'s in the name
        val email = user.replace('.','^')
        Log.i(TAG, "did the prep work")

        //this first listener gets the uid of user from their email

                val uid = mAuth!!.currentUser!!.uid
                //this second nested listener gets the current list of tabs the user is in
                //so that it can add the user to the new tab without erasing the old ones
                userRef.child(uid).addValueEventListener( object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        tabs = snapshot.child("ownedTabs").getValue<String>().toString()
                        //saves the tab list to the user
                        val tabArray = tabs.split(",")

                        for (i in tabArray){
                            if(i != null) {
                                tabReference.child(i)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val open = snapshot.child("open")
                                                .getValue<Boolean>().toString().toBoolean()


                                            val tab = Tab(
                                                snapshot.child("eventName").getValue<String>()
                                                    .toString(),
                                                snapshot.child("owner").getValue<String>()
                                                    .toString(),
                                                snapshot.child("users").getValue<String>()
                                                    .toString(),
                                                snapshot.child("paidUsers").getValue<String>()
                                                    .toString(),
                                                snapshot.child("totalRequested").getValue<String>()
                                                    .toString(),
                                                snapshot.child("balance").getValue<String>()
                                                    .toString(),
                                                open,
                                                snapshot.child("description").getValue<String>()
                                                    .toString()
                                            )

                                            if (tab.balance != "null")
                                                refreshOwned(tab, i)


                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.i(TransactionHandler.TAG, "error")
                                        }


                                    })
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.i(TransactionHandler.TAG, "error")
                    }
                })
    }

    private fun retrivePartOfTabs(){
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("Users")
        val converterRef = database.getReference("emailToUid")
        val tabReference = database.getReference("Tabs")
        var tabs = ""
        //changes the .'s to ^'s as the database cannot have data sets with .'s in the name
        val email = user.replace('.','^')
        Log.i(TAG, "did the prep work")

        //this first listener gets the uid of user from their email

        val uid = mAuth!!.currentUser!!.uid
        //this second nested listener gets the current list of tabs the user is in
        //so that it can add the user to the new tab without erasing the old ones
        userRef.child(uid).addValueEventListener( object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tabs = snapshot.child("tabs").getValue<String>().toString()
                //saves the tab list to the user
                val tabArray = tabs.split(",")

                for (i in tabArray){
                    if(i != null) {
                        tabReference.child(i)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val openString = snapshot.child("open")
                                        .getValue<Boolean>().toString()
                                    var open = false
                                    if(openString != null)
                                        open = openString.toBoolean()

                                    val test =
                                        snapshot.child("eventName").getValue<String>()
                                            .toString()
                                    val tab = Tab(
                                        snapshot.child("eventName").getValue<String>().toString(),
                                        snapshot.child("owner").getValue<String>().toString(),
                                        snapshot.child("users").getValue<String>().toString(),
                                        snapshot.child("paidUsers").getValue<String>().toString(),
                                        snapshot.child("totalRequested").getValue<String>().toString(),
                                        snapshot.child("balance").getValue<String>().toString(),
                                        open as Boolean,
                                        snapshot.child("description").getValue<String>().toString()
                                    )
                                    if (tab.balance != "null")
                                        refreshPartOf(tab, i)

                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.i(TransactionHandler.TAG, "error")
                                }
                            })
                    }
                }


            }
            override fun onCancelled(error: DatabaseError) {
                Log.i(TransactionHandler.TAG, "error")
            }
        })







    }
    class uidAndTab{
        lateinit var uid:String
        lateinit var tab:Tab
        constructor(t: Tab, i:String){
            uid = i
            tab = t
        }

    }

    companion object {
        val TAG = "ftk"
    }
}