package com.celloud.NewApiInputFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

import java.io.IOException;


public class CustomReader extends RecordReader<Text, Text> {
    private LineReader lr ;
   // private  MylineReader lr;
    private Text key = new Text();
    private Text value = new Text();
    private long start ;
    private long end;
    private long currentPos;
    private Text line = new Text();
    @Override
    public void initialize(InputSplit inputSplit, TaskAttemptContext cxt)throws IOException, InterruptedException {
        FileSplit split =(FileSplit) inputSplit;
        Configuration conf = cxt.getConfiguration();
        Path path = split.getPath();
        FileSystem fs = path.getFileSystem(conf);
        FSDataInputStream is = fs.open(path);
        lr = new LineReader(is,conf);
        // 处理起始点和终止点
        start =split.getStart();
        long x = start;
        end = start + split.getLength();
        is.seek(start);
        if(start != 0){
            boolean flag = true;
            do{
                start += lr.readLine(line);
                /**
                 *  大数据的分片会造成处理单元不完整(每四行为一处理单元),以下逻辑功能为去掉inputSplit头部的不完整处理单元:
                 *  1.拿到@开头的行(这种情况不是新记录的第一行就是上条记录的第四行),假如此时读到第一行;
                 *  2.继续读3行,如果第二行长度与第四行不符,说明@开头的不是第一行,即为第四行,此时已经多读了三行,需将读头撤回到三行以前的位置;
                 *  3.如果第二行与第四行长度相符,说明假设成立,此时已经多读了四行,需将读头撤回到四行以前的位置;
                 *
                 * */
                if(!line.toString().equals("") && (line.toString().substring(0,1)).equals("@")){//如果捕捉到@开头的行,准备退出循环
                    flag = false;
                    int currentLineLen = line.getLength();
                    lr.readLine(line);//读其第二行(碱基行)
                    String atcg = line.toString();
                    lr.readLine(line);//读其第三行(加号行)
                    lr.readLine(line);//读其第四行(质量值行)
                    String atcgQuality = line.toString();
                    is.close();

                    if(atcg.length() != atcgQuality.length()){
                        FSDataInputStream is2 = fs.open(path);//申请新流
                        lr = new LineReader(is2,conf);//新reader
                        is2.seek(start - 1);//新流设定读头位置
                        currentPos = start - 1;
                    }else{
                        FSDataInputStream is2 = fs.open(path);//申请新流
                        lr = new LineReader(is2,conf);//新reader
                        is2.seek(start - currentLineLen - 1);//新流设定读头位置
                        currentPos = start - currentLineLen -1;
                    }
                }
            }while (flag);
        }
    }
    // 针对每行数据进行处理
    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {

            if(currentPos > end){
                return false;
            }
            StringBuffer sb = new StringBuffer();
            //----------------------------------------
            currentPos += lr.readLine(line);//读取第一行数据
            if(line.getLength() == 0 || currentPos > end){
                return false;
            }
            sb.append(line.toString()+"\t");
            //----------------------------------------
            currentPos += lr.readLine(line);//读取第二行数据
            String secondLine = line.toString();
            if(line.getLength() == 0 || currentPos > end){
                return false;
            }
            sb.append(secondLine+"\t");
            //----------------------------------------
            currentPos += lr.readLine(line);//读取第三行数据
            if(line.getLength() == 0 || currentPos > end){
                return false;
            }
            sb.append(line.toString()+"\t");
            //----------------------------------------
            currentPos += lr.readLine(line);//读取第四行数据,若第四行数据不为零但是不等于第二行长度,说明为结尾的残缺记录,丢掉
            if(line.getLength() == 0 || line.getLength() != secondLine.length()){
                return false;
            }
            sb.append(line.toString());
            //----------------------------------------
            value.set(sb.toString());
            key.set("");
            return true;

        //        if(currentPos > end){
        //            return false;
        //        }
        //        currentPos += lr.readLine(line);
        //        if(line.getLength()==0){
        //            return false;
        //        }
        //        key.set("");
        //        value.set(line);
        //        return true;
    }

    @Override
    public Text getCurrentKey() throws IOException, InterruptedException {
        return key;
    }

    @Override
    public Text getCurrentValue() throws IOException, InterruptedException {
        return value;
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
        if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, (currentPos - start) / (float) (end - start));
        }
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        lr.close();
    }
}
