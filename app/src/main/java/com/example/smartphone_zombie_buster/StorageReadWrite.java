package com.example.smartphone_zombie_buster;
/*参考： https://akira-watson.com/android/gps-background.html*/
import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.text.format.DateFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

class StorageReadWrite {

    private File file;
    private StringBuffer stringBuffer;
    private String currentDay;
    private Context ctx;
    private int distance;

    StorageReadWrite(Context context) {
        ctx = context;
        File path = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        currentDay = DateFormat.format("yyyyMMdd", Calendar.getInstance()).toString();
        file = new File(path, currentDay+".txt");
        distance = getDistance();
    }

    void clearFile(){
        // ファイルをクリア
        writeFile("", false);

        // StringBuffer clear
        if(stringBuffer!=null) {
            stringBuffer.setLength(0);
        }
    }

    // ファイルを保存
    void writeFile(String gpsLog, boolean mode) {

        String saveLog = gpsLog;
        String[] saveStr = gpsLog.split(",");
        if(!saveStr[0].equals(currentDay))
        {
            File path = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            currentDay = DateFormat.format("yyyyMMdd", Calendar.getInstance()).toString();
            file = new File(path, currentDay+".txt");
        }
        if(mode){
            String[] lastLine = getLastLine();
            if(lastLine!=null) {
                double lastLatitude = Double.parseDouble(lastLine[2]);
                double lastLongitude = Double.parseDouble(lastLine[3]);
                double currentLatitude = Double.parseDouble(saveStr[2]);
                double currentLongitude = Double.parseDouble(saveStr[3]);
                if(Integer.parseInt(lastLine[1]) == Integer.parseInt(saveStr[1])) {
                    float[] results = new float[3];
                    Location.distanceBetween(lastLatitude, lastLongitude, currentLatitude, currentLongitude, results);
                    distance +=(int) results[0];
                }
                saveLog+=","+distance+"\n";
            }
            else
            {
                saveLog+=",0\n";
            }
        }

        if(isExternalStorageWritable()){
            try(FileOutputStream fileOutputStream =
                        new FileOutputStream(file, mode);
                OutputStreamWriter outputStreamWriter =
                        new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                BufferedWriter bw =
                        new BufferedWriter(outputStreamWriter);
            ) {

                bw.write(saveLog);
                bw.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    // ファイルを読み出し
    String readFile() {
        stringBuffer = new StringBuffer();

        // 現在ストレージが読出しできるかチェック
        if(isExternalStorageReadable()){

            try(FileInputStream fileInputStream =
                        new FileInputStream(file);

                InputStreamReader inputStreamReader =
                        new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);

                BufferedReader reader=
                        new BufferedReader(inputStreamReader) ) {

                String lineBuffer;

                while( (lineBuffer = reader.readLine()) != null ) {
                    stringBuffer.append(lineBuffer);
                    stringBuffer.append(System.getProperty("line.separator"));
                }

            } catch (Exception e) {
//                stringBuffer.append("error: FileInputStream");
                stringBuffer.append("");
                e.printStackTrace();
            }
        }

        return stringBuffer.toString();
    }

    int getDistance(){
        int dist = 0;
        String[] lastLine = getLastLine();
        if(lastLine!=null) {
            dist = Integer.parseInt(lastLine[4]);
        }
        return dist;
    }

    String[] getLastLine(){
        if(!readFile().equals("")) {
            String[] savedStr = readFile().split("\n");
            return savedStr[savedStr.length - 1].split(",");
        }
        else
        {
            return null;
        }
    }

    List<double[]> getPositions(){
        String[] lines = readFile().split("\n");
        List<double[]> pos = new ArrayList<double[]>();
        if(lines!=null) {
            for(int i=0;i<lines.length;i++) {
                if(!lines[i].equals("")) {
                    double[] where = {0, 0, 0};
                    where[0] = Double.parseDouble(lines[i].split(",")[1]);
                    where[1] = Double.parseDouble(lines[i].split(",")[2]);
                    where[2] = Double.parseDouble(lines[i].split(",")[3]);
                    pos.add(where);
                }
            }
            return pos;
        }
        else
        {
            return null;
        }
    }

    int getId(){
        if(!readFile().equals("")) {
            String[] savedStr = readFile().split("\n");
            return Integer.parseInt(savedStr[savedStr.length - 1].split(",")[1]);
        }
        else
        {
            return -1;
        }
    }



    /* Checks if external storage is available for read and write */
    boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    /* Checks if external storage is available to at least read */
    boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }
}
