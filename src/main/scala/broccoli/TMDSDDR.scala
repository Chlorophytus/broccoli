// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util.HasBlackBoxResource

class TMDSDDR extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val aresetn = Input(Bool())

    val d = Input(UInt(2.W))
    val q = Output(Bool())
  })
  addResource("/vsrc/TMDSDDR.v")
}