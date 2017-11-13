package net.unitecgroup.www.unitecrfid;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Aplicacao que controla as chamadas ao volley
 */
public class Application extends android.app.Application {

    //region Atributos Privados

    private static Context context;

    private static Application application;

    private static RequestQueue volleyRequestQueue;

    private static String username;

    private static Toast toast;

    //private static ServerCommunication serverCommunication;

    private static SharedPreferences sharedPreferences;

    private static DatabaseTable db;

    private static boolean bUseAsEditor = false;

    //endregion

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();

        application = this;

        volleyRequestQueue = Volley.newRequestQueue(this);

        //sharedPreferences = getSharedPreferences("settings_file", MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        //serverCommunication = new ServerCommunication();
        db = new DatabaseTable(this);

        toast = Toast.makeText(context,"", Toast.LENGTH_SHORT);
    }

    /**
     * Retorna a instancia da aplicacao
     * @return instancia da aplicacao
     */
    public synchronized static Application getInstance() {
        return application;
    }

    /**
     * Retorna a fila de requisoes de conexoes
     * @return fila de requisoes de conexoes
     */
    public static RequestQueue getVolleyRequestQueue() {
        return volleyRequestQueue;
    }

    /**
     * Retorno o contexto global da aplicacao
     * @return contexto global da aplicacao
     */
    public static Context getContext() {
        return context;
    }

    /*
    public static ServerCommunication getServerCommunication() {
        return serverCommunication;
    }
    */

    public static DatabaseTable getDatabase() { return db; }

    public static String getSavedUsername(){
        return sharedPreferences.getString("username", null);
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        Application.username = username;
    }

    /**
     * Retorna o toast da aplica??o
     * @return
     */
    public static Toast getToast() {
        return toast;
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public static String loadServerPath() {
        //SharedPreferences sharedPref = context.getSharedPreferences("settings_file", context.MODE_PRIVATE);
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        //SharedPreferences.Editor editor = sharedPref.edit();
        //SharedPreferences.Editor editor = sharedPreferences.edit();
        //String path = sharedPref.getString("server_path", "http://192.168.1.120/volley/request.php");
        String path = sharedPreferences.getString(SettingsActivity.SERVER_IP, "");
        if (!path.startsWith("http://"))
            path = "http://" + path;
        return path;
    }

    public static void setUseAsEditorOnly(boolean bEditor) {
        bUseAsEditor = bEditor;
    }

    public static boolean getUseAsEditorOnly() { return bUseAsEditor; }
}
