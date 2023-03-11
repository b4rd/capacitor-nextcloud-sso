package com.getcapacitor.community.nextcloudsso;

import android.app.Activity;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppAccountPermissionNotGrantedException;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.exceptions.SSOException;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;

@CapacitorPlugin(name = "NextcloudSso")
public class NextcloudSsoPlugin extends Plugin {

    @PluginMethod
    public void chooseAccount(PluginCall call) {
        try {
            AccountImporter2.checkAndroidAccountPermissions(getContext());
            if (AccountImporter2.appInstalledOrNot(getActivity())) {
                startActivityForResult(call, AccountImporter2.getChooseAccountIntent(), "onAccountSelected");
            } else {
                throw new NextcloudFilesAppNotInstalledException();
            }
        } catch (NextcloudFilesAppNotInstalledException | AndroidGetAccountsPermissionNotGranted e) {
            call.reject(e.getMessage());
            UiExceptionManager.showDialogForException(getContext(), e);
        }
    }

    @ActivityCallback
    private void onAccountSelected(PluginCall call, ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK) {
            call.reject("Choose account was canceled");
            return;
        }

        try {
            startActivityForResult(call, AccountImporter2.buildRequestAuthTokenIntent(getContext(), result.getData()), "onAuthTokenResult");
        } catch (NextcloudFilesAppAccountPermissionNotGrantedException e) {
            call.reject(e.getMessage());
            UiExceptionManager.showDialogForException(getContext(), e);
        }
    }

    @ActivityCallback
    private void onAuthTokenResult(PluginCall call, ActivityResult result) {
        if (result.getResultCode() != Activity.RESULT_OK) {
            try {
                // this throws
                AccountImporter2.handleFailedAuthRequest(result.getData());
            } catch (SSOException e) {
                call.reject(e.getMessage());
                UiExceptionManager.showDialogForException(getActivity(), e);
            } catch (Exception e) {
                call.reject(e.getMessage());
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }

        SingleSignOnAccount singleSignOnAccount = AccountImporter2.extractSingleSignOnAccountFromResponse(result.getData(), getContext());
        call.resolve(toJsonObject(singleSignOnAccount));
    }

    private JSObject toJsonObject(SingleSignOnAccount singleSignOnAccount) {
        JSObject ret = new JSObject();
        ret.put("name", singleSignOnAccount.name);
        ret.put("userId", singleSignOnAccount.userId);
        ret.put("token", singleSignOnAccount.token);
        ret.put("url", singleSignOnAccount.url);
        ret.put("type", singleSignOnAccount.type);
        return ret;
    }
}
