package com.celloud.Core


import com.celloud.Utils.Constant
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.util.CollectionAccumulator
import org.apache.spark.{SparkContext, SparkConf}
import scala.collection.mutable.Map
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import  com.celloud.algorithms
import com.celloud.algorithms.HMM
/**
  * Created by luo on 2017/2/16.
  */
class FastCallSNP extends Serializable{



}
object FastCallSNP {
  //ref,read,output文件
  def callsnp(refFilePath:String,readFilePath: String, outputPath: String,qualitycount:Int,qvalue:Int,isLocal:Boolean): Unit ={

    val name ="FastCallSNP"
    val conf = new SparkConf().setAppName(name)
    if(isLocal) {
      conf.setMaster("local")
    }
    val sc = new SparkContext(conf)
    val refline = sc.textFile(refFilePath).flatMap { line => line.split("\r") }
    val readline = sc.textFile(readFilePath).filter(line => line.contains("chr") && !line.contains("*"))
      .flatMap { v1line => v1line.split("\r") }
    val outputfile = outputPath

    //ref文件存入broadcast
    val baseline=refline.collect()
    val baseMap = Map[String,String]()
    for (i <- baseline.indices){
      val lines=baseline(i).split(Constant.spaceStr)
      val chrname=  lines(0)
      val bcontext=  lines(1)//内容
      baseMap += (chrname -> bcontext)
    }
    val broadcastArray = sc.broadcast(baseMap)

    val beginDate = System.currentTimeMillis()
    println("开始启动.................................................................................")
    var accuResultMap =new mutable.LinkedHashMap[String,Array[Int]]
    var accuResult = new mutable.LinkedHashMap[String,Array[Int]]
    var accu ="" //临时值
    val temppath = outputPath.substring(0, outputPath.length()-4)
    val accuResultStr= readline.sortBy(x=> x ).map { f =>
      val lines = f.split(" ")
      val chrname = lines(0)
      val position = lines(1).toInt //位置
    val quality = lines(2) //质量值
    val lens = lines(3) //长度
    val readcontent = lines(4).toBuffer
      val singlequality = lines(5) //每个碱基的质量ascii
    val bcArray = broadcastArray.value(chrname)
      val refcon = bcArray.subSequence(position - 1, bcArray.length).toString.toBuffer
      accu = chrname
      //根据ref计算碱基位置
      val lenstr = lens.replace("M", "M ").replace("I", "I ").replace("D", "D ").replace("X", "X ").replace("N", "N ").replace("P", "P ").replace("S", "S ").replace("H", "H ")
      val len = lenstr.split(Constant.spaceStr)
      var refNum = 0
      //var readArray = readcontent.toBuffer; var refArray = refcon.toBuffer
      for (i <- 0 to len.length - 1) {
        val l = len(i)
        val status = l.substring(l.length - 1, l.length) //MID标记
        val lenNum = l.substring(0, l.length - 1)
        refNum += lenNum.toInt //所有的长度
        if (!status.equals("M")) {
          for (t <- 1 to lenNum.toInt) {
            if (status.equals("I")) {
              refcon.insert(refNum.toInt - t, '_') //I修改ref加上_
            } else if (status.equals("D")) {
              readcontent.insert(refNum.toInt - t, '_') //D修改read加上_
            }
          }
        }
      }
      val refcontent = refcon.toArray.subSequence(0, refNum - 1).toString //ref数组加入了_后,长度为M+D+I
    //矩阵
    val hmm = new HMM()
      val midse = hmm.midseMatrix(readcontent.length.toDouble, refcontent.length.toDouble)
      //马尔可夫计算
      val (beforeBAQ, beforeend) = hmm.beforeCompute(refcontent.toArray, readcontent.toArray, singlequality.toArray, qvalue, midse)
      val backBAQ = hmm.backCompute(refcontent.toArray, readcontent.toArray, singlequality.toArray, qvalue, midse)
      //snp计算
      callsnpCalculation(refcontent.toArray, readcontent.toArray, singlequality.toArray,  chrname, position, beforeend,
        qvalue, qualitycount, accu, accuResult, beforeBAQ, backBAQ)
      if (accuResult.size > 0 && accu != accuResult.keys.toString().split(Constant.spaceStr)(0)) {
        //如果这行不是以前的chr了就清除
        accuResultMap.++(accuResult)
        accuResult.clear()
      }
      accuResultMap
    }.collect().last

    val endDate = System.currentTimeMillis()
    println("结束,计算的执行时间"+((endDate-beginDate)/1000)+"秒.......................................")


    sc.parallelize(accuResultStr.toList).map(y=>{
      y._1+" "+y._2.mkString(" ")
    }).coalesce(1).saveAsTextFile(temppath)
    val conf2 = new org.apache.hadoop.conf.Configuration()
    val fs = FileSystem.get(conf2)
    fs.rename(new Path(temppath + "/part-00000"), new Path(outputPath))
    println("重命名..........................."+temppath + "/part-00000;----命名为:"+outputPath)
    fs.delete(new Path(temppath), true)

    //清除累加器值班
    accuResultMap.clear()
    sc.stop()
  }
  //计算
  def callsnpCalculation(refArray:Array[Char], readsArray:Array[Char],singlequality:Array[Char],
                         chrname:String,position:Int,beforeend:Double,qvalue:Int,qualitycount:Int,
                         accu:String,accuResult:mutable.LinkedHashMap[String,Array[Int]],
                         beforeBAQ:ArrayBuffer[Double],backBAQ:ArrayBuffer[Double]): Unit ={
    var i = 0
    var status = "M"  //状态
    val readslength = readsArray.length   //M长度

    //------------------------计算比较BAQ------
    while(i < readslength){
      val read= readsArray(i).toString() //read Bases
      if(read.equals("_")) {//read为_,当前ref与read下一个比
        status = "D"
      }
      if(status=="M"){
        val quality= singlequality.charAt(i).toInt-qvalue  //quality
        val  ref= refArray(i).toString()  //ref Bases
        val keyid=chrname+" "+(i+position)+" "+ref

        val befBAQ = beforeBAQ(i+1)
        val bacBAQ = backBAQ(backBAQ.length - 1 - i)
        val m = 1 - (befBAQ * bacBAQ / beforeend)
        var baq = 0.00
        if (m.toDouble > 0.00) {
          val logp = -10 * math.log10(m.toDouble)
          if (logp < quality) {
            //判断取小值
            baq = logp
          } else {
            baq = quality
          }
        }
        //if baq>qualitycount
        if (baq.toDouble >= qualitycount.toDouble) {
          if (accuResult.contains(keyid)) {
            //如果结果集中已包含key
            val keyname = keyid.toString().split(" ")  //StringName
            val t = accuResult.get(keyid).get
            val strmap = containsValue(keyname(0).toString,keyname(1).toInt,keyname(2).toString, read.toString,t)
            accuResult.put(keyid,strmap)
          } else {
            val strmap = containsValueInitinize(chrname,(i+position).toInt,ref,read)
            accuResult.put(keyid,strmap)
          }

        }
      }
    }
  }


  //初始
  def containsValueInitinize(keyname:String,pos: Int,ref:String,read:String): Array[Int] ={
    val keyArray =Array(1,0,0,0,0,0)
    if(read.trim().equals("A")){
      keyArray(1)=1
    }else if(read.trim().equals("T")){
      keyArray(2)=1
    }else if(read.trim().equals("C")){
      keyArray(3)=1
    }else if(read.trim().equals("G")){
      keyArray(4)=1
    }else{
      //为N
      keyArray(5)=1
    }
    keyArray
  }
  //比对结果加入map
  def containsValue(keyname:String,pos: Int,ref:String,read:String,t: Array[Int]): Array[Int] ={
    //如果已存在,修改总统计次数和ATCGN的次数
    t(0)=t(0)+1
    if(read.trim().equals("A")){
      t(1)=t(1)+1
    }else if(read.trim().equals("T")){
      t(2)=t(2)+1
    }else if(read.trim().equals("C")){
      t(3)=t(3)+1
    }else if(read.trim().equals("G")){
      t(4)=t(4)+1
    }else{
      //为N
      t(5)=t(5)+1
    }
    t
  }

}
