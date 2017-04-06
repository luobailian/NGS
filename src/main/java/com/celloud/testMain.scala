
package com.celloud
import com.celloud.Core.test
import com.celloud.Core.FastQCTrims
import com.celloud.Utils.{Constant, fastqTrimUtil, readFiles}
import java.io.{BufferedReader, File, FileReader}


import org.apache.spark.util.{AccumulatorV2, LongAccumulator}
import org.apache.spark.{SparkConf, SparkContext}

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
    val inputpath = args(2) //文件
    val outputQC = args(3)
    val conf = new SparkConf().setAppName(name).setMaster("local[6]")
    val sc = new SparkContext(conf)
    val nums = "1"::("2"::"3"::"4"::Nil)
    var result = List[String]()

    val fruit1 = "apples" :: ("oranges" :: ("pears" :: Nil))
    val fruit2 = "mangoes" :: ("banana" :: Nil)
    // use two or more lists with ::: operator
    var fruit = fruit1 ::: fruit2
    println( "fruit1 ::: fruit2 : " + fruit )

    // use two lists with Set.:::() method
    fruit = fruit1.:::(fruit2)
    println( "fruit1.:::(fruit2) : " + fruit )

    // pass two or more lists as arguments
    fruit = List.concat(fruit1, fruit2)
    println( "List.concat(fruit1, fruit2) : " + fruit  )

    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒............................")

  }
  def seq(a:Int,b:Int): Int ={
    if(a>b) a else b
  }
  def combine(a:Int,b:Int): Int ={
    a+b
  }

}

