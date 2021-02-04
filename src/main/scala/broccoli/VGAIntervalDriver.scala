// See README.md for license details.

package broccoli

import chisel3._

/** X/Y interval driver for VGA monitors
  */
class VGAIntervalDriver(constraints: VGAIntervalConstraints) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val hSync = Output(Bool())
    val vSync = Output(Bool())
    val hBlank = Output(Bool())
    val vBlank = Output(Bool())

    val currentX = Output(UInt(12.W))
    val currentY = Output(UInt(12.W))
  })

  // withClockAndReset is currently very broken
  withReset(io.aresetn) {
    // val holdX = Reg(constraints.hBackPorch.U(12.W) - 1.U(12.W))
    // val holdY = Reg(constraints.vBackPorch.U(12.W) - 1.U(12.W))
    val holdX = Reg(UInt(12.W))
    val holdY = Reg(UInt(12.W))

    // interval for X axis
    when(~io.aresetn | ~holdX.orR()) {
      holdX := constraints.hBackPorch.U(12.W) - 1.U(12.W)
    } otherwise {
      holdX := holdX - 1.U(12.W)
    }

    // now for the Y axis we need to make sure X is 0
    when(~io.aresetn | ~holdX.orR()) {
      when(~io.aresetn | ~holdY.orR()) {
        holdY := constraints.vBackPorch.U(12.W) - 1.U(12.W)
      } otherwise {
        holdY := holdY - 1.U(12.W)
      }
    }

    io.vBlank := holdY > constraints.height.U(12.W)
    io.hBlank := holdX > constraints.width.U(12.W)
    io.vSync := ((holdY < constraints.vBlank.U(12.W)) && (holdY >= constraints.vFrontPorch.U(12.W))) ^ (constraints.vNegateSync.B)
    io.hSync := ((holdX < constraints.hBlank.U(12.W)) && (holdX >= constraints.hFrontPorch.U(12.W))) ^ (constraints.hNegateSync.B)
    io.currentX := holdX
    io.currentY := holdY
  }
}
