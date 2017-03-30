package com.celloud.Core

import com.celloud.Utils.fastqTrimUtil
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by luo on 2017/3/6.
  */
object test {
  def LoadTrims(inputpath: String, inputpath2: String, core_standard: Int, len_standard: Int, outputPath:
  String, outputPath2: String, outputQC: String, isLocal: Boolean, QCandTrim: Int,partitionNum:Int): Unit = {
   val name = "test"
    val conf = new SparkConf().setAppName(name)
    if (isLocal) {
      conf.setMaster("local")
    }
    conf.set("spark.Kryoserializer.buffer.max", "2048m")
   val sc = new SparkContext(conf)

    val beginDate = System.currentTimeMillis()
    println("开始启动..................................................................................")


    /*val readfile= new readFiles()
    val score = readfile.readsCore(inputpath)*/

    //结束
    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒..........")
    sc.stop()
  }
}
