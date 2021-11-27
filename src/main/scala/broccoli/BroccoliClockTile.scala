// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util.HasBlackBoxResource

class BroccoliClockTile extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val aresetn = Input(Bool())

    val clockP = Output(Clock())
    val clockF = Output(Clock())
    val clockD = Output(Clock())
  })
  addResource("/vsrc/BroccoliClockTile.v")
}
