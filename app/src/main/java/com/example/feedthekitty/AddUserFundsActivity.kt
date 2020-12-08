package com.example.feedthekitty

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue

class AddUserFundsActivity : AppCompatActivity(){

    private lateinit var addButton: Button
    private lateinit var amountText: EditText
    private lateinit var currentBalanceView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_funds)

        addButton = findViewById(R.id.addFundsButton)
        amountText = findViewById(R.id.editTextNumberDecimal)
        currentBalanceView = findViewById(R.id.currentBalanceView)

        setCurrentBalanceListener()
        addButton.setOnClickListener(){
            addMoney(amountText.text.toString())
        }

    }

    /**
     * this function sets up a listener to keep the current balance textview updated
     */
    private fun setCurrentBalanceListener(){
        val mAuth = FirebaseAuth.getInstance()
        val uid = mAuth.currentUser!!.uid
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("Users").child(uid).child("balance")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("ftk", "error connection issue")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val balance = snapshot.getValue<String>().toString()
                currentBalanceView.text = "$$balance"
            }
        })
    }

    /**
     * adds the designated funds to the current users account.  Will clear the textview and then
     * show a toast confirming the funds were added
     * amount: the amount in a string that will be added to the users account
     */
    private fun addMoney(amount: String){
        val mAuth = FirebaseAuth.getInstance()
        val uid = mAuth.currentUser!!.uid
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("Users").child(uid)
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i("ftk", "error")
                Toast.makeText(applicationContext, "connection error", Toast.LENGTH_LONG).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val currentBalance = snapshot.child("balance").getValue<String>().toString()
                val newBalance = amount.toFloat() + currentBalance.toFloat()
                reference.child("balance").setValue(newBalance.toString())
                Toast.makeText(applicationContext,"Funds added to your account", Toast.LENGTH_LONG).show()
                amountText.setText("")
            }
        })
    }

}