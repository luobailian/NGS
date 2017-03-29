package com.celloud.Core

import com.celloud.Utils.{fastqTrimUtil, Constant}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkContext, SparkConf}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by luo on 2017/3/9.
  */
object NGSCore {
  def NGSCalculation(isLocal: Boolean,partitionNum:Int,core_standard: Int, len_standard: Int, threadsNumber:Int,
        snapPath: String,databasepath: String,inputpath: String, inputpath2: String,
                     outputQC:String,samtoolsPath:String,outputpath:String): Unit = {

    val name = "NGS"
    val conf = new SparkConf().setAppName(name)
    if (isLocal) {
      conf.setMaster("local")
    }
    conf.set("spark.Kryoserializer.buffer.max", "4096m")
    val sc = new SparkContext(conf)

    val beginDate = System.currentTimeMillis()
    println("开始启动..................................................................................")
    val linefeed = Constant.linefeed   //换行符
    val accu = ArrayBuffer[String]()
    var linecount = 0
    val file1 = sc.textFile(inputpath).flatMap { v1line => v1line.split(linefeed) }.map { lines =>
      val resline = fastqTrimUtil.readlines(lines, accu, linecount)
      linecount = resline._1
      resline._2
    }.filter { x => x.toString().length() > 2 }.cache()
    val pc =file1.take(20)
    //头分隔符
    var headspilt=fastqTrimUtil.getheadspilt(pc,linefeed)
    //计算打分
    val score=  fastqTrimUtil.getStandardScore(pc)
    if(score!=0) {
      FastQCTrims.QCCalculation(file1, sc, inputpath, outputQC, score, partitionNum,linefeed,headspilt)
      //Trim
     TrimsCalculation(inputpath,inputpath2, core_standard, len_standard,  isLocal, score, sc, partitionNum,
       threadsNumber, snapPath,databasepath,samtoolsPath,outputpath)
    }
    //结束
    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒.........."+ score)
    sc.stop()
  }
  def TrimsCalculation( inputpath: String, inputpath2: String, core_standard: Int, len_standard: Int,
                        isLocal: Boolean,standCoreChar:Int, sc: SparkContext,partitionNum:Int,threadsNumber:Int,
  snapPath: String,databasepath: String,samtoolsPath:String,outputpath:String): Unit = {
    val file1 = sc.textFile(inputpath,partitionNum).flatMap { v1line => v1line.split(Constant.linefeed) }
    val file2 = sc.textFile(inputpath2,partitionNum).flatMap { v1line => v1line.split(Constant.linefeed) } //输入文件1  \r
    val accu = ArrayBuffer[String]()
    var linecount = 0
    //snap paired 数据库 –t 每个并行度的线程数  -I -pairedInterleavedFastq -  -o 结果文件
    val commd= snapPath+" paired "+databasepath+" -t "+threadsNumber+"  -I -pairedInterleavedFastq -  -o -sam -"
    //开始计算
    val disRDD = file1.union(file2).map { lines =>
      val resline = fastqTrimUtil.readlines(lines, accu, linecount)
      linecount = resline._1
      resline._2
    }.filter { x => x.toString().length() > 2 }.groupBy { x =>
      val heads = x.toString().split(Constant.linefeed)(0)
      if(heads.contains(Constant.underline)){
        heads.split(Constant.underline)(0)  //下横线
      } else if(heads.contains(Constant.slash)){
        heads.split(Constant.slash)(0)  //斜线
      }else{
        heads.split(Constant.spaceStr)(0)   //空格
      }
    }.filter(x =>
      x._2.count { x => true } == 2
    ).map { x =>
      var str=""
      val linestr = x._2.toList(0) //
    val linestr2 = x._2.toList(1)
      val length1 = linestr.split(Constant.linefeed)(0).split(Constant.slash)(1).substring(0, 1)
      val length2 = linestr2.split(Constant.linefeed)(0).split(Constant.slash)(1).substring(0, 1)
      if (length1.equals("1")) str += linestr + Constant.linefeed
      if (length2.equals("1")) str += linestr2 + Constant.linefeed //2的文件
      if (length1.equals("2")) str += linestr //1的文件
      if (length2.equals("2")) str += linestr2 //2的文件
      str
    }.pipe(commd).coalesce(1).distinct().filter(x=> x.split(Constant.tab)(2).toString!="*").cache()
    disRDD.count()//缓存

    val samtoolRDD = disRDD.filter(x=> !x.startsWith("@"))
      .sortBy(_.split(Constant.tab)(3).toInt)
      .groupBy(_.split(Constant.tab)(2))
      .collect()
    val titleRDD= disRDD.filter(x=> x.startsWith("@")).collect()

    //命令 /share/biosoft/Software/samtools-1.2/samtools mpileup  -f /share/biosoft/Database/HG_19/hg19.fasta -
     val samtoolcommond = samtoolsPath+" -"
    val size= samtoolRDD.length
    var i=0
    while(i<size){
      //加入头文件
      sc.makeRDD(titleRDD).union(sc.makeRDD(samtoolRDD(i)._2.toList)).coalesce(1).pipe(samtoolcommond).saveAsTextFile(outputpath+i)
      i+=1
    }
  }
}
