package com.celloud

import com.celloud.Core.FastSNAP
import org.apache.spark.{SparkContext, SparkConf}

/**
  * Created by luo on 2017/3/6.
  */
object SNAPMain {
  def main(args: Array[String]) {
    if (args.length != 8) {
      System.err.println("isLocal(true,false); partition Number; threads Number; snap path; database; " +
        "inputpath1; inputpath2, outputPath")
      System.exit(1)
    }
    assert(args.length > 8, "")
    //是否本地, partition数; 线程数; snap路径; 数据库; 输入文件1; 输入文件2, 输出文件
    val isLocal = args(0).toBoolean
    val partitionNum = args(1).toInt  //partition数量
    val threadsNumber= args(2).toInt  // Number of threads
    val snapPath = args(3).replace("__"," ")
    val database = args(4)
    val inputpath = args(5)
    val inputpath2 = args(6)
    val outputPath = args(7)
    FastSNAP.snapCalculation(isLocal,partitionNum,threadsNumber,snapPath,database,inputpath,inputpath2,outputPath)
  }
}
