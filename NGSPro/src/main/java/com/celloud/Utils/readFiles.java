package com.celloud.Utils;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Created by luo on 2017/3/1.
 */
public class readFiles {
    public String readline(String file1Path,String file2Path) throws IOException {

        File file = new File(file1Path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String tempString =null;
        int line = 1;
        int keyline = 0;
        String sb = new String();
        Hashtable hst = new  Hashtable();
        // 一次读入一行，直到读入null为文件结束
        while ((tempString = reader.readLine()) != null) {
            sb +=tempString +"\t";
            if (line % 4 == 0){
                hst.put(keyline,sb.toString());
                keyline++;
                sb ="";
            }
            // 显示行号
            line++;
        }
        reader.close();

        File file2 = new File(file2Path);
        BufferedReader reader2 = new BufferedReader(new FileReader(file2));
        int linecount = 1;
        int keyline2 = 0;
        String sb2 = new String();
        Hashtable hst2 = new  Hashtable();
        String tempString2 =null;
        while ((tempString2 = reader2.readLine()) != null) {
            sb2 += tempString2 +"\t";
            if (linecount % 4 == 0){
                hst2.put(keyline2,sb2.toString());
                keyline2++;
                sb2 ="";
            }
            // 显示行号
            linecount++;
        }
        reader2.close();

        //如果完全相同返回一条记录
        int rescount = 0 ;
        int arrcount = hst.size();
        int arrcount2 = hst2.size();
        int c =0;
        while (c < arrcount){
            String str = hst.get(c).toString();
           if(hst2.contains(str)){
               rescount= rescount+1;
           }
        /*   int t =0;
            while (t < arrcount2){
                String str2 = hst2.get(t).toString();
                if(str.equals(str2)){
                    rescount= rescount+1;
                    break;
                }
                t++;
            }*/
            c++;
        }
        String restr ="--------------表1记录数"+arrcount+"----表2记录数"+arrcount2+"     相同记录数"+rescount;
        return restr;
    }
    public ArrayList readlineArray(String file1Path) throws IOException {
        File file = new File(file1Path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String tempString = null;
        int line = 1;
        String sb = new String();
        ArrayList bd1 = new ArrayList();
        // 一次读入一行，直到读入null为文件结束
        while ((tempString = reader.readLine()) != null) {
            sb += tempString + "\t";
            if (line % 4 == 0) {
                bd1.add(sb.toString());
                sb = "";
            }
            // 显示行号
            line++;
        }
        reader.close();
        return bd1;
    }
    public int readsCore(String file1Path) throws IOException {
        File file = new File(file1Path);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String tempString = null;
        int line = 1;
        int standCoreChar = 0;
        boolean flag = true;
        // 一次读入一行，直到读入null为文件结束
        while ((tempString = reader.readLine()) != null && flag ) {
            if (line % 4 == 0) {
                int counter = 0;
                while (flag && counter < tempString.length()) {
                    int score =tempString.charAt(counter);
                    if (score < '5') {
                        standCoreChar = '!';
                        flag = false;
                    } else if (score > 'T') {
                        standCoreChar = '@';
                        flag = false;
                    } else {
                        counter = counter + 1;
                    }
                }
            }
            // 显示行号
            line++;
        }
        reader.close();
      return   standCoreChar;
    }
}
