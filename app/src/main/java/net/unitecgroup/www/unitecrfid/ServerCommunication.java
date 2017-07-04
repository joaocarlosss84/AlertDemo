package net.unitecgroup.www.unitecrfid;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.VolleyError;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * http://www.journaldev.com/9412/android-shared-preferences-example-tutorial
 *
 */
public class ServerCommunication {

    private static Gson gson;
    private static String serverPath;
    private static String requestPath;
    private Context context;

    private static String username;

    //region Construtor

    public ServerCommunication(Context context) {
        this.context = context;
        gson = new Gson();
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        serverPath = pref.getString("server_path", "http://192.168.1.1");
        //serverPath = context.getSharedPreferences("server_path","http://192.168.1.202");
        requestPath = serverPath + "/req";
    }

    //endregion

    //region Metodos Publicos
    public static String getServerPath() {return serverPath;}

    public void setServerPath(String path) {
        SharedPreferences.Editor editor = this.context.getSharedPreferences("MyPref", Context.MODE_PRIVATE).edit();
        editor.putString("server_path", path);
        editor.commit();
        //editor.apply();
        serverPath = path;
        requestPath = serverPath + "/req";
    }

    //endregion

/*
    public static void attemptLogin(String username, String password){
        String parameters = "{\"Password\": \"" + password + "\"}";
        JSONObject json = makeCommunicationJson("Login", username, parameters);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        master.attemptLoginServerCallback(serverResponse);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.attemptLoginServerCallback(serverResponse);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void loadRegisteredProductModels(){
        JSONObject json = makeCommunicationJson("GetProductModels", "");
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        List<ProductModel> productModels = null;
                        if (serverResponse.isConsultSuccess()){
                            JSONArray message = getMessageJSONArray(response);
                            if (message != null) {
                                productModels = new ArrayList<>();
                                for (int i=0;i<message.length();i++){
                                    ProductModel productModel = new ProductModel();
                                    try {
                                        productModel = gson.fromJson(message.get(i).toString(), productModel.getClass());
                                    } catch (JSONException e) {
                                        handleJsonException(e);
                                    }
                                    productModels.add(productModel);
                                }
                            }
                        }
                        master.loadRegisteredProductModelsServerCallback(serverResponse, productModels);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.loadRegisteredProductModelsServerCallback(serverResponse, null);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void loadRegisteredContainers(){
        JSONObject json = makeCommunicationJson("GetContainers", "");
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        List<Container> containerModelList = null;
                        if (serverResponse.isConsultSuccess()){
                            JSONArray message = getMessageJSONArray(response);
                            if (message != null) {
                                containerModelList = new ArrayList<>();
                                for (int i=0;i<message.length();i++){
                                    Container container = new Container();
                                    try {
                                        container = gson.fromJson(message.get(i).toString(), container.getClass());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    containerModelList.add(container);
                                }
                            }
                        }
                        master.loadRegisteredContainersServerCallback(serverResponse, containerModelList);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.loadRegisteredContainersServerCallback(serverResponse, null);
                    }
                }
        );

        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void loadInventory(){
        JSONObject json = makeCommunicationJson("GetInventory", "");
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        List<Container> containerModelList = null;
                        if (serverResponse.isConsultSuccess()){
                            JSONArray message = getMessageJSONArray(response);
                            if (message != null) {
                                containerModelList = new ArrayList<>();
                                for (int i=0;i<message.length();i++){
                                    Container container = new Container();
                                    try {
                                        container = gson.fromJson(message.get(i).toString(), container.getClass());
                                        container.refreshItemCount();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    containerModelList.add(container);
                                }
                            }
                        }
                        master.loadInventoryServerCallback(serverResponse, containerModelList);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.loadInventoryServerCallback(serverResponse, null);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void loadProductTags(){
        JSONObject json = makeCommunicationJson("GetProductTags", "");
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        List<ProductTag> productTagList = null;
                        if (serverResponse.isConsultSuccess()){
                            JSONArray message = getMessageJSONArray(response);
                            if (message != null) {
                                productTagList = new ArrayList<>();
                                for (int i=0;i<message.length();i++){
                                    ProductTag productTag = new ProductTag();
                                    try {
                                        productTag = gson.fromJson(message.get(i).toString(), productTag.getClass());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    productTagList.add(productTag);
                                }
                            }
                        }
                        master.loadProductTagsServerCallback(serverResponse, productTagList);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.loadProductTagsServerCallback(serverResponse, null);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void loadProductLocations(String idProduct) {
        JSONObject json = makeCommunicationJson("GetProductLocation", "{\"IdProduct\":" +idProduct + "}");
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        List<Container> containerList = null;
                        if (serverResponse.isConsultSuccess()){
                            JSONArray message = getMessageJSONArray(response);
                            if (message != null) {
                                containerList = new ArrayList<>();
                                for (int i=0;i<message.length();i++){
                                    Container container = new Container();
                                    try {
                                        container = gson.fromJson(message.get(i).toString(), container.getClass());
                                        container.refreshItemCount();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    containerList.add(container);
                                }
                            }
                        }
                        master.loadProductLocationsCallback(serverResponse, containerList);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.loadProductLocationsCallback(serverResponse, null);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void updateTags(String productId, String tags){
        String parameters = "{\"IdProduct\": \"" + productId + "\", \"EPC_CODES\": " + tags + "}}";
        JSONObject json = makeCommunicationJson("UpdateTags", parameters);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        master.updateTagsServerCallback(serverResponse);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.updateTagsServerCallback(serverResponse);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void addInventory(String parameters){
        JSONObject json = makeCommunicationJson("AddInventory", parameters);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        master.addInventoryServerCallback(serverResponse);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.addInventoryServerCallback(serverResponse);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }

    public static void updateContainer(String parameters) {
        //TODO: definir qual sera o nome da requisicao
        JSONObject json = makeCommunicationJson("UpdateContainer", parameters);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                requestPath,
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ServerResponse serverResponse = makeServerResponse(response);
                        master.updateContainerServerCallback(serverResponse);
                    }
                },

                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        ServerResponse serverResponse = makeServerResponse(error);
                        master.updateContainerServerCallback(serverResponse);
                    }
                }
        );
        Application.getInstance().getVolleyRequestQueue().add(request);
    }
*/

