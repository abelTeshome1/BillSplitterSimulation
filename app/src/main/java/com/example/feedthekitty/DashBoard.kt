package com.example.feedthekitty

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.SignInMethodQueryResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

class DashBoard : AppCompatActivity() {

    //hold arraylist of added users in tab
    private var usersTab = ArrayList<String>()
    private var mAuth: FirebaseAuth? = null

    private var  allUsers =  ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()

        balanceView()
        listUsers()

        val addUser : Button = findViewById(R.id.buttonAddUser)
        addUser.setOnClickListener { addUser() }

        val createTab : Button = findViewById(R.id.buttonCreateTab)
        createTab.setOnClickListener { createTab() }


    }

    /** This function adds the user the to a scroll view that lists all the
     * participants of the event. */
    private fun addUser(){

        var enteredUser: String = findViewById<AutoCompleteTextView>(R.id.add_user)!!.text.toString()
        var layout : LinearLayout = findViewById(R.id.linear);

        // no entered input
        if(enteredUser == "" || enteredUser == null) {
            Toast.makeText(
                applicationContext,
                "Enter a User",
                Toast.LENGTH_SHORT
            ).show()

            //invalid user
        }else if(!Validators().validEmail(enteredUser)){

            Toast.makeText(
                applicationContext,
                "Invalid User",
                Toast.LENGTH_SHORT
            ).show()

        }else{

            mAuth!!.fetchSignInMethodsForEmail(enteredUser).addOnCompleteListener(OnCompleteListener<SignInMethodQueryResult> { task ->
                val notRegistered = task.result!!.signInMethods!!.isEmpty()

                if (notRegistered) {
                    Toast.makeText(
                        applicationContext,
                        "That user is not registered",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (usersTab.contains(enteredUser)) {
                    Toast.makeText(
                        applicationContext,
                        "User is already included",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {

                    // enters user into list and create scroll view to see all included users
                    usersTab.add(enteredUser);
                    val view: TextView = TextView(this)

                    view.text = enteredUser

                    view.setTextColor(Color.parseColor("#FFFFFF"))
                    view.gravity = Gravity.CENTER;
                    view.textSize = 15F

                    layout.addView(view)
                    Toast.makeText(applicationContext, "User Added to Tab", Toast.LENGTH_LONG)
                        .show()
                }
            })

            // clear text
            findViewById<AutoCompleteTextView>(R.id.add_user)!!.text.clear()

        }

    }

    /** This function creates the tab with the given input from the user and clears the fields
     * upon pressing the 'Create New Tab' button.*/

    private fun createTab(){
        val price = findViewById<EditText>(R.id.price)
        val event = findViewById<EditText>(R.id.event)
        val description = findViewById<EditText>(R.id.description)

        val priceString = price.text.toString()
        val eventString = event.text.toString()
        val descriptionString = description.text.toString()


        if( (priceString == null || priceString == "") || (eventString == null || eventString== "") ){
            Toast.makeText(
                applicationContext,
                "Fill out Price and Event Fields",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if(!isNumeric(price.text.toString())){
            Toast.makeText(
                applicationContext,
                "Invalid Price",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if(priceString.contains('.') ){
            val cents =  priceString.substringAfter('.')
            if(cents.length > 2){
                Toast.makeText(
                    applicationContext,
                    "Invalid Price",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

        }

        if(usersTab.isEmpty()){
            Toast.makeText(
                applicationContext,
                "No Users in Tab",
                Toast.LENGTH_SHORT
            ).show()

            return

        }

        findViewById<EditText>(R.id.price).text.clear()
        findViewById<EditText>(R.id.event).text.clear()
        findViewById<EditText>(R.id.description).text.clear()
        findViewById<LinearLayout>(R.id.linear).removeAllViews()

        Toast.makeText(
            applicationContext,
            "Tab has been created",
            Toast.LENGTH_LONG
        ).show()


        TransactionHandler().sendTab(
            usersTab,
            mAuth?.currentUser?.email.toString(),
            priceString,
            eventString, descriptionString
        )

    }


    private fun isNumeric(strNum: String): Boolean {
        if (strNum == null) {
            return false
        }

        try {
            val intgr: Double = strNum.toDouble()
            if(intgr <= 0.0){
                Toast.makeText(applicationContext, "Must request more than 0", Toast.LENGTH_LONG).show()
                return false
            }
        }catch (e: NumberFormatException){

            return false

        }

        return true;
    }

    /** This function sets the balance the user has in a text view when creating a Tab */
    private fun balanceView(){

        val textView = findViewById<TextView>(R.id.textView)
        val balView = findViewById<TextView>(R.id.balance)

        textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        val database = FirebaseDatabase.getInstance()
        val userReference = database.getReference("Users")
        val converterReference = database.getReference("emailToUid")

        val email = mAuth!!.currentUser?.email.toString().replace('.', '^')

        // sets the users balance upon login
        converterReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = snapshot.child(email).getValue<String>().toString()

                userReference.child(uid).addListenerForSingleValueEvent(object :
                    ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val balance = snapshot.child("balance").getValue<String>().toString()
                        balView.text = "Balance: " + "$" + balance
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.i(TransactionHandler.TAG, "error")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i(TransactionHandler.TAG, "Error")
            }
        })

    }

    /** This function gives user prediction when searching for a user in the database */
    private fun listUsers(){

        val database = FirebaseDatabase.getInstance()
        val userReference = database.getReference("emailToUid")

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                for (i in snapshot.children) {

                    val newEmail = i.key.toString().replace('^', '.')
                    allUsers.add(newEmail)

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i(TransactionHandler.TAG, "error")
            }
        })

        // uses adapter predict entered users in database
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line, allUsers
        )
<<<<<<< HEAD
        val textView = findViewById(R.id.add_user) as AutoCompleteTextView
=======
        val textView = findViewById<AutoCompleteTextView>(R.id.add_user)
>>>>>>> 9e544db03a42803f3def9249d96a46fd9bc3741b
        textView.setAdapter(adapter)

    }


}