package com.celloud.Core

import com.celloud.Utils.{Constant, customPartitions}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{HashPartitioner, Partitioner, SparkConf, SparkContext}

/**
  * Created by luo on 2017/3/22.
  */
object callSnpPipe {
  def LoadCallSnpPipe(isLocal: Boolean,partitionNum:Int,inputpath: String,samtoolsPath: String, outputpath: String): Unit ={
    Logger.getLogger("org").setLevel(Level.WARN)
    val name = "callSnpPipe"
    val conf = new SparkConf().setAppName(name)
    if (isLocal) {
      conf.setMaster("local[8]")
    }
    conf.set("spark.Kryoserializer.buffer.max", "8192m")
    val sc = new SparkContext(conf)

    val beginDate = System.currentTimeMillis()
    println("开始启动..................................................................................")
    //命令 /share/biosoft/Software/samtools-1.2/samtools mpileup__-f__/share/biosoft/Database/HG_19/hg19.fasta -
    val linefeed = Constant.linefeed   //换行符
    val spiltstr=Constant.tab
    val samtoolcommond = samtoolsPath+" -"

    val disRDD = sc.textFile(inputpath,partitionNum).flatMap { v1line => v1line.split(Constant.linefeed) }.cache()
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
        reslist+= strres.toString
      }
      result ::= (reslist.toString.substring(0,reslist.length- Constant.linefeed.length))
      result ::= (broadcastStr.value.toString) //头
      result.iterator
    },true)
      .pipe(samtoolcommond)
    .saveAsTextFile(outputpath)

  /*  var i = -1
   val samtoolRDD2 = disRDD.filter(x=> !x.startsWith("@"))
      .filter(x=> x.split(hspilt)(2).toString!="*")
      .sortBy(_.split(hspilt)(3).toInt)
     .groupBy(_.split(hspilt)(2))
     .map{x=>
     val p = x._2.toList
     var strres = broadcastStr.value.toString
     var c = 0
     while(c< p.size){
       strres+= Constant.linefeed + p(c)
       c+=1
     }
     i+=1
     (i,strres)
   }.persist(StorageLevel.MEMORY_AND_DISK)
    samtoolRDD2.checkpoint()
    samtoolRDD2.partitionBy(new customPartitions(2)).foreach(println)*/
      //.saveAsTextFile(outputpath)


    //结束
    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒..........")
    sc.stop()
  }
}
class callSnpPipe extends Serializable{
   def callpipe(x:List[String],sc:SparkContext): Unit ={
     sc.makeRDD(x)
   }
}
