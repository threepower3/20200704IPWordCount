


import com.alibaba.fastjson.JSONObject;


import java.io.*;


/**
 * tq
 * 2020/7/4
 * 12:37
 */
public class JsonParse {


    public static void main(String[] args) {


        File file = new File("20200704IPWordCount/src/data/nginx_http.212.2019090600.log");
        try {

            Reader reader = new BufferedReader(new FileReader(file));
            String line;
            String jsonString;
            while ((line=((BufferedReader) reader).readLine())!=null){
                jsonString="{"+line+"}";


                System.out.println(jsonString);
                System.out.println(JSONObject.parseObject(jsonString).getString("remote_addr"));


            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
