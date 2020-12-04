package com.example.feedthekitty

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.SignInMethodQueryResult
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.ArrayList


class Login : AppCompatActivity() {
    private var mDatabaseReference: DatabaseReference? = null
    private var mDatabase: FirebaseDatabase? = null
    private var loginBtn: Button? = null
    private var emailView: EditText? = null
    private var passView: EditText? = null

    private var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        initializeViews()

        mDatabase = FirebaseDatabase.getInstance()
        mDatabaseReference = mDatabase!!.reference.child("Users")
        mAuth = FirebaseAuth.getInstance()

        loginBtn!!.setOnClickListener { loginUserAccount() }
    }


    private fun loginUserAccount() {

        //modeled after example materials

        val email = emailView?.text.toString()
        val password = passView?.text.toString()

        val intent = Intent(this, DashBoard::class.java)

        mAuth!!.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                val transactionHandler = TransactionHandler()
                transactionHandler.addUserToDatabase()
                intent.putExtra(USER_ID, mAuth!!.uid)
                intent.putExtra(USER_EMAIL, emailView.toString())
                startActivity(intent)
            }
        }
    }


    private fun initializeViews() {
        loginBtn = findViewById(R.id.login)
        emailView = findViewById(R.id.email)
        passView = findViewById(R.id.password)
    }

    companion object {
        const val USER_EMAIL = "useremail"
        const val USER_ID = "userid"
        val TAG = "FTK"
    }
}