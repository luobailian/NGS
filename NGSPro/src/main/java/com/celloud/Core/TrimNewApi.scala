package com.celloud.Core

import com.celloud.NewApiInputFormat.CustomInputFormat
import com.celloud.Utils.Constant
import com.celloud.Utils.Constant
import com.celloud.Utils.fastqTrimUtil
import org.apache.hadoop.io.Text
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by luo on 2017/2/24.
  */
object TrimNewApi {
  def LoadQCTrims(inputpath: String,inputpath2: String,core_standard:Int,len_standard:Int,outputPath:
  String,outputPath2: String,isLocal:Boolean): Unit = {
    val name = "FastQCTrims"
    val conf = new SparkConf().setAppName(name)
    if (isLocal) {
      conf.setMaster("local")
    }
    val beginDate = System.currentTimeMillis()
    println("开始启动..................................................................................")
    val sc = new SparkContext(conf)
    val file = sc.newAPIHadoopFile[Text, Text, CustomInputFormat](Constant.maserURI_Master+ inputpath)
    val file2 = sc.newAPIHadoopFile[Text, Text, CustomInputFormat](Constant.maserURI_Master+ inputpath2)
    val standCoreChar = fastqTrimUtil.getStandardScoreAPI(file)
    if(standCoreChar != 0) {
      val tmppath = outputPath+"_tmp"
      val tmppath2 = outputPath2+"_tmp"
     val res= file.union(file2).map(x => x._2.toString).map{ x =>
        val arraysplit = x.toString().split("\t")
        val bases =arraysplit(1).toCharArray  //碱基
      val scoreLineArrayPair= arraysplit(2).toCharArray //质量
        var lastCharPair=""; var secondCharPair=""
        var flagPair = true
        var counterPair1 = 0
        while(flagPair && counterPair1 < scoreLineArrayPair.length){
          if(scoreLineArrayPair(counterPair1) - standCoreChar.toString().toInt < core_standard){//如果当前质量值-标准值<20,即满足截取条件
            if(counterPair1 != 0 && counterPair1 > len_standard){//如果质量值长度已经满足30个的条件
              lastCharPair = scoreLineArrayPair.subSequence(0,counterPair1).toString//质量截取
              secondCharPair = bases.subSequence(0,counterPair1).toString//碱基截取
            }
            flagPair = false
          }else if(counterPair1 == scoreLineArrayPair.length-1&& counterPair1 > len_standard){//全部满足的情况,全部截取
            lastCharPair = scoreLineArrayPair.subSequence(0,counterPair1+1).toString
            secondCharPair = bases.subSequence(0,counterPair1+1).toString
            flagPair = false
          }else{
            counterPair1 += 1
          }
        }
        if(lastCharPair.equals(""))  "!!" else arraysplit(0)+"\n"+secondCharPair+"\n"+"+"+"\n"+lastCharPair
      }.filter(line => !line.startsWith("!!")).groupBy { x =>
        //根据头文件截取最后的文件
          x.toString().split("\n")(0).split(Constant.spaceStr)(0)
      }.filter(x =>
        x._2.count { x => true }==2
      )

    }
    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间"+((endDate-beginDate)/1000)+"秒.......................................")

  }

}
