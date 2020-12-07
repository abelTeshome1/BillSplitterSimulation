package com.example.feedthekitty

import android.content.Intent
import android.util.Log
import android.widget.ScrollView
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue


class TransactionHandler {

    private var mAuth: FirebaseAuth? = null
    private lateinit var tabsReference: DatabaseReference

    /**
     * recieves a tab, then sends out requests for payment from each user in the tab, and registers
     * the tab for payment on the server with it's amount.
     * users: an array list of accounts that the payments will be requested from
     * Owner: the email of the owner of the tab, who the payments will be directed to
     * Amount: the positive dollar amount that the tab has
     * Returns: the unique String tab id for this tab, if any of the fields are null it instead returns an
     * empty string
     **/
    fun sendTab( users: ArrayList<String>, owner: String, amount: String,event : String, description: String) : String{
        if(users == null || owner == null || amount == null)
            return ""
        //test()
        val database = FirebaseDatabase.getInstance()
        tabsReference = database.getReference("Tabs")
        var id = (tabsReference.push()).key.toString()

        val iter = users.iterator()
        var userString = ""

        // iterate through users in list to create a string of users
        while(iter.hasNext()){
            val cur = iter.next()
            userString += "$cur,"
            Log.i(TAG, "adding to tab")
            addToTab(cur, id)
        }
        userString = userString.substring(0, userString.length - 1)

        val tab = Tab(event,owner, userString, "", amount, "0.00", true, description);

        Log.i(TAG, "adding new element")
        tabsReference.child(id).setValue(tab)

        addTabToOwner(owner, id)


        // TODO send out a request for payment to each user

        return id;
    }

    /**
     * adds the tabId to the tab owners ownedTabs string
     */
    private fun addTabToOwner(owner: String, tabId: String){
        val database = FirebaseDatabase.getInstance()
        val userReference = database.getReference("Users")
        val converterReference = database.getReference("emailToUid")

        //
        val email = owner.replace('.', '^')

        //this first listener is to get the uid from the email using the EmailToUid database
        converterReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = snapshot.child(email).getValue<String>().toString()

                //this second listener uses the UID to find the owner in the database, get their
                //current list of owned tabs, and append the new tab to the end of that list
                userReference.child(uid).addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val curTabs = snapshot.child("ownedTabs").getValue<String>().toString()
                        val newTabs = "$curTabs,$tabId"

                        //saves the new owned tab string to the owner
                        userReference.child(uid).child("ownedTabs").setValue(newTabs)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.i(TAG, "error")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "Error")
            }
        })
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
                    val newUser = email?.let { User(it,"1000.00", "", "") }
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

    // adds all users to tab
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
        //this first listener gets the balance of the tab

        tabReference.child(tabId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //checks to ensure the tab hasn't already been closed
                if(snapshot.child("open").getValue<Boolean>() == true){

                    //gets the owner of the tab
                    val ownerId = snapshot.child("owner").getValue<String>().toString()

                    //this line is what actually grabs the balance
                    val payout = snapshot.child("balance").getValue<String>().toString()
                    //this second listener gets the current balance of the owner
                    userReference.child(ownerId).child("balance")
                        .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentBalance = snapshot.getValue<String>().toString()
                            val newBalance = payout.toFloat() + currentBalance.toFloat()
                            //adds the new balance to the current balance and then saves it in the
                            //database, then closes the tab
                            userReference.child(ownerId).child("balance").setValue(newBalance.toString())
                            tabReference.child(tabId).child("open").setValue(false)
                        }
                            override fun onCancelled(error: DatabaseError) {
                                Log.i(TAG, "error")
                            }
                        })

                } else{
                    Log.i(TAG, "Tab has already been closed")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "error")
            }
        })
    }

    companion object {
        val TAG = "FTK"
    }

    fun testDatabase() : String{
        val userArrayList = ArrayList<String>()
        userArrayList.add("bob@gmail.com")
        userArrayList.add("joe@gmail.com")
        userArrayList.add("steve@gmail.com")
        userArrayList.add("adam@gmail.com")
        val description = "This is a test, at the start of it no one as contributed anything"
        val id = sendTab(userArrayList,"owner@gmail.com","20","Test","")

        return id
        //sendTab(userArrayList, "owner@gmail.com", "5.00")
    }
}