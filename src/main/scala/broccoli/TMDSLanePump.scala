// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** 5-to-1 serdes
  *
  * 10 data to 2 data, can be used for ODDR. Fork if you need a 1-data version.
  */
class TMDSLanePump extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val wen = Input(Bool())
    val ready = Output(Bool())

    val d = Input(UInt(10.W))
    val qq = Output(UInt(2.W))
  })
  withReset(~io.aresetn) {
    val shiftReady = RegInit(false.B)

    // Take the ten then keep shifting out.
    val qqShifter = RegInit(0.U(10.W))
    when(~io.aresetn) {
      qqShifter := 0.U(10.W)
    }.elsewhen(io.wen) {
      qqShifter := io.d
    }.otherwise {
      qqShifter := qqShifter.rotateRight(2)
    }

    when(~io.aresetn | io.wen) {
      shiftReady := false.B
    }.otherwise {
      shiftReady := ShiftRegister(true.B, 5, false.B, true.B)
    }

    // 2LSBs are extracted.
    val qq = RegInit(0.U(2.W))
    when(~io.aresetn) {
      qq := 0.U(2.W)
    }.otherwise {
      qq := qqShifter(1, 0)
    }

    io.ready := shiftReady
    io.qq := qq
  }
}
