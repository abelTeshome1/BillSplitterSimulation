package com.example.feedthekitty

import android.os.Bundle
import android.os.PersistableBundle
import android.renderscript.Sampler
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.android.synthetic.main.activity_tab_detail.*
import kotlinx.android.synthetic.main.login.*
import org.w3c.dom.Text

class TabDetailActivity : AppCompatActivity(){

    private lateinit var mAuth: FirebaseAuth

    private lateinit var eventName: String
    private lateinit var owner: String
    private lateinit var description: String
    private lateinit var balance: String
    private lateinit var paidUsers: String
    private lateinit var totalRequested: String
    private lateinit var users: String
    private lateinit var tabId: String
    private lateinit var amountContributed: String
    private var open: Boolean = false


    private lateinit var paymentButton: Button
    private lateinit var balanceView: TextView
    private lateinit var requestedView: TextView
    private lateinit var amountContributedView: TextView
    private lateinit var newContributionView: EditText
    private lateinit var eventView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var contributeMoreText: TextView
    private lateinit var listView: ListView
    private lateinit var closeTabButton: Button



    internal lateinit var mAdapter: UserListAdapter



    override fun onCreate(savedInstanceState: Bundle?){
        Log.i(TAG, "started activity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_detail)
        mAuth = FirebaseAuth.getInstance()

        Log.i(TAG, "did basic setup")

        //for this intent, put in every field in tabs
        eventName = intent.getStringExtra("eventName").toString()
        owner = intent.getStringExtra("owner").toString()
        description = intent.getStringExtra("description").toString()
        balance = intent.getStringExtra("balance").toString()
        paidUsers = intent.getStringExtra("paidUsers").toString()
        totalRequested = intent.getStringExtra("totalRequested").toString()
        users = intent.getStringExtra("users").toString()
        tabId = intent.getStringExtra("tabId").toString()
        open = intent.getBooleanExtra("open", false)


        Log.i(TAG, "got data from intent")

        paymentButton = findViewById(R.id.payButton)
        balanceView = findViewById(R.id.currentBalance)
        requestedView = findViewById(R.id.amountRequested)
        amountContributedView = findViewById(R.id.amountContributed)
        newContributionView = findViewById(R.id.ContributionEditText)
        eventView = findViewById(R.id.eventText)
        descriptionView = findViewById(R.id.descriptionText)
        contributeMoreText = findViewById(R.id.contributeMoreText)
        listView = findViewById(R.id.userListView)
        closeTabButton = findViewById(R.id.closeTabButton)

        Log.i(TAG, "set up views")




        eventView!!.text = eventName
        val balanceText = "$$balance"
        balanceView!!.text = balanceText
        val requestedText = "$$totalRequested"
        requestedView!!.text = requestedText
        descriptionView!!.text = description

        if(mAuth.currentUser!!.email.equals(owner)){
            setupOwnerView()
        } else{
            setupPayerView()
        }


        paymentButton!!.setOnClickListener(){
            pay()
        }

        closeTabButton.setOnClickListener(){
            closeTab()
        }


    }


    private fun setupOwnerView(){
        amountContributedView!!.visibility = View.GONE
        amountContributedView!!.visibility = View.GONE
        paymentButton!!.visibility = View.GONE
        newContributionView.visibility = View.GONE
        contributeMoreText!!.visibility = View.GONE
        listView.visibility = View.VISIBLE
        if(open)
            closeTabButton.visibility = View.VISIBLE
        else
            closeTabButton.visibility = View.INVISIBLE

        setupListView()
    }

    private fun setupListView(){

        mAdapter = UserListAdapter(applicationContext)
        listView!!.adapter = mAdapter

        val list = users.split(',')
        val paidList = paidUsers.split(',')
        val iterator = paidList.iterator()
        var processed = ""
        while(iterator.hasNext()){
            val cur = iterator.next()
            if(cur != ""){
                val curParts = iterator.next().split(':')
                val email = curParts[0]
                val amount = curParts[1]
                processed += "$email,"
                mAdapter.add(email, "$$amount")
            }
        }
        val iter = list.iterator()
        while(iter.hasNext()){
            val cur = iter.next()
            if(!processed.contains(cur)){
                mAdapter.add(cur, "$0")
            }
        }
    }

    private fun setupPayerView(){
        listView!!.visibility = View.GONE
        closeTabButton!!.visibility = View.GONE
        if(open) {
            paymentButton!!.visibility = View.VISIBLE
            contributeMoreText.text = "Contribute more?"
            newContributionView.visibility = View.VISIBLE
        } else {
            paymentButton.visibility = View.INVISIBLE
            contributeMoreText.text = "This tab has been closed"
            newContributionView.visibility = View.INVISIBLE
        }
        contributeMoreText!!.visibility = View.VISIBLE
        amountContributedView!!.visibility = View.VISIBLE


        val currentContribution = getCurrentUserContribution()
        val contributedText = "you have contributed $currentContribution to this fund."
        amountContributedView!!.text = contributedText
    }
    private fun getCurrentUserContribution() : String{
        val user = mAuth.currentUser!!.email

        val users = paidUsers.split(',')
        val iter = users.iterator()
        var amount = "$0"
        while (iter.hasNext()){
            val cur = iter.next().split(':')
            if(cur[0] == user)
                amount = "$" + cur[1]
        }
        amountContributed = amount
        return amount
    }

    private fun pay(){
        val amount = newContributionView.text.toString()
        val database = FirebaseDatabase.getInstance()
        val uid = mAuth.uid.toString()
        val userReference = database.getReference("Users").child(uid)
        val tabReference = database.getReference("Tabs").child(tabId)

        val processPayment = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val balance = snapshot.child("balance").getValue<String>().toString()
                val newBalance = balance.toFloat() - amount.toFloat()
                if (newBalance < 0.0f) {
                    Toast.makeText(applicationContext, "Account Balance Too Low", Toast.LENGTH_LONG)
                        .show()
                } else {
                    userReference.child("balance").setValue(newBalance.toString())

                    tabReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentBalance = snapshot.child("balance").getValue<String>().toString()
                            val newTabBalance = currentBalance.toFloat() + amount.toFloat()
                            tabReference.child("balance").setValue(newTabBalance.toString())
                            val newPaidUserString = getNewPaidUserString(amount, snapshot.child("paidUsers").getValue<String>().toString())
                            tabReference.child("paidUsers").setValue(newPaidUserString)
                            Toast.makeText(applicationContext, "Transaction Complete", Toast.LENGTH_LONG).show()
                            amountContributedView.text = "You have Contributed $amountContributed to this fund."
                            balanceView.text = "$$newTabBalance"


                            }

                        override fun onCancelled(error: DatabaseError) {
                            Log.i(TAG, "error, balance changed but tab not updated")
                            Toast.makeText(
                                applicationContext,
                                "Lost connection to server, contact customer support for a refund",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "error cancelled changing user balance")
                Toast.makeText(
                    applicationContext,
                    "lost connection to server, transaction cancelled",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        tabReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.child("open").getValue<Boolean>() == true){
                    userReference.addListenerForSingleValueEvent(processPayment)
                } else{
                    Toast.makeText(applicationContext, "This tab has been closed", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG,"cannot connect to server, please try again later")
            }
        })


    }

    private fun getNewPaidUserString(amount: String, paidUserString: String) : String{
        val user = mAuth.currentUser!!.email

        val users = paidUserString.split(',')
        val iter = users.iterator()
        var newString = ""
        var newAmount = amount
        while (iter.hasNext()){
            val cur = iter.next()
            if(cur != "") {
                val curParts = cur.split(":")
                if (curParts[0] == user) {
                    val curAmount = curParts[1].toFloat()
                    newAmount = (curAmount + amount.toFloat()).toString()
                } else {
                    newString += ",$cur"
                }
            }
        }
        newString += ",$user:$newAmount"
        paidUsers = newString
        amountContributed = newAmount
        return newString
    }



    private fun closeTab(){
        if(open) {
            TransactionHandler().closeTab(tabId)
            Toast.makeText(applicationContext,"The Tab has been closed", Toast.LENGTH_LONG)
            closeTabButton.visibility = View.INVISIBLE
        }
    }

    companion object{
        const val TAG = "ftk"
    }
}