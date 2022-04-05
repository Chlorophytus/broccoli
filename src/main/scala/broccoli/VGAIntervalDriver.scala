// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class VGAIntervalDriver extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val x = Output(UInt(12.W))
    val y = Output(UInt(12.W))
    val vSync = Output(Bool())
    val hSync = Output(Bool())
    val vblankn = Output(Bool())
    val hblankn = Output(Bool())
  })

  withReset(~io.aresetn) {
    val x = RegInit(0.U(12.W))
    val y = RegInit(0.U(12.W))
    val hSync = RegInit(true.B)
    val vSync = RegInit(true.B)

    when(io.enable && (x >= 799.U(12.W))) {
      x := 0.U(12.W)
    }.elsewhen(io.enable) {
      x := x + 1.U(12.W)
    }

    when(io.enable && (y >= 524.U(12.W))) {
      y := 0.U(12.W)
    }.elsewhen(io.enable && (x === 0.U(12.W))) {
      y := y + 1.U(12.W)
    }

    when(io.enable && ~(x.orR)) {
      hSync := true.B
    }.elsewhen(io.enable && hSync && (x === (640 + 16).U(12.W))) {
      hSync := false.B
    }.elsewhen(io.enable && (x === (640 + 16 + 96).U(12.W))) {
      hSync := true.B
    }
    when(io.enable && ~(y.orR)) {
      vSync := true.B
    }.elsewhen(io.enable && vSync && (y === (480 + 10).U(12.W))) {
      vSync := false.B
    }.elsewhen(io.enable && (y === (480 + 10 + 2).U(12.W))) {
      vSync := true.B
    }

    io.hSync := hSync
    io.vSync := vSync
    io.x := x
    io.y := y
    io.hblankn := x < 640.U(12.W)
    io.vblankn := y < 480.U(12.W)
  }
}
