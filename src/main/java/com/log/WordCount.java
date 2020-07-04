package com.log;

import com.alibaba.fastjson.JSONObject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.lib.InverseMapper;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.File;
import java.io.IOException;


/**
 * 一共两个mapreduce job，一个做wordcount，一个做排序
 */
public class WordCount {


    public static class MyMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

        //创建map端输出的key和value
        Text mapOutKey = new Text();
        IntWritable mapOutValue = new IntWritable();

        @Override
        protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, IntWritable>.Context context)
                throws IOException, InterruptedException {

            //获取读取的每一行字符串
            String line = value.toString();
            //给每行的字符串加上{}变成json字符串
            String jsonString = "{" + line + "}";

            //获取ip地址
            String ip = JSONObject.parseObject(jsonString).getString("remote_addr");

            //smap端输出key为IP地址，value为1
            mapOutKey.set(ip);
            mapOutValue.set(1);
            context.write(mapOutKey, mapOutValue);
        }
    }


    public static class MyReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

        //定义reduce端输出value
        IntWritable outValue = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {


            int nums = 0;
            //相同ip地址的出现次数相加
            for (IntWritable value : values) {
                nums += 1;
            }

            outValue.set(nums);
            //输出ip地址、出现次数（此处还未排序）
            context.write(key, outValue);
        }
    }


    //在map端排序
    public static class SortMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

        IntWritable outKey = new IntWritable();
        Text outValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            //得到wordcount输出的结果集
            String[] line = value.toString().split("\t");

            //得到ip地址
            String ip = line[0];
            //得到ip出现次数
            int nums = Integer.valueOf(line[1]);
            //ip出现次数为key，ip地址为value
            outKey.set(nums);
            outValue.set(ip);
            //输出
            context.write(outKey, outValue);
        }
    }

    //在reduce端交换ip地址和出现次数的位置
    public static class SortReducer extends Reducer<IntWritable, Text, Text, IntWritable> {


        @Override
        protected void reduce(IntWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            for (Text outKey : values) {
                ///输出ip地址、出现次数
                context.write(outKey, key);
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {

        //定义wordcount和排序的输入、输出路径
        String wcJobInput;
        String wcJobOut;
        String sortJobInput;
        String sortJobOut;

        //在集群运行方法为 hadoop jar xxx.jar com.log.WordCount 数据输入路径 中间结果输出路径 结果输出路径
        if (args.length != 3) {
            wcJobInput = "20200704IPWordCount/src/data/nginx_http.212.2019090600.log";
            wcJobOut = "20200704IPWordCount/src/out";
            sortJobInput = wcJobOut;
            sortJobOut = "20200704IPWordCount/src/out2";
            System.err.print("请依次输入数据源地址，wordcount结果输出地址，排序结果输出地址");
        } else {
            wcJobInput = args[0];
            wcJobOut = args[1];
            sortJobInput = wcJobOut;
            sortJobOut = args[2];

        }


        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf);
        job.setJarByClass(WordCount.class);

        job.setNumReduceTasks(1);
        job.setMapperClass(MyMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);


        job.setReducerClass(MyReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);


        //运行前删除输出路径
        FileUtil.fullyDelete(new File(wcJobOut));

        //数据输入路径
        FileInputFormat.setInputPaths(job, wcJobInput);

        //数据输出路径
        FileOutputFormat.setOutputPath(job, new Path(wcJobOut));

        boolean result1 = job.waitForCompletion(true);
        //true 成功，false 失败
        System.out.println(result1);


        Job sortJob = Job.getInstance(conf);
        sortJob.setMapperClass(SortMapper.class);
        sortJob.setMapOutputKeyClass(IntWritable.class);
        sortJob.setMapOutputValueClass(Text.class);

        sortJob.setReducerClass(SortReducer.class);
        sortJob.setOutputKeyClass(Text.class);
        sortJob.setOutputValueClass(IntWritable.class);
        //选择排序方式，此处为降序排序
        sortJob.setSortComparatorClass(Comparator.class);

        //运行前删除输出路径
        FileUtil.fullyDelete(new File(sortJobOut));

        //数据输入路径
        FileInputFormat.setInputPaths(sortJob, sortJobInput);

        //数据输出路径
        FileOutputFormat.setOutputPath(sortJob, new Path(sortJobOut));

        boolean result2 = sortJob.waitForCompletion(true);
        System.out.println(result2);

    }
}
