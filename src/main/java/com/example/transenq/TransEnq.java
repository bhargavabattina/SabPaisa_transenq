package com.example.transenq;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.os.AsyncTask;

public class TransEnq {

    public interface TransEnqCallback {
        void onSuccess(String result);

        void onFailure(String error);
    }

    public static void performTransactionEnquiry(String clientCode, String clientTxnId, String authKey, String authIV, String apiUrl, TransEnqCallback callback) {
        new MyAsyncTask(callback, clientCode, clientTxnId, authKey, authIV, apiUrl).execute();
    }

    private static class MyAsyncTask extends AsyncTask<Void, Void, String> {
        private TransEnqCallback callback;
        private String clientCode;
        private String clientTxnId;
        private String authKey;
        private String authIV;
        private String apiUrl;

        public MyAsyncTask(TransEnqCallback callback, String clientCode, String clientTxnId, String authKey, String authIV, String apiUrl) {
            this.callback = callback;
            this.clientCode = clientCode;
            this.clientTxnId = clientTxnId;
            this.authKey = authKey;
            this.authIV = authIV;
            this.apiUrl = apiUrl;
        }

        @Override
        protected String doInBackground(Void... params) {

            String encryptedString = null;

            String query = "clientCode=" + clientCode + "&clientTxnId=" + clientTxnId;

            try {
                encryptedString = Encryptor.encrypt(authKey, authIV, query);

            } catch (Exception e) {
                e.printStackTrace();
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("clientCode", clientCode);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            try {
                jsonObject.put("statusTransEncData", encryptedString);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            String response = performPostCall(apiUrl, jsonObject.toString());

            String decryptedString = null;
            try {
                JSONObject statusResponse = new JSONObject(response);
                decryptedString = Encryptor.decrypt(authKey, authIV, statusResponse.getString("statusResponseData"));

            } catch (Exception e) {
                e.printStackTrace();
            }

            return decryptedString;
        }


        protected void onPostExecute(String result) {
            if (callback != null) {
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onFailure("Failed to get result");
                }
            }
        }
    }


    private static String performPostCall(String requestURL, String postDataParams) {
        URL url;
        StringBuilder response = new StringBuilder();
        try {
            url = new URL(requestURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            OutputStream os = conn.getOutputStream();
            os.write(postDataParams.getBytes("UTF-8"));
            os.close();

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            } else {
                // Handle the error case
                // You can use conn.getErrorStream() to get the error response
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();

    }

}