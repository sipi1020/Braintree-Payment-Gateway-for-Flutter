package com.deligence.braintree_payment;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalRequest;
import com.braintreepayments.api.models.PaymentMethodNonce;

import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import java.util.HashMap;

public class BraintreePaymentPlugin implements MethodCallHandler, ActivityResultListener{
  private Activity activity;
  private Context context;
  Result activeResult;
  private static final int REQUEST_CODE = 0x1337;
  String clientToken;
  HashMap<String, String> map = new HashMap<String, String>();
  public BraintreePaymentPlugin(Registrar registrar) {
      activity = registrar.activity();
      context = registrar.context();
      registrar.addActivityResultListener(this);
  }

  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "braintree_payment");
    channel.setMethodCallHandler(new BraintreePaymentPlugin(registrar));
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("showDropIn")) {
      this.activeResult=result;
      this.clientToken=call.argument("clientToken");
      payNow();
    } else {
      result.notImplemented();
    }
  }

  void payNow(){
        DropInRequest dropInRequest = new DropInRequest().clientToken(clientToken);
//        enableGooglePay(dropInRequest);
        activity.startActivityForResult(dropInRequest.getIntent(context), REQUEST_CODE);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data)  {
        switch (requestCode) {
            case REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                    String paymentNonce = result.getPaymentMethodNonce().getNonce();
                    if(paymentNonce == null && paymentNonce.isEmpty()){
                      map.put("status", "fail");
                      map.put("message", "Payment Nonce is Empty.");  
                      activeResult.success(map);
                    }
                    else{
                      map.put("status", "success");
                      map.put("message", "Payment Nouce is ready.");
                      map.put("paymentNonce", paymentNonce);
                      activeResult.success(map);
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                      map.put("status", "fail");
                      map.put("message", "User canceled the Payment");
                      activeResult.success(map);
                } else {
                    Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                    map.put("status", "fail");
                    map.put("message", error.getMessage());
                    activeResult.success(map);
                }
                return true;
            default:
              return false;
        } 
    }
}
