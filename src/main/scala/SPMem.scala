package memory

import chisel3._
import chisel3.util._
import chisel3.internal.naming.chiselName
import chisel3.experimental.AffectsChiselPrefix
import memory.SinglePortMemoryWrapper
import memory.MemBase

class SPMem[T <: Data](depth: Int, data : T) extends MemBase[T](depth, data) {

  val mem = Module(new SinglePortMemoryWrapper(depth, dataWidth))

  override def read(addr: UInt, enable: Bool): T = {
    mem.io.readAddr := addr
    mem.io.readEnable := enable
    mem.io.readData.asTypeOf(data)
  }
}

class SPMemMasked[T <: Data](depth: Int, data : T) extends SPMem[T](depth, data) {


  override def write(addr: UInt, da: T, writeMask: Vec[Bool]): Unit = {
    mem.io.writeAddr := addr
    mem.io.writeEnable := true.B
    mem.io.writeMask := writeMask
    mem.io.writeData := da.asUInt
  }
}


class SPMemUnmasked[T <: Data](depth: Int, data : T) extends SPMem[T](depth, data) {

  override def write(addr: UInt, da: T, enable: Bool): Unit = {
    mem.io.writeAddr := addr
    mem.io.writeEnable := enable
    mem.io.writeMask := Seq.fill(dataWidth/8)(true.B)
    mem.io.writeData := da.asUInt
  }
}

object SPMem {
  def apply[T <: Data](depth: Int, data: T): SPMem[T] = {
    data match {
      case v: Vec[_] =>new SPMemMasked(depth, data)
      case _ => new SPMemUnmasked(depth, data)
    }
  }
}
