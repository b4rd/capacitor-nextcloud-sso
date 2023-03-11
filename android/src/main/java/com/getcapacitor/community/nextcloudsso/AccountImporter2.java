package com.getcapacitor.community.nextcloudsso;

import static com.nextcloud.android.sso.Constants.NEXTCLOUD_FILES_ACCOUNT;
import static com.nextcloud.android.sso.Constants.NEXTCLOUD_SSO;
import static com.nextcloud.android.sso.Constants.NEXTCLOUD_SSO_EXCEPTION;
import static com.nextcloud.android.sso.Constants.SSO_SHARED_PREFERENCE;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.Constants;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountPermissionNotGrantedException;
import com.nextcloud.android.sso.exceptions.SSOException;
import com.nextcloud.android.sso.model.FilesAppType;
import com.nextcloud.android.sso.model.SingleSignOnAccount;

import java.io.IOException;
import java.util.Arrays;

/**
 * Rewrite of {@link com.nextcloud.android.sso.AccountImporter} that doesn't start activities,
 * just returns the Intents to them.
 */
public class AccountImporter2 {
    private static final String TAG = AccountImporter2.class.getCanonicalName();
    private static final String PREF_ACCOUNT_STRING = "PREF_ACCOUNT_STRING";
    private static final String AUTH_TOKEN_SSO = "SSO";

    public static final int CHOOSE_ACCOUNT_SSO = 4242;
    public static final int REQUEST_AUTH_TOKEN_SSO = 4243;
    public static final int REQUEST_GET_ACCOUNTS_PERMISSION = 4244;

    private static SharedPreferences SHARED_PREFERENCES;

    private static final String[] ACCOUNT_TYPES = Arrays.stream(FilesAppType.values()).map(a -> a.accountType).toArray(String[]::new);

    public static Intent getChooseAccountIntent() {
        return AccountManager.newChooseAccountIntent(null, null, ACCOUNT_TYPES,
                true, null, AUTH_TOKEN_SSO, null, null);
    }

    public static void requestAndroidAccountPermissionsAndPickAccount(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.GET_ACCOUNTS},
                REQUEST_GET_ACCOUNTS_PERMISSION);
    }

    public static void checkAndroidAccountPermissions(Context context) throws AndroidGetAccountsPermissionNotGranted {
        // https://developer.android.com/reference/android/accounts/AccountManager#getAccountsByType(java.lang.String)
        // Caller targeting API level below Build.VERSION_CODES.O that have not been granted the
        // Manifest.permission.GET_ACCOUNTS permission, will only see those accounts managed by
        // AbstractAccountAuthenticators whose signature matches the client.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // Do something for lollipop and above versions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission not granted yet!");
                throw new AndroidGetAccountsPermissionNotGranted();
            } else {
                Log.d(TAG, "Permission granted!");
            }
        }
    }

    public static boolean appInstalledOrNot(Context context) {
        boolean returnValue = false;
        PackageManager pm = context.getPackageManager();
        for (final var appType : FilesAppType.values()) {
            try {
                pm.getPackageInfo(appType.packageId, PackageManager.GET_ACTIVITIES);
                returnValue = true;
                break;
            } catch (PackageManager.NameNotFoundException e) {
                Log.v(TAG, e.getMessage());
            }
        }
        return returnValue;
    }

    public static SingleSignOnAccount extractSingleSignOnAccountFromResponse(Intent intent, Context context) {
        Bundle future = intent.getBundleExtra(NEXTCLOUD_SSO);

        String accountName = future.getString(AccountManager.KEY_ACCOUNT_NAME);
        String userId = future.getString(Constants.SSO_USER_ID);
        if (userId == null) {
            // backwards compatibility
            userId = future.getString("username");
        }
        String token = future.getString(Constants.SSO_TOKEN);
        String serverUrl = future.getString(Constants.SSO_SERVER_URL);
        String type = future.getString("accountType");

        SharedPreferences mPrefs = getSharedPreferences(context);
        String prefKey = getPrefKeyForAccount(accountName);
        SingleSignOnAccount ssoAccount = new SingleSignOnAccount(accountName, userId, token, serverUrl, type);
        try {
            mPrefs.edit().putString(prefKey, SingleSignOnAccount.toString(ssoAccount)).apply();
        } catch (IOException e) {
            Log.e(TAG, "SSO failed", e);
        }
        return ssoAccount;
    }

    public static void handleFailedAuthRequest(Intent data) throws SSOException {
        String exception = data.getStringExtra(NEXTCLOUD_SSO_EXCEPTION);
        throw SSOException.parseNextcloudCustomException(new Exception(exception));
    }

    public static Intent buildRequestAuthTokenIntent(Context context, Intent intent) throws NextcloudFilesAppAccountPermissionNotGrantedException {
        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        Account account = AccountImporter.getAccountForName(context, accountName);
        if(account == null) {
            throw new NextcloudFilesAppAccountPermissionNotGrantedException();
        }

        String componentName = FilesAppType.findByAccountType(account.type).packageId;

        Intent authIntent = new Intent();
        authIntent.setComponent(new ComponentName(componentName,
                "com.owncloud.android.ui.activity.SsoGrantPermissionActivity"));
        authIntent.putExtra(NEXTCLOUD_FILES_ACCOUNT, account);
        return authIntent;
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        if(SHARED_PREFERENCES != null) {
            return SHARED_PREFERENCES;
        } else {
            return context.getSharedPreferences(SSO_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        }
    }

    protected static String getPrefKeyForAccount(String accountName) {
        return PREF_ACCOUNT_STRING + accountName;
    }


    /**
     * Allows developers to set the shared preferences that the account information should be stored in.
     * This is helpful when writing unit tests
     */
    public static void setSharedPreferences(SharedPreferences sharedPreferences) {
        SHARED_PREFERENCES = sharedPreferences;
    }
}
