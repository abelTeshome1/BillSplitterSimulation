package com.example.feedthekitty

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class TransactionHandler {

    private var mAuth: FirebaseAuth? = null
    private lateinit var tabsReference: DatabaseReference


    data class Tab(
        val owner: String = "",
        val users: String = "",
        val paidUsers: String = "",
        val totalRequested: Int = 0,
        val balance: Int = 0,
        val open: Boolean = true
    )

    data class User(
        val name : String = "",
        val balance: Int = 0,
        val tabs: String = ""
    )


    /**
     * recieves a tab, then sends out requests for payment from each user in the tab, and registers
     * the tab for payment on the server with it's amount.
     * users: an array list of accounts that the payments will be requested from
     * Owner: the email of the owner of the tab, who the payments will be directed to
     * Amount: the positive dollar amount that the tab has
     * Returns: the unique String tab id for this tab, if any of the fields are null it instead returns an
     * empty string
     **/
    fun sendTab( users: ArrayList<String>, owner: String, amount: Int ) : String{
        if(users == null || owner == null || amount == null)
            return ""
        //test()
        val database = FirebaseDatabase.getInstance()
        tabsReference = database.getReference("Tabs")
        var id = (tabsReference.push()).key.toString()

        val iter = users.iterator()
        var userString = ""
        while(iter.hasNext()){
            val cur = iter.next()
            userString += "$cur,"
            Log.i(TAG, "adding to tab")
            addToTab(cur, id)
        }
        userString = userString.substring(0, userString.length - 1)
        val tab = Tab(owner, userString, "", amount, 0, true);
        Log.i(TAG, "adding new element")
        tabsReference.child(id).setValue(tab)

        // TODO send out a request for payment to each user

        return id;
    }

    /**
     * checks to see if the user is in the database, and adds them if they are not
     * also adds their email to the Email to UID section of the database
     */
    fun addUserToDatabase(){
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("Users")
        mAuth = FirebaseAuth.getInstance()
        val uid = mAuth!!.currentUser!!.uid
        val email = mAuth!!.currentUser!!.email
        if (email != null) {
            addEmailToUid(email, uid)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val exists = snapshot.child(uid).exists()
                //if the user is not already in the database, add them to it
                if(!exists) {
                    val newUser = email?.let { User(it,0, "") }
                    userRef.child(uid).setValue(newUser)
                } else{
                    Log.i(TAG, "user already exists in table")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "error")
            }
        }
        userRef.addListenerForSingleValueEvent(listener)
//        Log.i(TAG, "added listener")
    }

    /**
     * it is not possible to access other users emails or UID's while logged in, so there
     * needs to be a way to swtich betwen the email and uid so that users can be accessed
     *
     * users are not stored by their email as periods cannot be used to name data sets in the
     * database.
     *
     * this is the same reason that the periods have to be replaced with ^'s
     */
    private fun addEmailToUid(email : String, uid : String){
        val database = FirebaseDatabase.getInstance()
        val converterRef = database.getReference("emailToUid")
        val newEmail = email.replace('.','^')
        converterRef.child(newEmail).setValue(uid)
    }
    private fun addToTab(user: String, tabId: String){
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("Users")
        val converterRef = database.getReference("emailToUid")
        var tabs = ""
        //changes the .'s to ^'s as the database cannot have data sets with .'s in the name
        val email = user.replace('.','^')
        Log.i(TAG, "did the prep work")

        //this first listener gets the uid of user from their email
        converterRef.child(email).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = snapshot.getValue<String>().toString()
                //this second nested listener gets the current list of tabs the user is in
                //so that it can add the user to the new tab without erasing the old ones
                userRef.child(uid).addListenerForSingleValueEvent( object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        tabs = snapshot.child("tabs").getValue<String>().toString()
                        if(tabs.equals("")){
                            tabs = tabId
                        } else{
                            tabs += ",$tabId"
                        }
                        //saves the tab list to the user
                        userRef.child(uid).child("tabs").setValue(tabs)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.i(TAG, "error")
                    }
                })

            }

            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "error")
            }
        })


    }


    /**
     * Gives the tab owner the money that has been paid into a tab, then closes the tab.
     * tabId: the unique String id for a tab
     * does not return, will make a toast when the tab is closed
     */
    fun closeTab(tabId: String){
        val database = FirebaseDatabase.getInstance()
        val tabReference = database.getReference("Tabs")
        val userReference = database.getReference("Users")
        mAuth = FirebaseAuth.getInstance()
        /*
        this method can only be called by the owner of the tab it is closing, so it is safe to
        use the id of the currently logged in user as the id for the owner
         */
        val ownerId = mAuth!!.currentUser!!.uid
        //this first listener gets the balance of the tab
        tabReference.child(tabId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //this line is what actually grabs the balance
                val payout = snapshot.child("balance").getValue<Int>()
                //this second listener gets the current balance of the owner
                userReference.child(ownerId).child("balance").addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentBalance = snapshot.getValue<Int>()
                        val newBalance = payout?.let { currentBalance?.plus(it) }
                        //adds the new balance to the current balance and then saves it in the
                        //database, then closes the tab
                        userReference.child(ownerId).child("balance").setValue(newBalance)
                        tabReference.child(tabId).child("open").setValue(false)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.i(TAG, "error")
                    }
                })

            }

            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "error")
            }
        })
    }

    /**
     * queries the database to check if a particular user has paid for this tab
     * user: the user that is being queried
     * tabId: the unique integer identifier for this tab
     * returns: true if the user has paid, false if there are any errors or if they have not paid
     */
    //this function should not be called here.  listeners should be set up in the dashboard page
//    fun refreshPayments(user: String, tabId: Int) : Boolean {
//
//        return true;
//    }

    companion object {
        val TAG = "FTK"
    }

    fun testDatabase(){
        val userArrayList = ArrayList<String>()

        userArrayList.add("one@gmail.com")
        userArrayList.add("two@gmail.com")
        userArrayList.add("three@gmail.com")
        sendTab(userArrayList, "owner@gmail.com", 5)
    }
}