package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;


    SQLiteDatabase articlesDB;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB .execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articlesId, INTEGER , title VARCHAR, content VARCHAR)");










        DownloadTask task = new DownloadTask();
        try{
           task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }







        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this ,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);





        //to move to new acivity

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(),articleActivity.class);
                intent.putExtra("content",content.get(i));
                startActivity(intent);
            }
        });



        updateListView();

    }



    public void updateListView(){
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles",null);
        int ContentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if (c.moveToFirst()){
            titles.clear();
            content.clear();


            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(ContentIndex));
            }while (c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }

    }







    //Todo Json

    public class DownloadTask extends AsyncTask<String , Void ,String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url ;
            HttpURLConnection urlConnection = null;
            try{

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader= new InputStreamReader(inputStream);
                int data = inputStreamReader.read();
                while (data != -1){
                    char current = (char) data;
                    result +=current;
                    data = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int numberOfItem = 20;
                if (jsonArray.length() < 20){
                    numberOfItem = jsonArray.length();
                }





                articlesDB.execSQL("DELETE FROM articles");


                //for id


                for (int i = 0 ; i<numberOfItem ; i++){
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                     inputStream = urlConnection.getInputStream();
                     inputStreamReader= new InputStreamReader(inputStream);
                    data = inputStreamReader.read();
                    String articleInfo = "";
                    while (data != -1){
                        char current = (char) data;
                        articleInfo +=current;
                        data = inputStreamReader.read();
                    }




                    //for title and url



                    JSONObject jsonObject = new JSONObject(articleInfo);
                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                       url = new URL(articleUrl);
                       urlConnection = (HttpURLConnection)url.openConnection();
                       inputStream = urlConnection.getInputStream();
                       inputStreamReader = new InputStreamReader(inputStream);
                       data = inputStreamReader.read();
                       String articleContent = "";
                       while(data != -1 ){
                           char current = (char) data;
                           articleContent += current;
                           data = inputStreamReader.read();
                       }
                       Log.i("HTML",articleContent);











                       //Todo SQ

                        String sql = "INSERT INTO articles (articlesId , title, content ) VALUES (?,?,?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(1,articleTitle);
                        statement.bindString(1,articleContent);
                        statement.execute();

                    }
                }
                Log.i("URL",result);
                return result;
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}
