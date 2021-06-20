// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Mixes a 3b alpha, a 3b foreground, and a 3b background
  */
class ShaderMix extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val strobe = Input(Bool())

    val alphaInF = Input(UInt(3.W))
    val colorInF = Input(UInt(3.W))
    val colorInB = Input(UInt(3.W))

    val output = Output(UInt(3.W))
    val ready = Output(Bool())
  })
  withReset(~io.aresetn) {
    val state = Reg(UInt(10.W))
    val alphaInF = Reg(UInt(3.W))
    val colorInF = Reg(UInt(3.W))
    val colorInB = Reg(UInt(3.W))
    val mixedResult = Reg(UInt(7.W))
    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn) {
      state := 0.U(4.W)
    }.elsewhen(io.enable & io.strobe & ~state.orR()) {
      state := 1.U(4.W)
    }.elsewhen(io.enable & ~io.strobe) {
      state := state << 1.U(4.W)
    }
    // ========================================================================
    //  Hold Components
    // ========================================================================
    when(~io.aresetn) {
      alphaInF := "b000".U(3.W)
    }.elsewhen(io.enable & state(0)) {
      alphaInF := io.alphaInF
    }
    when(~io.aresetn) {
      colorInF := "b000".U(3.W)
    }.elsewhen(io.enable & state(0)) {
      colorInF := io.colorInF
    }
    when(~io.aresetn) {
      colorInB := "b000".U(3.W)
    }.elsewhen(io.enable & state(0)) {
      colorInB := io.colorInB
    }
    // ========================================================================
    //  Perform Mixing
    // ========================================================================
    // MIX: x * (1 - a) + y * a
    when(~io.aresetn) {
      mixedResult := "b0000000".U(7.W)
    }.elsewhen(io.enable & state(3)) {
      mixedResult := (colorInF * ("b111".U(
        3.W
      ) - alphaInF)) + (colorInB * alphaInF)
    }
    io.output := mixedResult(7, 5)
    io.ready := ~state.orR()
  }
}
