package com.celloud.Core

import com.celloud.Utils.Constant
import com.celloud.algorithms.HMM
import org.apache.spark.{SparkContext, SparkConf}
import scala.collection.mutable.Map
import scala.collection.mutable

/**
  * Created by luo on 2017/2/22.
  */
object callSnp {
  //ref,read,output文件
  def callsnp(refFilePath:String,readFilePath: String, outputPath: String,qualitycount:Int,qvalue:Int,isLocal:Boolean): Unit = {

    val name = "FastCallSNP"
    val conf = new SparkConf().setAppName(name)
    if (isLocal) {
      conf.setMaster("local")
    }
    val sc = new SparkContext(conf)
    val refline = sc.textFile(refFilePath).flatMap { line => line.split("\r") }
    val readline = sc.textFile(readFilePath).filter(line => line.contains("chr") && !line.contains("*"))
      .flatMap { v1line => v1line.split("\r") }
    val outputfile = outputPath

    //ref文件存入broadcast
    val baseline = refline.collect()
    val baseMap = Map[String, String]()
    for (i <- baseline.indices) {
      val lines = baseline(i).split(Constant.spaceStr)
      val chrname = lines(0)
      val bcontext = lines(1) //内容
      baseMap += (chrname -> bcontext)
    }
    val broadcastArray = sc.broadcast(baseMap)

    val beginDate = System.currentTimeMillis()
    println("开始启动.................................................................................")
    var accuResultMap = new mutable.HashMap[String, Array[Int]]
    var accuResult = new mutable.LinkedHashMap[String, Array[Int]]
    var accu = "" //临时值

    readline.sortBy(x=> x.split("\t")(0) ).foreach { f =>
      val lines = f.split(Constant.spaceStr)
      val chrname = lines(0)
      val position = lines(1).toInt //位置
    val quality = lines(2) //质量值
    val lens = lines(3) //长度
    val readcontent = lines(4).toBuffer
      val singlequality = lines(5) //每个碱基的质量ascii
    val bcArray = broadcastArray.value(chrname)
      val refcon = bcArray.subSequence(position - 1, bcArray.length).toString.toBuffer
      accu = chrname
      //根据ref计算碱基位置
      val lenstr = lens.replace("M", "M ").replace("I", "I ").replace("D", "D ").replace("X", "X ").replace("N", "N ").replace("P", "P ").replace("S", "S ").replace("H", "H ")
      val len = lenstr.split(Constant.spaceStr)
      var refNum = 0
      //var readArray = readcontent.toBuffer; var refArray = refcon.toBuffer
      for (i <- 0 to len.length - 1) {
        val l = len(i)
        val status = l.substring(l.length - 1, l.length) //MID标记
        val lenNum = l.substring(0, l.length - 1)
        refNum += lenNum.toInt //所有的长度
        if (!status.equals("M")) {
          for (t <- 1 to lenNum.toInt) {
            if (status.equals("I")) {
              refcon.insert(refNum.toInt - t, '_') //I修改ref加上_
            } else if (status.equals("D")) {
              readcontent.insert(refNum.toInt - t, '_') //D修改read加上_
            }
          }
        }
      }
      val refcontent = refcon.toArray.subSequence(0, refNum).toString //ref数组加入了_后,长度为M+D+I
    //矩阵
    val hmm = new HMM()
      val midse = hmm.midseMatrix(readcontent.length.toDouble, refcontent.length.toDouble)
      //马尔可夫计算
      val (beforeBAQ, beforeend) = hmm.beforeCompute(refcontent.toArray, readcontent.toArray, singlequality.toArray, qvalue, midse)
      val backBAQ = hmm.backCompute(refcontent.toArray, readcontent.toArray, singlequality.toArray, qvalue, midse)

      //snp计算
      FastCallSNP.callsnpCalculation(refcontent.toArray, readcontent.toArray, singlequality.toArray, chrname, position, beforeend,
        qvalue, qualitycount, accu, accuResult, beforeBAQ, backBAQ)
      if (accuResult.size > 0 && accu != accuResult.keys.toString().split(Constant.spaceStr)(0)) {
        //如果这行不是以前的chr了就清除
        accuResultMap.++(accuResult)
        accuResult.clear()
      }
      accuResultMap
    }
  }
}
