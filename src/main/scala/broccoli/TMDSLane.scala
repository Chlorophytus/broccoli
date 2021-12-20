// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class TMDSLane extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val de = Input(Bool())
    val d = Input(UInt(8.W))
    val c = Input(UInt(2.W))

    val qq = Output(UInt(2.W))

    val debugCnt = Output(SInt(12.W))
    val debugQWhich = Output(Bool())
    val debugQOut = Output(UInt(10.W))
  })

  withReset(~io.aresetn) {
    // TODO: reimplement a lot of stuff...
  }
}
