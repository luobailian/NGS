package com.celloud
import com.celloud.Core.callSnp
import org.apache.spark.SparkConf

/**
  * Created by luo on 2017/2/16.
  */
object FastCallSNPMain {

  def main(args: Array[String]) {
    val isLocal = args(0).toBoolean
    assert(args.length>=6,"isLocal,ref,read,output,要过滤的质量值,碱基质量数-值")
    //args(0)标准文件,args(1)样本文件,args(2)输出路径,args(3)要过滤的质量值,args(4)碱基质量数-值
    val refFilePath  = args(1)
    val readFilePath = args(2)
    val outputPath = args(3)
    val qualitycount = args(4).toInt
    val qvalue = args(5).toInt //判断碱基质量数

    callSnp.callsnp(refFilePath,readFilePath,outputPath,qualitycount,qvalue,isLocal)
  }
}
