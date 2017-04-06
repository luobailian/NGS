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
       threadsNumber, snapPath,databasepath,samtoolsPath,outputpath,linefeed,headspilt)
    }
    //结束
    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒.........."+ score)
    sc.stop()
  }
  def TrimsCalculation( inputpath: String, inputpath2: String, core_standard: Int, len_standard: Int,
                        isLocal: Boolean,standCoreChar:Int, sc: SparkContext,partitionNum:Int,threadsNumber:Int,
  snapPath: String,databasepath: String,samtoolsPath:String,outputpath:String,
                        linefeed: String,headspilt: String): Unit = {
    val spiltstr=Constant.tab
    val file1 = sc.textFile(inputpath,partitionNum).flatMap { v1line => v1line.split(linefeed) }
    val file2 = sc.textFile(inputpath2,partitionNum).flatMap { v1line => v1line.split(linefeed) } //输入文件1  \r
    val accu = ArrayBuffer[String]()
    var linecount = 0
    //snap paired 数据库 –t 每个并行度的线程数  -I -pairedInterleavedFastq -  -o 结果文件
    val commd= snapPath+" paired "+databasepath+" -t "+threadsNumber+"  -I -pairedInterleavedFastq -  -o -sam -"
    val samtoolcommond = samtoolsPath+" -"

    //开始计算
    val disRDD = file1.union(file2).map { lines =>
      val resline = fastqTrimUtil.readlines(lines, accu, linecount)
      linecount = resline._1
      resline._2
    }.filter { x => x.toString().length() > 2 }.groupBy { x =>
      x.toString().split(linefeed)(0).split(headspilt)(0)  //下横线
    }.filter(x =>
      x._2.count { x => true } == 2
    ).map { x =>
      var str=""
      val linestr = x._2.toList(0) //
    val linestr2 = x._2.toList(1)
      val length1 = linestr.split(linefeed)(0).split(headspilt)(1).substring(0, 1)
      val length2 = linestr2.split(linefeed)(0).split(headspilt)(1).substring(0, 1)
      if (length1.equals("1")) str += linestr + linefeed
      if (length2.equals("1")) str += linestr2 + linefeed //2的文件
      if (length1.equals("2")) str += linestr //1的文件
      if (length2.equals("2")) str += linestr2 //2的文件
      str
    }.pipe(commd).filter(x=> x.split(spiltstr)(2).toString!="*").coalesce(1).cache()   //.coalesce(1)

    val titleRDD= disRDD.filter(x=> x.startsWith("@")).distinct().sortBy(_.split(spiltstr)(1)).collect()
    println("------------客户端设置的partition------------"+partitionNum)

    var strBuf = new StringBuffer()
    for (i <- titleRDD.indices) {
      val lines = titleRDD(i)+ Constant.linefeed
      strBuf.append(lines)
    }
    val titlesort = strBuf.toString.replace("unsorted","sorted")
    val broadcastStr= sc.broadcast(titlesort.substring(0,titlesort.length- Constant.linefeed.length))

    val groupsSort = disRDD.filter(x=> !x.startsWith("@"))
      .filter(x=> x.split(spiltstr)(2).toString!="*")
      .groupBy(_.split(spiltstr)(2)).sortBy(_._1)
    disRDD.unpersist()  //清除缓存

    groupsSort.mapPartitions({ iter =>
      var result = List[String]()
      var reslist=""
      while (iter.hasNext){
        val p = iter.next()._2.toList
        var c = 0
        val sortValues= p.sortBy(_.split(spiltstr)(3).toInt).take(p.size)
        var strres = new StringBuffer()
        while(c< sortValues.size){
          strres.append(sortValues(c)+Constant.linefeed )
          c+=1
        }
        //val reslist =strres.toString.substring(0,strres.length- Constant.linefeed.length)
        reslist+= strres.toString
      }
      result ::= (reslist.toString.substring(0,reslist.length- Constant.linefeed.length))
      result ::= (broadcastStr.value.toString) //头
      result.iterator
    },true)
      .pipe(samtoolcommond)
      .saveAsTextFile(outputpath)
  }
}
