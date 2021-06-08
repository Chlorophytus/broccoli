// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class Downscaler(factor: Int) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val currentX = Input(UInt(12.W))
    val currentY = Input(UInt(12.W))
    val downscaledX = Output(UInt(12.W))
    val downscaledY = Output(UInt(12.W))
  })

  withReset(~io.aresetn) {
    val holdX = Reg(UInt(12.W))
    val holdY = Reg(UInt(12.W))
    when(~io.aresetn) {
      holdX := 0.U(12.W)
    } otherwise {
      holdX := (io.currentX >> factor)
    }
    when(~io.aresetn) {
      holdY := 0.U(12.W)
    } otherwise {
      holdY := (io.currentY >> factor)
    }

    io.downscaledX := holdX
    io.downscaledY := holdY
  }
}
