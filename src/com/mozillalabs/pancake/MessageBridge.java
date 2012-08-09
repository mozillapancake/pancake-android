package com.mozillalabs.pancake;

import android.util.Log;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MessageBridge
{
    public interface Handler {
        Object handleMessage(String name, JSONArray arguments) throws JSONException;
    }

    // The following is private or package protected so that the JavaScript bridge cannot access it.

    private final Map<String,Handler> handlers = new HashMap<String,Handler>();

    void registerHandler(String name, Handler handler)
    {
        handlers.put(name, handler);
    }

    Handler getHandler(String name)
    {
        return handlers.get(name);
    }

    private final Map<String,WebView> webViews = new HashMap<String, WebView>();

    void registerWebView(String name, WebView webView)
    {
        webViews.put(name, webView);
    }

    WebView getWebView(String name)
    {
        return webViews.get(name);
    }

    /**
     * Receive a call from the JavaScript side. The message is either handled by one of the registered
     * handlers if it's destination is 'native' or it is forwarded to a registered WebView.
     *
     * @param request the JSON serialized request
     * @return the JSON serialized response
     */

    @SuppressWarnings("UnusedDeclaration")
    public synchronized String dispatch(String request) throws JSONException
    {
        try
        {
            Log.d("PANCAKE.MessageThing", "Received a message: " + request);

            JSONObject requestObject = new JSONObject(request);
            String destination = requestObject.getString("destination");
            JSONObject call = requestObject.getJSONObject("call");
            String callName = call.getString("name");
            JSONArray callArguments = call.getJSONArray("arguments");

            if (destination.equals("native")) {
                Log.d("PANCAKE.MessageThing", "Dispatching to native");
                return dispatchToNative(callName, callArguments);
            } else {
                Log.d("PANCAKE.MessageThing", "Dispatching to webview");
                return dispatchToWebView(destination, call);
            }
        }

        catch (JSONException exception)
        {
            Log.e("PANCAKE.MessageThing", "JSON Exception: " + exception.getMessage(), exception);
            return null;
        }
    }

    private String dispatchToNative(String callName, JSONArray callArguments) throws JSONException
    {
        Handler handler = getHandler(callName);
        Object result = handler.handleMessage(callName, callArguments);

        JSONObject response = buildResponse(true, result);
        return response.toString();
    }

    private String dispatchToWebView(String destination, JSONObject call) throws JSONException
    {
        WebView webView = getWebView(destination);
        if (webView != null) {
            String json = call.toString();
            Log.d("PANCAKE.MessageBridge", "Calling " + destination + " " + "javascript:MessageThing.handleCall(" + json + ");");
            webView.loadUrl("javascript:MessageThing.handleCall(" + json + ");");
        }
        return null;
    }

    private JSONObject buildResponse(boolean success, Object result) throws JSONException
    {
        JSONObject response = new JSONObject();
        response.put("success", success);
        response.put("result", result);
        return response;
    }
}
