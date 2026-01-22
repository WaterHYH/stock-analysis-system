import java.io.*;
import java.net.*;

public class TestAPI {
    public static void main(String[] args) {
        try {
            // 创建URL对象
            URL url = new URL("http://localhost:8080/stock-history/api/update-ma60/sh600685");
            
            // 打开连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // 设置请求方法为POST
            conn.setRequestMethod("POST");
            
            // 设置请求头
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            
            // 允许输出
            conn.setDoOutput(true);
            
            // 发送请求
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // 读取响应
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            // 打印结果
            System.out.println("Response: " + response.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}