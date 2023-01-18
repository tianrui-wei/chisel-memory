package memory

import chisel3._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util._
import chisel3.util.random.GaloisLFSR
import memory.SinglePortMemory
import firrtl.options.TargetDirAnnotation
import chisel3.experimental.AffectsChiselPrefix

class SinglePortMemoryWrapper(depth: Int, width: Int = 32, masked: Boolean = false) extends Module with AffectsChiselPrefix {
  require(isPow2(depth))
  require(
    width % 8 == 0,
    "if memory is byte addressable, then the adderss width must be a multiple of 8"
  )
  val addrLen = log2Ceil(depth)
  val dataMaskWidth = width / 8

  val io = IO(new Bundle {
    val readAddr = Input(UInt(addrLen.W))
    val writeAddr = Input(UInt(addrLen.W))
    val readEnable = Input(Bool())
    val writeEnable = Input(Bool())
    val writeMask = Input(Vec(dataMaskWidth, Bool()))
    val writeData = Input(UInt(width.W))
    val readData = Output(UInt(width.W))
  })

    val bank = Module(
      new SinglePortMemory(
        dataWidth = width,
        addrWidth = addrLen,
        numberOfLines = depth,
        masked = masked
      )
    )

    bank.io.clk := clock
    bank.io.rst_n := !(reset.asBool)
    bank.io.addr := Mux(io.writeEnable, io.writeAddr, io.readAddr)
    bank.io.write_data := io.writeData
    bank.io.write_enable := io.writeEnable
    bank.io.read_enable := io.readEnable
    io.readData := bank.io.read_data
}

// this is why blackboxing doesn't work. It will generate two different modules we need to fill
class SRM(depth: Int, width: Int = 32) extends Module with AffectsChiselPrefix {
  val mem = SyncReadMem(depth, UInt(32.W))
  val mem2 = SyncReadMem(depth, Vec(32, UInt(32.W)))
  mem.write(0.U, 0.U)
  mem2.write(0.U, VecInit.fill(32)(0.U))
}

class SPMTest(depth: Int, width: Int = 32) extends Module with AffectsChiselPrefix {
  val rand5: UInt = GaloisLFSR.maxPeriod(5)
  val rand32: UInt = GaloisLFSR.maxPeriod(32)
  val spm = SPMem(32, UInt(8.W))
  val ret = spm.read(rand5, rand5(0) === 1.U)
  spm.write(rand5, rand32, rand5(0) === 0.U)
}

object SinglePortMemoryWrapperElaborate extends App {
  (new ChiselStage).execute(
    Array("-gmv", "blackbox"),
    Seq(ChiselGeneratorAnnotation(() => new SPMTest(32, 32)), TargetDirAnnotation("generation"))
    //Seq(ChiselGeneratorAnnotation(() => new SinglePortMemoryWrapper(32, 32)), TargetDirAnnotation("generation"))
    //Seq(ChiselGeneratorAnnotation(() => new SRM(32, 32)), TargetDirAnnotation("generation"))
  )
}
