package com.celloud.Core

import java.text.DecimalFormat

import com.celloud.Utils.{QCUtil, fastqTrimUtil, Constant}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by luo on 2017/3/15.
  */
object FastQCTrimsJoin {
  def LoadTrims(inputpath: String, inputpath2: String, core_standard: Int, len_standard: Int, outputPath:
  String, outputPath2: String, outputQC: String, isLocal: Boolean, QCandTrim: Int,partitionNum:Int): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    val name = "FastQCTrims"
    val conf = new SparkConf().setAppName(name)
    if (isLocal) {
      conf.setMaster("local")
    }
    conf.set("spark.Kryoserializer.buffer.max", "2048m")
    val sc = new SparkContext(conf)

    val beginDate = System.currentTimeMillis()
    println("开始启动..................................................................................")

    val accu = ArrayBuffer[String]()
    var linecount = 0
    val scoreCalculation = sc.textFile(inputpath,partitionNum).flatMap { v1line => v1line.split(Constant.linefeed) }.map { lines =>
      val resline = fastqTrimUtil.readlines(lines, accu, linecount)
      linecount = resline._1
      resline._2
    }.filter { x => x.toString().length() > 2 }.take(20)
    //计算打分
    val score=  fastqTrimUtil.getStandardScore(scoreCalculation)
    if(score!=0) {
      if (QCandTrim.equals(1) || QCandTrim.equals(0)) {
        QCCalculation(sc, inputpath, outputQC, score, partitionNum)
      }
      if (QCandTrim.equals(2) || QCandTrim.equals(0)) {
        //Trim
        TrimsCalculation(inputpath, inputpath2, core_standard, len_standard, outputPath, outputPath2,
          isLocal, score, sc, partitionNum)
      }
    }
    //结束
    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒.........."+ score)
    sc.stop()
  }


  //计算QC
  def QCCalculation(sc: SparkContext, inputpath: String, outputPath: String,standCoreChar:Int,partitionNum:Int): Unit = {
    val  countreads, basescount, gcount,avgValue =sc.longAccumulator
    var maxlen, mixlen = 0
    val resMap = new mutable.LinkedHashMap[Int, mutable.HashMap[Int, Int]]
    val accu = ArrayBuffer[String]()
    var linecount = 0

    val file1 = sc.textFile(inputpath,partitionNum).flatMap { v1line => v1line.split(Constant.linefeed) }
    //开始计算
    val readMap = file1.map { x =>
      val arraysplit = x.toString().split(Constant.linefeed)
      val bases = arraysplit(1).toCharArray //碱基
    val scoreLineArray = arraysplit(3).toCharArray //质量
      if (maxlen==0 || (maxlen < bases.length)) {
        maxlen = bases.length
      }
      if (mixlen == 0 || (mixlen> bases.length)) {
        mixlen = bases.length
      }
      countreads.add(1)//总的记录数
      basescount.add(bases.length)

      var counter = 1
      while (counter <= scoreLineArray.length) {
        val base = bases(counter - 1).toString
        if (base.equals("G") || base.equals("C")) {
          gcount.add(1)
        }
        //位置+~+打分值+|
        val q = (Integer.valueOf(scoreLineArray(counter - 1)) - standCoreChar).toInt
        if (resMap.contains(counter)) {
          //如果结果集中已包含key
          val keyname = resMap.get(counter).get
          if (keyname.contains(q)) {
            //如果包含了值,直接加上
            keyname += (q -> (keyname(q).toInt + 1))
            resMap.put(counter, keyname)
          } else {
            keyname += (q -> 1)
            resMap.put(counter, keyname)
          }
        } else {
          val arr = mutable.HashMap[Int, Int]()
          arr += (q -> 1)
          resMap += (counter -> arr)
        }
        counter = counter + 1
      }
      (mixlen,maxlen, resMap)
    }.collect().last
    /* readMap._3.map{ x=>
       println(x._1+ "------------"+x._2)
     }
 */
    val singMap = sc.parallelize(readMap._3.toList).map { x =>
      val lineres = QCUtil.getLineResultString(x._2)
      avgValue.add(lineres._2.toInt)
      (x._1, lineres._1)
    }.collect()
    resMap.clear()
    //输出
    printlnQC(sc,  readMap._1.toInt, readMap._2.toInt, countreads.value.toInt, basescount.value.toInt,
      gcount.value.toInt, inputpath, outputPath, singMap, avgValue.value.toInt)
  }

  //打印QC
  def printlnQC(sc: SparkContext, mixlen: Int, maxlen: Int, countreads: Int, basescount: Int, gcount: Int,
                inputpath: String,
                outputPath: String, singMap: Array[(Int, String)], avg: Int): Unit = {

    val fasqQCname = inputpath.substring(inputpath.lastIndexOf("/") + 1) //路径
    var i = 0
    val showtablecount = 9
    val df = new DecimalFormat("#.00")
    val per = (gcount.toString().toDouble / basescount.toString().toDouble) * 100
    val percentage = df.format(per)
    val avgTotal = df.format(avg / singMap.size) //统计平均数
    var res = new java.io.FileWriter(outputPath)
    if (mixlen == maxlen)
      res.write("Measure value \nFilename:" + fasqQCname + " \nTotal reads:" + countreads
        + " \nTotal Sequences:" + basescount + " \nSequence length:" + maxlen
        + " \nAverage quality:" + avgTotal + " \n%GC:" + percentage + " \n")
    else
      res.write("Measure value \nFilename:" + fasqQCname + " \nTotal reads:" + countreads
        + " \nTotal Sequences:" + basescount + " \nSequence length:" + mixlen
        + "-" + mixlen + "  \nAverage quality:" + avgTotal + " \n%GC:" + percentage + " \n")

    res.write("----------For Box plot----------\n")
    var buffer = ArrayBuffer[String]()
    val pssingMap = singMap.sortBy(x => x._1)
    while (i < pssingMap.size) {
      val j = pssingMap(i)._1 //行号
      if (j > showtablecount) {
        buffer.append(pssingMap(i)._2) //key值
        if ((j - showtablecount) % 5 == 0) {
          val arr0 = buffer(0).split(Constant.spaceStr)
          val arr1 = buffer(1).split(Constant.spaceStr)
          val arr2 = buffer(2).split(Constant.spaceStr)
          val arr3 = buffer(3).split(Constant.spaceStr)
          val arr4 = buffer(4).split(Constant.spaceStr)
          val age10 = df.format((arr0(0).toDouble + arr1(0).toDouble + arr2(0).toDouble + arr3(0).toDouble + arr4(0).toDouble) / 5) //  10%
          val age25 = df.format((arr0(1).toDouble + arr1(1).toDouble + arr2(1).toDouble + arr3(1).toDouble + arr4(1).toDouble) / 5) //  25%
          val age50 = df.format((arr0(2).toDouble + arr1(2).toDouble + arr2(2).toDouble + arr3(2).toDouble + arr4(2).toDouble) / 5) //  50%
          val age75 = df.format((arr0(3).toDouble + arr1(3).toDouble + arr2(3).toDouble + arr3(3).toDouble + arr4(3).toDouble) / 5) //  75%
          val age90 = df.format((arr0(4).toDouble + arr1(4).toDouble + arr2(4).toDouble + arr3(4).toDouble + arr4(4).toDouble) / 5) //  90%
          val avg = df.format((arr0(5).toDouble + arr1(5).toDouble + arr2(5).toDouble + arr3(5).toDouble + arr4(5).toDouble) / 5) //平均
          res.write(j - 4 + "-" + j + Constant.spaceStr + age10 + Constant.spaceStr + age25 + Constant.spaceStr
            + age50 + Constant.spaceStr + age75 + Constant.spaceStr + age90 + Constant.spaceStr + avg + Constant.linefeed)
          buffer.clear()
        } else if (j == pssingMap.size) {
          //最后一条记录
          res.write(j + "	" + pssingMap(i)._2 + Constant.linefeed)
        }
      } else {
        res.write(j + "	" + pssingMap(i)._2 + Constant.linefeed)
      }

      i += 1

    }
    res.close()
  }


  def TrimsCalculation(inputpath:String, inputpath2: String, core_standard: Int, len_standard: Int, outputPath:
  String, outputPath2: String, isLocal: Boolean,standCoreChar:Int, sc: SparkContext,partitionNum:Int): Unit = {
    val file1 = sc.textFile(inputpath,partitionNum).flatMap { v1line => v1line.split(Constant.linefeed) }
    val file2 = sc.textFile(inputpath2,partitionNum).flatMap { v1line => v1line.split(Constant.linefeed) } //输入文件1  \r

    val accu = ArrayBuffer[String]()
    var linecount = 0
    val res = file1.map { x =>
      val resline = fastqTrimUtil.readAndTrimsCalculation(x,accu,linecount,standCoreChar,core_standard,len_standard)
      linecount=resline._1
      (resline._2, resline._3)
    }.filter { x => x._1.toString().length() > 2 }.join(
    file2.map { x =>
      val resline = fastqTrimUtil.readAndTrimsCalculation(x,accu,linecount,standCoreChar,core_standard,len_standard)
      linecount=resline._1
      (resline._2, resline._3)
    }.filter { x => x._1.toString().length() > 2 }).coalesce(1)
    //打印
    printTrims(res, isLocal, outputPath, outputPath2)
  }
  def printTrims(res: RDD[(String, (String,String))], isLocal: Boolean, outputPath:
  String, outputPath2: String): Unit = {
    val tmppath = outputPath + "_tmp"
    val tmppath2 = outputPath2 + "_tmp"
    //文件1
    val fileres1 = res.map { x =>
      x._2.toString().split(",")(0) //
    }.saveAsTextFile(tmppath)

    //文件2
    val fileres2 = res.map { x =>
      x._2.toString().split(",")(1)
    }.saveAsTextFile(tmppath2)

    /*      Runtime.getRuntime.exec("hadoop fs -mv " + tmppath + "/part-00000" + " " + outputPath).waitFor()
          Runtime.getRuntime.exec("hadoop fs -mv " + tmppath2 + "/part-00000" + "  " + outputPath2).waitFor()
          Runtime.getRuntime.exec("hadoop fs -rmr " + tmppath).waitFor()
          Runtime.getRuntime.exec("hadoop fs -rmr " + tmppath2).waitFor()*/
  }
  def readCompteMap(x:String, standCoreChar: Int,
                    core_standard: Int, len_standard: Int): (String,String) = {
    val arraysplit = x.toString().split(Constant.linefeed)
    val bases = arraysplit(1).toCharArray //碱基
    val scoreLineArrayPair = arraysplit(3).toCharArray //质量
    var lastCharPair = "";
    var secondCharPair = ""
    var flagPair = true
    var counterPair1 = 0
    while (flagPair && counterPair1 < scoreLineArrayPair.length) {
      if (scoreLineArrayPair(counterPair1) - standCoreChar < core_standard) {
        //如果当前质量值-标准值<20,即满足截取条件
        if (counterPair1 != 0 && counterPair1 > len_standard) {
          //如果质量值长度已经满足30个的条件
          lastCharPair = scoreLineArrayPair.subSequence(0, counterPair1).toString //质量截取
          secondCharPair = bases.subSequence(0, counterPair1).toString //碱基截取
        }
        flagPair = false
      } else if (counterPair1 == scoreLineArrayPair.length - 1 && counterPair1 > len_standard) {
        //全部满足的情况,全部截取
        lastCharPair = scoreLineArrayPair.subSequence(0, counterPair1 + 1).toString
        secondCharPair = bases.subSequence(0, counterPair1 + 1).toString
        flagPair = false
      } else {
        counterPair1 += 1
      }
    }
    if (lastCharPair.equals("")) ("!!","!!")
    else{
      val str = arraysplit(0) + Constant.linefeed + secondCharPair + Constant.linefeed + "+" + Constant.linefeed + lastCharPair
      val heads = arraysplit(0).toString
      if(heads.contains(Constant.underline)){
        (heads.split(Constant.underline)(0),str)//下横线
      } else if(heads.contains(Constant.slash)){
        (heads.split(Constant.slash)(0),str) //斜线
      }else{
        (heads.split(Constant.spaceStr)(0),str) //空格
      }
    }
  }
}
