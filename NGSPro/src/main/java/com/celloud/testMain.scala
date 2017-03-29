
package com.celloud
import com.celloud.Core.test
import com.celloud.Core.FastQCTrims
import com.celloud.Utils.{Constant, readFiles, fastqTrimUtil}
import java.io.{BufferedReader, FileReader, File}
import org.apache.spark.util.{LongAccumulator, AccumulatorV2}
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by luo on 2017/2/24.
  */
object testMain {
  def main(args: Array[String]): Unit = {

    val beginDate = System.currentTimeMillis()
    println("开始启动..................................................................................")

    val name = "test"

    val masterset = args(0)
   val partitionNum = args(1).toInt
    val inputpath2 = args(2) //文件
    val outputQC = args(3)
    val conf = new SparkConf().setAppName(name).setMaster(masterset)
    val sc = new SparkContext(conf)
    var rdd1 = sc.makeRDD(1 to 5,2)
    var rdd2 = sc.makeRDD(Seq("A","B","C","D","E"),2)

    val p=rdd2.zipWithIndex().collect




    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒............................")

  }
  def mapre(): mutable.Map[String,String] ={
    val t= 22
    mutable.Map("Spark" -> "6")
  }

}

