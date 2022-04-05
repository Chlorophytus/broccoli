// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class InitializePPU extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())

    val writeEnable = Output(Bool())
    val writeStrobe = Output(Bool())
    val registerAddress = Output(UInt(4.W))
    val registerData = Output(UInt(12.W))
    val ready = Input(Bool())

    val done = Output(Bool())
  })

  withReset(~io.aresetn) {
    // 640x480 settings
    val registerRom =
      VecInit(
        Seq(
          "b0000000000".U(12.W),
          (640).U(12.W),
          (640 + 16).U(12.W),
          (640 + 16 + 96).U(12.W),
          (640 + 16 + 96 + 48).U(12.W),
          (480).U(12.W),
          (480 + 10).U(12.W),
          (480 + 10 + 2).U(12.W),
          (480 + 10 + 2 + 33).U(12.W),
          0.U(12.W),
          0.U(12.W),
          0.U(12.W),
          0.U(12.W),
          0.U(12.W),
          0.U(12.W),
          0.U(12.W)
        )
      )

    val currentRegister = RegInit(0.U(4.W))
    val shift = RegInit(false.B)
    val writeData = RegInit(0.U(12.W))
    val writeEnable = RegInit(false.B)
    val writeStrobe = RegInit(false.B)
    val done = RegInit(false.B)

    when(~io.aresetn) {
      done := false.B
    }.otherwise {
      done := RegNext(currentRegister > 8.U(4.W))
    }

    when(~io.aresetn) {
      shift := false.B
    }.elsewhen(io.enable && ~done) {
      shift := RegNext(~shift)
    } .otherwise {
      shift := RegNext(false.B)
    }

    when(~io.aresetn) {
      currentRegister := 0.U(4.W)
    }.elsewhen(io.enable && io.ready && ~done) {
      currentRegister := RegNext(currentRegister + 1.U(4.W))
    }

    io.writeEnable := ~done
    io.writeStrobe := shift
    io.registerAddress := currentRegister
    io.registerData := registerRom(currentRegister)
    io.done := done
  }
}
