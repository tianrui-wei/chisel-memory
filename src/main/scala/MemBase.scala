package memory

import chisel3._
import chisel3.util._

abstract class MemBase[T <: Data](depth: Int, data : T) {

  val dataWidth: Int = data.asUInt.getWidth

  /** actual chisel module that gets instantiated */
  val mem : Module

  /**
   * @addr: address to read
   * @enable: condition to enable the read
   * */
  def read(addr: UInt, enable: Bool): T = ???

  def write(addr: UInt, data: T, writeMask: Vec[Bool]): Unit = ???

  def write(addr: UInt, data: T, enable: Bool): Unit = ???

}
