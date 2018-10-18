package ru.p3tr0vich.fuel.sync

import android.accounts.Account
import android.content.ContentResolver
import android.content.Context
import android.text.TextUtils
import ru.p3tr0vich.fuel.R
import ru.p3tr0vich.fuel.helpers.PreferencesHelper
import ru.p3tr0vich.fuel.helpers.SystemServicesHelper
import ru.p3tr0vich.fuel.utils.UtilsLog

class SyncAccount(context: Context) {

    private val accountManager = SystemServicesHelper.getAccountManager(context)

    val authority: String = context.getString(R.string.sync_authority)

    private val accountName = context.getString(R.string.sync_account_name)
    private val accountType = context.getString(R.string.sync_account_type)

    val account: Account

    val isSyncActive: Boolean
        get() = ContentResolver.isSyncActive(account, authority)

    var yandexDiskToken: String?
        get() = getUserData(YANDEX_DISK_TOKEN)
        set(token) = setUserData(YANDEX_DISK_TOKEN, token)

    val isYandexDiskTokenEmpty: Boolean
        get() = TextUtils.isEmpty(yandexDiskToken)

    init {
        account = createAccount(context)
    }

    private fun createAccount(context: Context): Account {
        val account: Account

        val accounts = accountManager!!.getAccountsByType(accountType)

        if (accounts.isNotEmpty()) {
            account = accounts[0]
        } else {
            account = Account(accountName, accountType)

            if (accountManager.addAccountExplicitly(account, null, null)) {
                setIsSyncable(account, PreferencesHelper.getInstance(context).isSyncEnabled)

                UtilsLog.d(TAG, "createAccount", "addAccountExplicitly == true")
            } else {
                UtilsLog.d(TAG, "createAccount", "addAccountExplicitly == false")
            }
        }

        return account
    }

    private fun setIsSyncable(account: Account, syncable: Boolean) {
        ContentResolver.setIsSyncable(account, authority, if (syncable) 1 else 0)
    }

    fun setIsSyncable(syncable: Boolean) {
        setIsSyncable(account, syncable)
    }

    private fun getUserData(key: String): String {
        return accountManager!!.getUserData(account, key)
    }

    private fun setUserData(key: String, value: String?) {
        accountManager!!.setUserData(account, key, value)
    }

    companion object {
        private const val TAG = "SyncAccount"

        private const val YANDEX_DISK_TOKEN = "yandex disk token"
    }
}
