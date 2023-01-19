package memory

import chisel3._
import chisel3.util.log2Ceil

/** single port ram is conceptually like a `Reg` because of the read first write
  * mode
  * i.e. the data out is the old value
  *
  * @param dataWidth
  *   : the width of data in bits
  * @param byteWriteWidth
  *   : how many bits to write each bit in write mask (wea), should default to 8
  * @param addrWidth
  *   : the request of the width to request all locations
  * @param numberOfLines
  *   : how wide is the request (to cover all lines)
  * @param memoryPrimitive
  *   : should I use auto, block ram or distributed ram
  */
class SinglePortMemory(
    dataWidth: Int = 32,
    addrWidth: Int,
    numberOfLines: Int,
    maskWidth: Int
) extends BlackBox(
      Map(
        "ADDR_WIDTH" -> addrWidth,
        "DATA_WIDTH" -> dataWidth,
        "DEPTH" -> numberOfLines,
        "MASK_WIDTH" -> maskWidth
      )
    ) {
  override def desiredName: String = "fpv_sp_mem"

  val io = IO(new Bundle {
    require(addrWidth <= 20, "request width should be 1 to 20")
    require(
      addrWidth == log2Ceil(numberOfLines),
      "request width should be log 2 of number of lines to request all"
    )
    // clock and reset
    val clk = Input(Clock())
    val rst_n = Input(Reset())

    val addr_i = Input(UInt(addrWidth.W))
    val write_enable = Input(Bool())
    val read_enable = Input(
      Bool()
    ) // not actually used, just an assertion statement. We mux the addr in the previous stage
    val write_data = Input(UInt(dataWidth.W))
    val read_data_o = Output(UInt(dataWidth.W))
    val write_mask_u = Input(
      UInt(maskWidth.W)
    )
  })
}
