package com.example.feedthekitty

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
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
    private lateinit var closedTextView: TextView
    private lateinit var recommendedView: TextView
    private lateinit var recommendedText: TextView

    internal lateinit var mAdapter: UserListAdapter


    override fun onCreate(savedInstanceState: Bundle?){
        Log.i(TAG, "started activity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_detail)
        mAuth = FirebaseAuth.getInstance()

        Log.i(TAG, "did basic setup")

        //for this intent, put in every field in tabs
        //extracts all the fields from the intent so they can be displayed and used
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

        //initalize the views
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
        closedTextView = findViewById(R.id.closedText)
        recommendedView = findViewById(R.id.recommendedView)
        recommendedText = findViewById(R.id.recommendedText)


        Log.i(TAG, "set up views")

        //give the views shared between the user view and owner view their values
        eventView.text = eventName
        val balanceText = "$$balance"
        balanceView.text = balanceText
        val requestedText = "$$totalRequested"
        requestedView.text = requestedText
        descriptionView.text = description

        //changes how the activity is displayed based on whether or not the current user owns the tab
        if(mAuth.currentUser!!.email.equals(owner)){
            setupOwnerView()
        } else{
            setupPayerView()
        }

        paymentButton.setOnClickListener(){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Payment Confirmation")
            builder.setMessage(R.string.payment_confirmation_string)
            builder.setPositiveButton(R.string.yes){ _, _ ->
                pay()
            }
            builder.setNegativeButton(R.string.no){ _, _ ->
                Log.i(TAG, "said no to payment")
            }
            builder.show()
        }

        closeTabButton.setOnClickListener(){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Close Tab Confirmation")
            builder.setMessage(R.string.close_tab_confirmation)
            builder.setPositiveButton(R.string.yes){ _, _ ->
                closeTab()
            }
            builder.setNegativeButton(R.string.no){ _, _ ->
                Log.i(TAG, "said no to close tab")
            }
            builder.show()
        }



    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.funds_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val intent = Intent(this,AddUserFundsActivity::class.java)
        startActivity(intent)

        return true
    }

    /**
     * this function shows the fields relevant to the tab owner, and hides those relevant to the
     * tab users
     */
    private fun setupOwnerView(){
        amountContributedView.visibility = View.GONE
        amountContributedView.visibility = View.GONE
        paymentButton.visibility = View.GONE
        newContributionView.visibility = View.GONE
        contributeMoreText.visibility = View.GONE
        recommendedView.visibility = View.GONE
        recommendedText.visibility = View.GONE
        listView.visibility = View.VISIBLE
        //if the tab has been closed the close button should not appear
        if(open) {
            closeTabButton.visibility = View.VISIBLE
            closedTextView.visibility = View.GONE
        }        else{
            closedTextView.visibility = View.VISIBLE
            closeTabButton.visibility = View.INVISIBLE

        }

        setupListView()
        setUpListeners()
        setUpOwnerListeners()
    }

    /**
     * initalizes and populates the listview with the users of the tab based on the passed in
     * paidUser and user strings.  this listview shows each user, whether or not they have paid, and
     * how much they have contributed anything to the tab. it should only be called when the tab owner
     * is the one looking at the tab
     */
    private fun setupListView(){

        mAdapter = UserListAdapter(applicationContext)
        listView.adapter = mAdapter

        //splits the list to get each individual user
        val list = users.split(',')
        val paidList = paidUsers.split(',')
        val iterator = paidList.iterator()
        var processed = ""
        //goes through the list of paid users, adding them to the adapter with the amount they have
        //contributed
        while(iterator.hasNext()){
            val cur = iterator.next()
            if(cur != ""){
                //the paid users have their payment information stored in the form of
                // email@email.com:money  This seperateds the values so they can be passed seperately
                val curParts = cur.split(':')
                val email = curParts[0]
                val amount = curParts[1]
                processed += "$email,"
                mAdapter.add(email, "$$amount")
            }
        }
        //goes through and adds the users who have not paid to the adapter at the end of it
        val iter = list.iterator()
        while(iter.hasNext()){
            val cur = iter.next()
            if(!processed.contains(cur)){
                mAdapter.add(cur, "$0")
            }
        }
    }

    private fun setUpListeners(){
        val database = FirebaseDatabase.getInstance()
        val tabReference = database.getReference("Tabs").child(tabId)
        //ensures the balance is accurately listed
        tabReference.child("balance").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "lost connection")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                balance = snapshot.getValue<String>().toString()
                balanceView.text = "$$balance"
            }
        })

    }
    private fun setUpOwnerListeners(){
        val database = FirebaseDatabase.getInstance()
        val tabReference = database.getReference("Tabs").child(tabId)
        tabReference.child("paidUsers").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "lost connection")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                paidUsers = snapshot.getValue<String>().toString()
                //there's no easy way to find out which value specifically has been changed
                //so it is easier to simply remake the adapter with the new string
                setupListView()
            }
        })
    }
    /**
     * this function is called if the user who is accessing this tab does not own it.  it hides
     * the buttons and textviews that contain information relevant to only the tab owner, and provides
     * the views necessecary for the user to pay the tab
     */
    private fun setupPayerView(){
        //hide the owner views
        listView.visibility = View.GONE
        closeTabButton.visibility = View.GONE
        closedTextView.visibility = View.GONE

        //if the tab is closed, the user should not be able to pay more, but should still be able
        //to access the tab
        if(open) {
            paymentButton.visibility = View.VISIBLE
            contributeMoreText.text = "Contribute more?"
            newContributionView.visibility = View.VISIBLE
            recommendedView.visibility = View.VISIBLE
            recommendedText.visibility = View.VISIBLE
        } else {
            paymentButton.visibility = View.INVISIBLE
            contributeMoreText.text = "This tab has been closed"
            newContributionView.visibility = View.INVISIBLE
            recommendedView.visibility = View.INVISIBLE
            recommendedText.visibility = View.INVISIBLE
        }
        contributeMoreText.visibility = View.VISIBLE
        amountContributedView.visibility = View.VISIBLE


        //displays how much the user has contributed
        val currentContribution = getCurrentUserContribution()
        val contributedText = "you have contributed $currentContribution to this fund."
        amountContributedView.text = contributedText
        val numUsers = users.split(",").size.toFloat()
        val recDonation = totalRequested.toFloat() / numUsers
        recommendedView.text = "$$recDonation"


        setUpListeners()
    }

    /**
     * this function breaks down the paidUser string to check if the current user is in the string
     * and if they are return how much they have contributed to this tab, otherwise return $0
     */
    private fun getCurrentUserContribution() : String{
        val user = mAuth.currentUser!!.email

        val users = paidUsers.split(',')
        val iterator = users.iterator()
        var amount = "$0"
        while (iterator.hasNext()){
            val cur = iterator.next().split(':')
            if(cur[0] == user)
                amount = "$" + cur[1]
        }
        amountContributed = amount
        return amount
    }

    /**
     * this function allows the user to pay the amount they have listed in the payment edit text to
     * the tab.  It first checks to see if the tab has been closed while the user was looking at it,
     * then it ensures that the user has enough money in their account for the transaction before
     * subtracting that money from the account.  it then gets the current balance from the
     * tab, adds the amount contributed to that balance, and sets the new balance in the server.
     * once this transaction is completed, the editText where the user inputted their contribution
     * is cleared, the balance and contribution views are updated,
     * and a toast is shown stating the contribution was successful
     */
    private fun pay(){
        val amount = newContributionView.text.toString()
        val database = FirebaseDatabase.getInstance()
        val uid = mAuth.uid.toString()
        val userReference = database.getReference("Users").child(uid)
        val tabReference = database.getReference("Tabs").child(tabId)

        //this listener happens SECOND, the first one is at the bottom and calls this one
        //this listener deducts the balance from the users account
        val processPayment = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val balance = snapshot.child("balance").getValue<String>().toString()
                val newBalance = balance.toFloat() - amount.toFloat()
                if (newBalance < 0.0f) {
                    Toast.makeText(applicationContext, "Account Balance Too Low", Toast.LENGTH_LONG)
                        .show()
                } else {
                    //sets the users balance equal to the new deducted amount
                    userReference.child("balance").setValue(newBalance.toString())

                    //this last listener gets the current account balance, adds the user amount, then
                    //updates the value in the database and sets the views to reflect the new values
                    tabReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentBalance = snapshot.child("balance").getValue<String>().toString()
                            val newTabBalance = currentBalance.toFloat() + amount.toFloat()
                            //set the database value to it's new value
                            tabReference.child("balance").setValue(newTabBalance.toString())

                            //gets the new paidUserString with the current users new contribution
                            //added into it
                            val newPaidUserString = getNewPaidUserString(amount,
                                snapshot.child("paidUsers").getValue<String>().toString())

                            //set the databases paidUser string to the new one
                            tabReference.child("paidUsers").setValue(newPaidUserString)
                            Toast.makeText(applicationContext, "Transaction Complete", Toast.LENGTH_LONG).show()
                            //update the views to reflect the changes
                            amountContributedView.text = "You have Contributed $amountContributed to this fund."
                            balanceView.text = "$$newTabBalance"
                            newContributionView.setText("")
                            newContributionView.clearFocus()


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
        //this checks to ensure the tab is open before the transaction occurs
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

    /**
     * this function edits the paid user string to add the new contribution ammount to the current
     * user
     * amount: the amount to be added to the current users contribution in the paidUserString
     * paidUserString: the current paidUser string fresh from the database
     */
    private fun getNewPaidUserString(amount: String, paidUserString: String) : String{
        val user = mAuth.currentUser!!.email

        val users = paidUserString.split(',')
        val iter = users.iterator()
        var newString = ""
        var newAmount = amount
        //this iterator creates a new string with everyone but the current user in it
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
        //adds the current user with the new balance to the end of the new paid user string
        newString += ",$user:$newAmount"
        paidUsers = newString
        amountContributed = newAmount
        return newString
    }


    /**
     * this function calls the TransactionHandlers closeTab method, which closes the tab and
     * pays the tab balance to the tab owner
     */
    private fun closeTab(){
        //ensures the tab is closed so the money isn't paid twice
        if(open) {
            TransactionHandler().closeTab(tabId)
            Toast.makeText(applicationContext,"The Tab has been closed", Toast.LENGTH_LONG)
            closeTabButton.visibility = View.INVISIBLE
            closedTextView.visibility = View.VISIBLE
        }
    }

    companion object{
        const val TAG = "ftk"
    }
}