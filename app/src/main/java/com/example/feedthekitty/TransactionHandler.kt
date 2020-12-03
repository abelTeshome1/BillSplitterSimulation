package com.example.feedthekitty

class TransactionHandler {

    /**
     * recieves a tab, then sends out requests for payment from each user in the tab, and registers
     * the tab for payment on the server with it's amount.
     * users: an array list of accounts that the payments will be requested from
     * Owner: the owner of the tab, who the payments will be directed to
     * Amount: the positive dollar amount that the tab has
     * Returns: the unique tab id for this tab, if any of the fields are null it instead returns 0
     **/
    fun sendTab( users: ArrayList<String>, owner: String, amount: Int ) : Int{

        return 1;
    }

    /**
     * Gives the tab owner the money that has been paid into a tab, then closes the tab.
     * tabId: the unique integer id for a tab
     * returns: false if the tab does not exist or if there are any errors in processing the
     * transaction, otherwise it returns true
     */
    fun closeTab(tabId: Int) : Boolean{

        return true;
    }

    /**
     * queries the database to check if a particular user has paid for this tab
     * user: the user that is being queried
     * tabId: the unique integer identifier for this tab
     * returns: true if the user has paid, false if there are any errors or if they have not paid
     * recommend calling all users of the tab in a loop every few seconds while the app is open
     */
    fun refreshPayments(user: String, tabId: Int) : Boolean {

        return true;
    }


}