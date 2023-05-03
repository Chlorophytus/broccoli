package broccoli

import chisel3._
import chisel3.util._

/** DDR register for DVI 10-to-1 SerDes.
  */
class DDR extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val aresetn = Input(Bool())

    val d = Input(UInt(2.W))
    val q = Output(Bool())
  })
  addResource("DDR.v")
}
