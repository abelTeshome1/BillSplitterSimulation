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
import java.util.*


class DashBoard : AppCompatActivity() {

    //hold arraylist of added users in tab
    private var usersTab = ArrayList<String>()
    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()

        welcome()

        val addUser : Button = findViewById(R.id.buttonAddUser)
        addUser.setOnClickListener { addUser() }

        val createTab : Button = findViewById(R.id.buttonCreateTab)
        createTab.setOnClickListener { createTab() }

        val yourTabs : Button = findViewById(R.id.buttonYourTabs)
        yourTabs.setOnClickListener {
            val intent = Intent(this, DashBoard::class.java)
            startActivity(intent)
        }

        val paymentTabs : Button = findViewById(R.id.buttonPaymentTabs)
        paymentTabs.setOnClickListener {
            val intent = Intent(this, PaymentTabs::class.java)
            startActivity(intent)
        }
    }


    private fun addUser(){

        var enteredUser: String = findViewById<AutoCompleteTextView>(R.id.add_user)!!.text.toString()
        var layout : LinearLayout = findViewById(R.id.linear);

        // no entered input
        if(enteredUser == "" || enteredUser == null) {
            Toast.makeText(
                applicationContext,
                "Enter a User",
                Toast.LENGTH_LONG
            ).show()

        //invalid user
        }else if(!Validators().validEmail(enteredUser)){

            Toast.makeText(
                applicationContext,
                "Invalid User",
                Toast.LENGTH_LONG
            ).show()

        }else{

            mAuth!!.fetchSignInMethodsForEmail(enteredUser).addOnCompleteListener(OnCompleteListener<SignInMethodQueryResult> { task ->
                val notRegistered = task.result!!.signInMethods!!.isEmpty()

                if (notRegistered) {
                    Toast.makeText(
                        applicationContext,
                        "That user is not registered",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (usersTab.contains(enteredUser)) {
                    Toast.makeText(
                        applicationContext,
                        "User is already included",
                        Toast.LENGTH_LONG
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

    private fun createTab(){
        val price = findViewById<EditText>(R.id.price)
        val event = findViewById<EditText>(R.id.event)

        val priceString = price.text.toString()
        val eventString = event.text.toString()


        if( (priceString == null || priceString == "") || (eventString == null || eventString== "") ){
            Toast.makeText(
                applicationContext,
                "Fill out Price and Event Fields",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if(!isNumeric(price.text.toString())){
            Toast.makeText(
                applicationContext,
                "Invalid Price",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if(priceString.contains('.') ){
           val cents =  priceString.substringAfter('.')
            if(cents.length > 2){
                Toast.makeText(
                    applicationContext,
                    "Invalid Price",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

        }

        if(usersTab.isEmpty()){
            Toast.makeText(
                applicationContext,
                "No Users in Tab",
                Toast.LENGTH_LONG
            ).show()

            return

        }

        findViewById<EditText>(R.id.price).text.clear()
        findViewById<EditText>(R.id.event).text.clear()
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
            eventString,
            ""
        )

    }


    private fun isNumeric(strNum: String): Boolean {
        if (strNum == null) {
           return false
        }

        try {
            val intgr: Double = strNum.toDouble()
        }catch (e: NumberFormatException){

            return false

        }

        return true;
    }

    private fun welcome(){
        val welcomeView = findViewById<TextView>(R.id.welcome)
        welcomeView.text = "Hello, " + mAuth!!.currentUser?.email.toString()

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




    private fun autoCompleteUser(){

       // val view = findViewById<AutoCompleteTextView>(R.id.add_user)

        //  mAuth!!.currentUser?.email

//        val database = FirebaseDatabase.getInstance().reference



        //Child the root before all the push() keys are found and add a ValueEventListener()
//        database.child("Users").addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(dataSnapshot: DataSnapshot) {
//                Log.d("test","hello");
//                //Basically, this says "For each DataSnapshot *Data* in dataSnapshot, do what's inside the method.
//                for (suggestionSnapshot in dataSnapshot.children) {
//
//                    val suggestion = suggestionSnapshot.child("suggestion").getValue(
//                        String::class.java
//                    )!!
//                    //Add the retrieved string to the list
//
//                    Toast.makeText(applicationContext, suggestion, Toast.LENGTH_LONG).show()
//                    val autoComplete = ArrayAdapter<String>(this@DashBoard, android.R.layout.simple_list_item_1)
//                    autoComplete.add(suggestion)
//                }
//            }
//
//            override fun onCancelled(databaseError: DatabaseError) {}
//        })
//        val ACTV = findViewById<AutoCompleteTextView>(R.id.add_user)
//        ACTV.setAdapter(autoComplete)

    }




}