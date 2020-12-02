package com.example.feedthekitty

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.Gravity
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginTop
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.SignInMethodQueryResult
import java.util.*


class DashBoard : AppCompatActivity() {

    //hold arraylist of added users in tab
    private var usersTab = ArrayList<String>()

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()


        val welcomeView = findViewById<TextView>(R.id.welcome)
        welcomeView.text = "Hello, " + mAuth!!.currentUser?.email.toString()

        val textView = findViewById<TextView>(R.id.textView)
        textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        val button : Button = findViewById(R.id.button1)
        button.setOnClickListener { addUser() }



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

            mAuth!!.fetchSignInMethodsForEmail(enteredUser)
                .addOnCompleteListener(OnCompleteListener<SignInMethodQueryResult> { task ->
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
                        view.setTextColor(Color.parseColor("#000000"))
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