package com.celloud.Core

import com.celloud.Utils.{Constant, fastqTrimUtil}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.log4j.{Logger, Level}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by luo on 2017/3/6.
  */
object FastSNAP {
   def snapCalculation(isLocal:Boolean,partitionNum:Int,threadsNumber:Int,snapPath:String,databasepath:String,
                       inputpath:String,inputpath2:String,outputPath:String): Unit ={
   // Logger.getLogger("org").setLevel(Level.WARN)
     val name = "FastSNAPpipe"
     val conf = new SparkConf().setAppName(name)
     if (isLocal) {
       conf.setMaster("local")
     }
     conf.set("spark.Kryoserializer.buffer.max", "8192m")
     val sc = new SparkContext(conf)

     val beginDate = System.currentTimeMillis()
     println("开始启动..............................................................................")
     val linefeed = Constant.linefeed   //换行符
     val headspilt=Constant.slash
     //snap paired 数据库 –t 每个并行度的线程数  -I -pairedInterleavedFastq -  -o 结果文件
     val commd= snapPath+" paired "+databasepath+" -t "+threadsNumber+"  -I -pairedInterleavedFastq -  -o -sam -"
     val collstr =sc.collectionAccumulator[String]

     val file1 = sc.textFile(inputpath,partitionNum).flatMap { v1line => v1line.split(linefeed) }
     val file2 = sc.textFile(inputpath2,partitionNum).flatMap { v1line => v1line.split(linefeed) }
    val accu = ArrayBuffer[String]()
     var linecount = 1

val res = file1.union(file2).map { lines =>
  val resline =fastqTrimUtil.readlines(lines,accu,linecount)
  linecount=resline._1
  resline._2
}.filter { x => x.toString().length() > 2 }.groupBy { x =>
  x.toString().split(linefeed)(0).split(headspilt)(0)
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
     }.pipe(commd)
  //.coalesce(1, true).filter(x=> x.split(Constant.tab)(2).toString!="*").distinct()
     .saveAsTextFile(outputPath)




   /* .foreachPartition{ x =>
      var result = List[String]()
        var strtmp = ""
       while(x.hasNext){
         val p = x.next()
         strtmp += p(0)+ Constant.linefeed+p(1)+ Constant.linefeed
       }
      collstr.add(strtmp)
    }
     val count=collstr.value.size()
     var p=0
    while(p<count){
      val output = outputPath.substring(0,outputPath.length-4)+p+outputPath.substring(outputPath.length-4,outputPath.length)
      val commd= snapPath+" paired "+database+" -t "+threadsNumber+"  -I -pairedInterleavedFastq -  -o "+ output+p +""
     println("执行命令------------"+commd)
      val str = collstr.value.get(p)
      sc.parallelize(str).map { x =>
       val line= x.toString().split(Constant.tab)
        line(0)+ Constant.linefeed+ line(1)+ Constant.linefeed+  line(2) + Constant.linefeed+  line(3)
      }.pipe(commd).first()
      p += 1
    }*/
     //结束
     val endDate = System.currentTimeMillis()
     println("结束,计算的执行时间" + ((endDate - beginDate) / 1000) + "秒.................................")
     sc.stop()
   }
}
class FastSNAP extends Serializable{

   def pipecommd(sc:SparkContext,x: Any): Unit ={

     sc.parallelize(x.toString).foreach(x=> println(x))
   }
}
