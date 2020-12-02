package com.example.feedthekitty

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.SignInMethodQueryResult
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*


class MainActivity : AppCompatActivity() {

    private var loginBtn: Button? = null
    private var alreadyRegistered: TextView? = null
    private var emailView: EditText? = null
    private var passView: EditText? = null
    private var conPassView: EditText? = null
    private var validator = Validators()
    private var eventView: EditText? = null
    private var priceView: EditText? = null


    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register)
        initializeViews()
        mAuth = FirebaseAuth.getInstance()
        loginBtn!!.setOnClickListener(){ registerNewUser() }
        alreadyRegistered!!.setOnClickListener(){
            val intent = Intent(this, Login::class.java)

            startActivity(intent)
        }

    }


    private fun registerNewUser() {

        val email: String = emailView!!.text.toString()
        val password: String = passView!!.text.toString()
        val confPass: String = conPassView!!.text.toString()

        if (!validator.validEmail(email)) {
            Toast.makeText(applicationContext, "Please enter a valid email...", Toast.LENGTH_LONG).show()
            return
        }
        if (!validator.validPassword(password)) {
            Toast.makeText(applicationContext, "Please enter a valid password!", Toast.LENGTH_LONG).show()
            return
        }

        if(!confPass.equals(password)){
            Toast.makeText(applicationContext, "Passwords do not match!", Toast.LENGTH_LONG).show()
            return
        }

        mAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        applicationContext,
                        "Registration successful!",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Registration failed! Please try again later",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

    }



    private fun initializeViews() {

        alreadyRegistered = findViewById(R.id.already)
        loginBtn = findViewById(R.id.login)
        emailView = findViewById(R.id.email)
        passView = findViewById(R.id.password)
        conPassView = findViewById(R.id.confirm_password)
        eventView = findViewById(R.id.event)
    }
}