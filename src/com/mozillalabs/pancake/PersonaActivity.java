package com.mozillalabs.pancake;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.*;

import java.io.*;

public class PersonaActivity extends Activity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.persona);

        Intent intent = getIntent();
        String origin = intent.getStringExtra("origin");

        final String injectedJavaScript = readPersonaJavaScript(origin);
        Log.d("PANCAKE.persona.js", "JS = " + injectedJavaScript);

        //

        WebView webView = (WebView) findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(new PersonaHelper(), "PersonaHelper");

        webView.setWebViewClient(new WebViewClient() {
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                Log.d("PANCAKE.PersonaActivity", "shouldOverrideUrlLoading " + url);
//                view.loadUrl(url);
//                return true;
//            }

            @Override
            public void onPageFinished(WebView webView, String url) {
                String js = "javascript:(function() { " + injectedJavaScript + "})()";
                Log.d("PANCAKE.PersonaActivity", "Injecting " + js);
                webView.loadUrl(js);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("PANCAKE.PersonaActivity", "onConsoleMessage " + consoleMessage.messageLevel().toString() + " " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " " + consoleMessage.message());
                return true;
            }



            @Override
            public void onReceivedTitle(WebView webView, String title) {
                //String js = "javascript:(function() { " + injectedJavaScript + "})()";
                String js = "javascript:" + injectedJavaScript;
                Log.d("PANCAKE.PersonaActivity", "Injecting " + js);
                webView.loadUrl(js);
            }
        });

        webView.loadUrl("https://login.persona.org/sign_in#NATIVE");
    }

    private String readPersonaJavaScript(String origin)
    {
        try {
            String result = "";

            InputStream inputStream = getResources().openRawResource(R.raw.persona);
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferReader.readLine()) != null) {
                result += line;
            }

            bufferReader.close();
            inputStream.close();

            result = result.replaceAll("__ORIGIN__", origin);

            return result;
        } catch (IOException e) {
            return null;
        }
    }

    private final class PersonaHelper
    {
        @SuppressWarnings("UnusedDeclaration")
        public void assertionReady(final String assertion)
        {
            PersonaActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("PANCAKE.PersonaHelper", "assertionReady(" + assertion + ")");
                    Intent intent = new Intent().putExtra("assertion", assertion);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            });
        }

        @SuppressWarnings("UnusedDeclaration")
        public void assertionFailure(String assertion)
        {
            PersonaActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("PANCAKE.PersonaHelper", "assertionFailure");
                    Intent intent = new Intent();
                    setResult(Activity.RESULT_CANCELED, intent);
                    finish();
                }
            });
        }
    }
}
