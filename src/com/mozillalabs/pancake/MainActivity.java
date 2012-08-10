package com.mozillalabs.pancake;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity
{
    private MessageBridge messageBridge;

    private LinearLayout linearLayout;
    private WebView mainWebView;
    private WebView topWebView;
    private WebView drawerWebView;
    private WebView browserWebView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Setup the message passing

        messageBridge = new MessageBridge();

        messageBridge.registerHandler("info", new MessageBridge.Handler() {
            @Override
            public Object handleMessage(String name, JSONArray arguments) {
                Log.d("PANCAKE", "Message " + name + " " + arguments.toString());
                return new JSONObject();
            }
        });

        messageBridge.registerHandler("load:url", new MessageBridge.Handler() {
            @Override
            public Object handleMessage(String name, JSONArray arguments) throws JSONException {
                Log.d("PANCAKE.MainActivity", "Received load:url request " + arguments.toString());
                JSONObject place = arguments.getJSONObject(0);
                browserWebView.loadUrl(place.getString("url"));
                return null;
            }
        });

        messageBridge.registerHandler("viewer:show", new MessageBridge.Handler() {
            @Override
            public Object handleMessage(String name, JSONArray arguments) throws JSONException {
                Log.d("PANCAKE", "VIEWER:SHOW");
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        linearLayout.setLeft(- (780 + 250 - 20));
                    }
                });
                return null;
            }
        });

        // Setup our views

        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        topWebView = setupTopWebView();

        mainWebView = setupMainWebView();
        mainWebView.setLayoutParams(new LinearLayout.LayoutParams(780, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(mainWebView);

        drawerWebView = setupDrawerWebView();
        drawerWebView.setLayoutParams(new LinearLayout.LayoutParams(250, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(drawerWebView);

        browserWebView = setupBrowserWebView();
        browserWebView.setLayoutParams(new LinearLayout.LayoutParams(780, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.addView(browserWebView);

        setContentView(linearLayout);

        // This gesture recognizer will open the drawer

        final GestureDetector gestureDetector = new GestureDetector(this,new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Toast.makeText(MainActivity.this, "Fling! " + Float.toString(velocityX), Toast.LENGTH_SHORT).show();
                if (velocityX < -200) {
                    // Fling left
                    if (linearLayout.getLeft() == 0) {
                        linearLayout.setLeft(-230);
                    } else if (linearLayout.getLeft() == -230) {
                        linearLayout.setLeft(- (780 + 250 - 20));
                    }
                } else if (velocityX > 200) {
                    // Fling right
                    if (linearLayout.getLeft() == -230) {
                        linearLayout.setLeft(0);
                    } else if (linearLayout.getLeft() == (-(780 + 250 - 20))) {
                        linearLayout.setLeft(-780);
                    } else if (linearLayout.getLeft() == -780) {
                        linearLayout.setLeft(0);
                    }
                }
                return true;
            }
        });

        drawerWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        // Load the landing page. If the user is logged in then it will redirect to /main

        loadMainWebView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra("assertion"))
        {
            final String assertion = data.getStringExtra("assertion");
            Log.d("PANCAKE.MainActivity", "Assertion is " + data.getStringExtra("assertion"));
            String url = "javascript:Root.verifyAssertion(\"" + assertion+ "\");";
            mainWebView.loadUrl(url);
        }
    }

    WebView setupMainWebView()
    {
        WebView webView = new WebView(this);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);

        webView.setInitialScale(100);

        webView.addJavascriptInterface(messageBridge, "PancakeNativeMessageBridge");
        messageBridge.registerWebView("main", webView);

        webView.addJavascriptInterface(new PancakeHelper(), "PancakeHelper");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("PANCAKE.mainWebView", "shouldOverrideUrlLoading " + url);
                if (url.endsWith("/main") || url.endsWith("/root") || url.endsWith("/")) {
                    url = url + "?platform=android";
                }
                view.loadUrl(url);

                if (url.endsWith("/main?platform=android")) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadTopWebView();
                            loadDrawerWebView();
                        }
                    });
                }

                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d("PANCAKE.mainWebView", "onPageStarted " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("PANCAKE.mainWebView", "onPageFinished " + url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.d("PANCAKE.mainWebView", "onLoadResource " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d("PANCAKE.mainWebView", "onReceivedError " + failingUrl + " " + description);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("PANCAKE.mainWebView", "onConsoleMessage " + consoleMessage.messageLevel().toString() + " " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " " + consoleMessage.message());
                return true;
            }
        });

        return webView;
    }

    void loadMainWebView()
    {
        mainWebView.loadUrl("http://174.143.205.91:6543/?platform=android");
    }

    WebView setupTopWebView()
    {
        Log.d("PANCAKE.MainActivity", "Setting up top WebView");

        WebView webView = new WebView(this);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(messageBridge, "PancakeNativeMessageBridge");
        messageBridge.registerWebView("top", webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("PANCAKE.topWebView", "shouldOverrideUrlLoading " + url);
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d("PANCAKE.topWebView", "onPageStarted " + url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.d("PANCAKE.topWebView", "onLoadResource " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d("PANCAKE.topWebView", "onReceivedError " + failingUrl + " " + description);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("PANCAKE.topWebView", "onConsoleMessage " + consoleMessage.messageLevel().toString() + " " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " " + consoleMessage.message());
                return true;
            }
        });

        return webView;
    }

    void loadTopWebView()
    {
        topWebView.loadUrl("http://174.143.205.91:6543/top?platform=android");
    }

    WebView setupDrawerWebView()
    {
        WebView webView = new WebView(this);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setInitialScale(100);

        webView.addJavascriptInterface(messageBridge, "PancakeNativeMessageBridge");
        messageBridge.registerWebView("drawer", webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("PANCAKE.drawerWebView", "shouldOverrideUrlLoading " + url);
                if (url.endsWith("/drawer")) {
                    url = url + "?platform=android";
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d("PANCAKE.drawerWebView", "onPageStarted " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("PANCAKE.drawerWebView", "onPageFinished " + url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.d("PANCAKE.drawerWebView", "onLoadResource " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d("PANCAKE.drawerWebView", "onReceivedError " + failingUrl + " " + description);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("PANCAKE.drawerWebView", "onConsoleMessage " + consoleMessage.messageLevel().toString() + " " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + " " + consoleMessage.message());
                return true;
            }
        });

        return webView;
    }

    void loadDrawerWebView()
    {
        Log.d("PANCAKE.MainActivity", "loadDrawerWebView");
        drawerWebView.loadUrl("http://174.143.205.91:6543/drawer?platform=android");
    }

    WebView setupBrowserWebView()
    {
        WebView webView = new WebView(this);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        return webView;
    }

    private final class PancakeHelper
    {
        @SuppressWarnings("UnusedDeclaration")
        public void startPersonaFlow(final String origin)
        {
            Log.d("PANCAKE.PancakeHelper", "startPersonaFlow with origin=" + origin);

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent()
                            .setClass(MainActivity.this, PersonaActivity.class)
                            .putExtra("origin", origin);
                    startActivityForResult(intent, 0);
                }
            });
        }
    }
}
