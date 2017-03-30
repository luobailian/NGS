package com.celloud.NewApiInputFormat;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import java.io.IOException;

public class CustomInputFormat extends FileInputFormat<Text, Text> {

    @Override
    public RecordReader<Text, Text> createRecordReader(InputSplit split,TaskAttemptContext context) throws IOException,
            InterruptedException {
        return new CustomReader();
    }
}