    //region Metodos Privados

    private static String getMessageString(JSONObject response) {
        String message = null;
        try {
            message = response.getString("Message");
        } catch (JSONException e) {
            handleJsonException(e);
        }
        return message;
    }

    private static JSONArray getMessageJSONArray(JSONObject response) {
        JSONArray message = null;
        try {
            message = response.getJSONArray("Message");
        } catch (JSONException e) {
            handleJsonException(e);
        }
        return message;
    }

    private static void handleJsonException (Exception e) {
        e.printStackTrace();
    }

    private static boolean isResponseSuccess(JSONObject response) {
        boolean success = false;
        try {
            success = response.getInt("Success") == 1;
        } catch (JSONException e) {
            handleJsonException(e);
        }
        return success;
    }

    private static JSONObject makeCommunicationJson(String action, String parameters){
        if (!parameters.startsWith("{") && !parameters.startsWith("\"")){
            parameters = "\"" + parameters + "\"";
        }
        JSONObject json = null;
        try {
            json = new JSONObject("{\"Action\":\"" + action + "\", \"User\":\"" + ServerCommunication.username + "\", \"Parameters\":" + parameters + "}");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    private static JSONObject makeCommunicationJson(String action, String username, String parameters){
        JSONObject json = null;
        try {
            json = new JSONObject("{\"Action\":\"" + action + "\", \"User\":\"" + username + "\", \"Parameters\":" + parameters + "}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private static ServerResponse makeServerResponse(JSONObject response) {
        boolean success = isResponseSuccess(response);
        String failureMessage = null;
        if (!success) {
            failureMessage = getMessageString(response);
        }
        return new ServerResponse(true, null, success, failureMessage);
    }

    private static ServerResponse makeServerResponse(VolleyError error) {
        //return new ServerResponse(false, R.string.msg_failed, false, null);
        return new ServerResponse(false, "ERROR SERVER", false, null);
    }

    private static String makeJsonArrayString(List<String> objectList) {
        String jsonArrayString = "[";
        boolean firstEntry = true;
        for (String objectString : objectList) {
            if (!firstEntry) {
                jsonArrayString += ",";
            }
            jsonArrayString += "\"" + objectString + "\"";
            firstEntry = false;
        }
        jsonArrayString += "]";
        return jsonArrayString;
    }

    //endregion
}