package memory

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util._
import chisel3.util.random.GaloisLFSR
import memory.SinglePortMemory
import firrtl.options.TargetDirAnnotation
import chisel3.experimental.AffectsChiselPrefix

class SinglePortMemoryWrapper(depth: Int, width: Int = 32, maskWidth: Int) extends Module with AffectsChiselPrefix {
  require(isPow2(depth))
  val addrLen = log2Ceil(depth)

  val io = IO(new Bundle {
    val readAddr = Input(UInt(addrLen.W))
    val writeAddr = Input(UInt(addrLen.W))
    val readEnable = Input(Bool())
    val writeEnable = Input(Bool())
    val writeMask = Input(Vec(maskWidth, Bool()))
    val writeData = Input(UInt(width.W))
    val readData = Output(UInt(width.W))
  })

    val bank = Module(
      new SinglePortMemory(
        dataWidth = width,
        addrWidth = addrLen,
        numberOfLines = depth,
        maskWidth = maskWidth
      )
    )

    bank.io.clk := clock
    bank.io.rst_n := !(reset.asBool)
    bank.io.addr_i := Mux(io.writeEnable, io.writeAddr, io.readAddr)
    bank.io.write_data := io.writeData
    bank.io.write_enable := io.writeEnable
    bank.io.read_enable := io.readEnable
    bank.io.write_mask_u := io.writeMask.asUInt
    io.readData := bank.io.read_data_o
}

// this is why blackboxing doesn't work. It will generate two different modules we need to fill
class SRM(depth: Int, width: Int = 32) extends Module with AffectsChiselPrefix {
  val mem = SyncReadMem(depth, UInt(32.W))
  val mem2 = SyncReadMem(depth, Vec(32, UInt(32.W)))
  mem.write(0.U, 0.U)
  mem2.write(0.U, VecInit.fill(32)(0.U))
}

class SPMTest(depth: Int, width: Int = 32) extends Module with AffectsChiselPrefix {
  val io = IO(new Bundle{
      val ret = Output(UInt(5.W))
      val retVec = Output(Vec(2, UInt(8.W)))
  })
  val rand5: UInt = GaloisLFSR.maxPeriod(5)
  val rand8: UInt = GaloisLFSR.maxPeriod(8)
  val rand32: UInt = GaloisLFSR.maxPeriod(32)
  val spm = SPMem(depth, UInt(width.W))
  val spmVec = SPMem(depth, Vec(2, UInt(8.W)))
  io.ret := spm.read(rand5, rand5(0) === 1.U)
  io.retVec := spmVec.read(rand8, rand8(0) === 1.U)
  //io.retVec := Seq.fill(2)(0.U(8.W))
  spm.write(rand5, rand32, rand5(0) === 0.U)
  spmVec.write(rand5, VecInit(Seq.fill(2)(0.U(8.W))), VecInit(Seq.fill(2)(false.B)), rand5(0) === 0.U)
}

object SinglePortMemoryWrapperElaborate extends App {
  (new ChiselStage).execute(
    Array("-gmv", "blackbox"),
    Seq(ChiselGeneratorAnnotation(() => new SPMTest(32, 32)), TargetDirAnnotation("generation"))
    //Seq(ChiselGeneratorAnnotation(() => new SinglePortMemoryWrapper(32, 32)), TargetDirAnnotation("generation"))
    //Seq(ChiselGeneratorAnnotation(() => new SRM(32, 32)), TargetDirAnnotation("generation"))
  )
}
