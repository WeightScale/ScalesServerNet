package com.kostya.scales_server_net.task;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.kostya.scales_server_net.Globals;
import com.kostya.scales_server_net.Internet;
import com.kostya.scales_server_net.provider.EventsTable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Kostya  on 28.06.2016.
 */
public class IntentServiceHttpPost extends IntentService{
    public static final String filePath = "forms/kolosok.xml";
    public static final String nameForm = "EventsForm";
    public static final String TAG = IntentServiceHttpPost.class.getName();
    public static final String EXTRA_LIST_VALUE_PAIR = "com.kostya.scaleswifinet.task.EXTRA_LIST_VALUE_PAIR";
    public static final String EXTRA_HTTP_PATH = "com.kostya.scaleswifinet.task.EXTRA_HTTP_PATH";
    public static final String ACTION_EVENT_TABLE = "com.kostya.scaleswifinet.task.ACTION_EVENT_TABLE";

    public IntentServiceHttpPost(String name) { super(name);  }
    public IntentServiceHttpPost() { super(IntentServiceHttpPost.class.getName());  }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String action = intent.getAction();
            switch (action){
                case ACTION_EVENT_TABLE:
                    runSendEventTable();
                break;
                default:{
                    Bundle bundle = intent.getExtras();
                    String http = bundle.getString(EXTRA_HTTP_PATH);
                    List<ValuePair> results = bundle.getParcelableArrayList(EXTRA_LIST_VALUE_PAIR);
                    Internet.getConnection(5000, 10);
                    submitData(http, results);
                }
            }



        }catch (Exception e){}

    }

    void runSendEventTable(){

        if (!Internet.getConnection(10000, 10)) {return;}

        try {
            /** Класс формы для передачи данных весового чека. */
            //String path = new SystemTable(getApplicationContext()).getProperty(SystemTable.Name.PATH_FORM);
            //GoogleForms.Form form = new GoogleForms(getApplicationContext().getAssets().open(filePath)).createForm(nameForm);
            //InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(Uri.parse(path));
            //String FilePath = getApplicationContext().getFilesDir() + File.separator + "forms" + File.separator + "form.xml";
            File file = new File(Globals.getInstance().pathLocalForms,"form.xml");
            //Uri uri = Uri.parse(FilePath);
            InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(Uri.fromFile(file));
            //InputStream inputStream = new FileInputStream(path);
            GoogleForms.Form form = new GoogleForms(inputStream).createForm(nameForm);
            Cursor event = new EventsTable(getApplicationContext()).getPreliminary();
            if (event.getCount() > 0) {
                event.moveToFirst();
                if (!event.isAfterLast()) {
                    do {

                        String http = form.getHttp();
                        Collection<BasicNameValuePair> values = form.getEntrys();
                        List<ValuePair> results = new ArrayList<>();

                        for (BasicNameValuePair valuePair : values){
                            try {
                                if(valuePair.getValue().equals(EventsTable.KEY_EVENT)){
                                    int i = event.getInt(event.getColumnIndex(EventsTable.KEY_EVENT));
                                    String ev = EventsTable.Event.values()[i].name();
                                    results.add(new ValuePair(valuePair.getName(), ev));
                                }else
                                    results.add(new ValuePair(valuePair.getName(), event.getString(event.getColumnIndex(valuePair.getValue()))));
                            } catch (Exception e) {}
                        }
                        try {
                            submitData(http, results);
                            int id = event.getInt(event.getColumnIndex(EventsTable.KEY_ID));
                            new EventsTable(getApplicationContext()).updateEntry(id, EventsTable.KEY_STATE, EventsTable.State.CHECK_ON_SERVER.ordinal());
                        }catch (Exception e){
                            Log.e(TAG, e.getMessage());
                        }

                    } while (event.moveToNext());
                }
            }
            event.close();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void submitData(String http_post, List<ValuePair> results) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(http_post);
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, 15000);
        HttpConnectionParams.setSoTimeout(httpParameters, 30000);
        post.setParams(httpParameters);
        post.setEntity(new UrlEncodedFormEntity(results, "UTF-8"));
        HttpResponse httpResponse = client.execute(post);
        if (httpResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK)
            throw new Exception(httpResponse.toString());
        //return httpResponse.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK;
    }

    static class ValuePair extends BasicNameValuePair implements Parcelable{

        public ValuePair(String name, String value) {
            super(name, value);
        }

        protected ValuePair(Parcel in) {
            super(in.readString(), in.readString());
        }

        public static final Creator<ValuePair> CREATOR = new Creator<ValuePair>() {
            @Override
            public ValuePair createFromParcel(Parcel in) {
                return new ValuePair(in);
            }

            @Override
            public ValuePair[] newArray(int size) {
                return new ValuePair[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(getValue());
            parcel.writeString(getName());
        }


        public ValuePair clone() throws CloneNotSupportedException {
            return (ValuePair) super.clone();
        }
    }

    static class GoogleForms {
        private final Document document;

        GoogleForms(Context context, int xmlRawResource) throws IOException, SAXException, ParserConfigurationException {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = documentBuilder.parse(context.getResources().openRawResource(xmlRawResource));
        }

        GoogleForms(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = documentBuilder.parse(inputStream);
        }

        public Form createForm(String name) throws Exception {
            Form form = new Form();
            Node node = document.getElementsByTagName(name).item(0);
            if(node == null)
                throw new Exception("Нет формы с именем " + name + " в файле disk.xml");
            form.setHttp(node.getAttributes().getNamedItem("http").getNodeValue());
            for (int i=0; i < node.getChildNodes().getLength() ; i++){
                Node entrys = node.getChildNodes().item(i);
                if("Entrys".equals(entrys.getNodeName())){
                    for (int e=0; e < entrys.getChildNodes().getLength(); e++){
                        Node table = entrys.getChildNodes().item(e);
                        if("Table".equals(table.getNodeName())){
                            form.setTable(table.getAttributes().getNamedItem("name").getNodeValue());
                            for (int t=0; t < table.getChildNodes().getLength(); t++){
                                Node columns = table.getChildNodes().item(t);
                                if("Columns".equals(columns.getNodeName())){
                                    NamedNodeMap map = columns.getAttributes();
                                    Collection<BasicNameValuePair> collection = new ArrayList<>();
                                    for (int m=0; m < map.getLength(); m++){
                                        collection.add(new BasicNameValuePair(map.item(m).getNodeName(), map.item(m).getNodeValue()));
                                    }
                                    form.setEntrys(collection);
                                    return form;
                                }
                            }
                        }
                    }
                }
            }
            return form;
        }

        public static class Form{
            private String http = "";
            private String table = "";
            private Collection<BasicNameValuePair> entrys = new ArrayList<>();

            public String getHttp() {
                return http;
            }

            public void setHttp(String http) {
                this.http = http;
            }

            public Collection<BasicNameValuePair> getEntrys() {
                return entrys;
            }

            public void setEntrys(Collection<BasicNameValuePair> entrys) {
                this.entrys = entrys;
            }

            public String getTable() {
                return table;
            }

            public void setTable(String table) {
                this.table = table;
            }

            public String getParams(){
                return TextUtils.join(" ", entrys);
            }

            public String[] getArrayParams(){
                String text = getParams();
                return text.split(" ");
            }

        }
    }
}
