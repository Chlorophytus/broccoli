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
    val currFramebuffer = Output(Bool())

    val currentX = Output(UInt(12.W))
    val currentY = Output(UInt(12.W))
  })

  withReset(~io.aresetn) {
    // val holdX = Reg(constraints.hBackPorch.U(12.W) - 1.U(12.W))
    // val holdY = Reg(constraints.vBackPorch.U(12.W) - 1.U(12.W))
    val holdX = Reg(UInt(12.W))
    val holdY = Reg(UInt(12.W))
    val allZero = Reg(Bool())
    val currFramebuffer = Reg(Bool())

    // interval for X axis
    when(~io.aresetn) {
      holdX := constraints.hBackPorch.asUInt(12.W) - 1.U(12.W)
    } otherwise {
      holdX := Mux(
        ~holdX.orR(),
        constraints.hBackPorch.asUInt(12.W) - 1.U(12.W),
        holdX -% 1.U(12.W)
      )
    }

    // now for the Y axis we need to make sure X is 0
    when(~io.aresetn) {
      holdY := constraints.vBackPorch.asUInt(12.W) - 1.U(12.W)
    }.elsewhen(~holdX.orR()) {
      holdY := Mux(
        ~holdY.orR(),
        constraints.vBackPorch.asUInt(12.W) - 1.U(12.W),
        holdY -% 1.U
      )
    }

    when(~io.aresetn) {
      allZero := false.B
    } otherwise {
      allZero := (~holdY.orR) &( ~holdX.orR)
    }
    when(~io.aresetn) {
      currFramebuffer := true.B
    } .elsewhen(allZero) {
      currFramebuffer := ~currFramebuffer
    }

    io.hBlank := holdX > constraints.width.asUInt(12.W)
    io.vBlank := holdY > constraints.height.asUInt(12.W)
    io.vSync := ((holdY < constraints.vBlank.asUInt(
      12.W
    )) && (holdY >= constraints.vFrontPorch.asUInt(
      12.W
    ))) ^ (constraints.vNegateSync.asBool())
    io.hSync := ((holdX < constraints.hBlank.asUInt(
      12.W
    )) && (holdX >= constraints.hFrontPorch.asUInt(
      12.W
    ))) ^ (constraints.hNegateSync.asBool())
    io.currentX := holdX
    io.currentY := holdY
    io.currFramebuffer := currFramebuffer
  }
}
