package io.github.hu2di.gdpradmob;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

public class FullscreenActivity extends AppCompatActivity {

    private final String TAG = "hu2diMainActivity";
    private View btnGdpr;
    private AdView smartBanner;

    private ConsentInformation consentInformation;
    // Use an atomic boolean to initialize the Google Mobile Ads SDK and load ads once.
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        initView();

        //GDPR
        requestConsentForm();
    }

    private void initView() {
        btnGdpr = findViewById(R.id.btn_gdpr);
        btnGdpr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConsentForm();
            }
        });
    }

    private void requestConsentForm() {
        // Debug only
        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("") // find test device id in logcat
                .build();

        // Set tag for under age of consent. false mean users are not under age of consent.
        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setConsentDebugSettings(debugSettings) // Debug only this param
                .setTagForUnderAgeOfConsent(false)
                .build();

        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        consentInformation.reset(); // Debug only
        consentInformation.requestConsentInfoUpdate(
                this,
                params,
                (ConsentInformation.OnConsentInfoUpdateSuccessListener)  () -> {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            this,
                            (ConsentForm.OnConsentFormDismissedListener) loadAndShowError -> {
                                if (loadAndShowError != null) {
                                    // Consent gathering failed.
                                    Log.d(TAG, String.format("%s: %s",
                                            loadAndShowError.getErrorCode(),
                                            loadAndShowError.getMessage()));
                                }

                                // Consent has been gathered.
                                if (consentInformation.canRequestAds()) {
                                    initializeMobileAdsSdk();
                                }
                            }
                    );
                },
                (ConsentInformation.OnConsentInfoUpdateFailureListener) requestConsentError -> {
                    // Consent gathering failed.
                    Log.d(TAG, String.format("%s: %s",
                            requestConsentError.getErrorCode(),
                            requestConsentError.getMessage()));
                }
        );

        // Check if you can init Mobile Ads Sdk in parallel while checking for new consent info.
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk();
        }
    }

    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            Log.d(TAG, "isMobileAdsInitializeCalled");
            return;
        }
        Log.d(TAG, "isNOT MobileAdsInitializeCalled");

        MobileAds.initialize(FullscreenActivity.this, initializationStatus -> {
            initSmartBanner();
        });
    }

    private void initSmartBanner() {
        smartBanner = findViewById(R.id.smartBanner);
        AdRequest adRequest = new AdRequest.Builder().build();
        smartBanner.loadAd(adRequest);
    }

    private void showConsentForm() {
        UserMessagingPlatform.showPrivacyOptionsForm(FullscreenActivity.this,
                new ConsentForm.OnConsentFormDismissedListener() {
                    @Override
                    public void onConsentFormDismissed(FormError formError) {
                        if (formError != null) {
                            Toast.makeText(FullscreenActivity.this, formError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
