// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** XY interval driver for VGA type monitors
  */
class VGAIntervalDynamicDriver extends Module {
  final val REGISTERS_LOG2 = 4

  final val R_CTRLREG0 = 0
  final val R_H = 1
  final val R_V = 2
  final val R_HFRONTPORCH = 3
  final val R_VFRONTPORCH = 4
  final val R_HBLANK = 5
  final val R_VBLANK = 6
  final val R_HBACKPORCH = 7
  final val R_VBACKPORCH = 8

  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())

    val hSync = Output(Bool())
    val vSync = Output(Bool())

    val ready = Output(Bool())
    val hBlank = Output(Bool())
    val vBlank = Output(Bool())

    val writeEnable = Input(Bool())
    val writeStrobe = Input(Bool())
    val registerAddress = Input(UInt(REGISTERS_LOG2.W))
    val registerData = Input(UInt(12.W))

    val x = Output(UInt(12.W))
    val y = Output(UInt(12.W))
  })

  withReset(~io.aresetn) {
    val registerMap = RegInit(VecInit.fill(REGISTERS_LOG2 * REGISTERS_LOG2)(0.U(12.W)))
    val x = RegInit(0.U(12.W))
    val y = RegInit(0.U(12.W))
    val hBlank = RegInit(true.B)
    val vBlank = RegInit(true.B)
    val ready = RegInit(false.B)
    val hSync = RegInit(false.B)
    val vSync = RegInit(false.B)

    // JUSTIFICATION FOR RESETTING XY:
    // This only happens when we get new EDID data
    // This event is very rare
    when(io.writeEnable & io.writeStrobe) {
      registerMap(io.registerAddress) := RegNext(io.registerData)
      ready := RegNext(false.B)
    }.otherwise {
      ready := RegNext(true.B)
    }

    
    when(~io.aresetn) {
      hSync := RegNext(false.B)
    }.elsewhen(x === registerMap(R_HBACKPORCH.U(REGISTERS_LOG2.W))) {
      hSync := RegNext(~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0))
    }.elsewhen(x === registerMap(R_H.U(REGISTERS_LOG2.W))) {
      hSync := RegNext(~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0))
    }.elsewhen(x === registerMap(R_HFRONTPORCH.U(REGISTERS_LOG2.W))) {
      hSync := RegNext(registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0))
    }.elsewhen(x === registerMap(R_HBLANK.U(REGISTERS_LOG2.W))) {
      hSync := RegNext(~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0))
    }

    // A BLANK event is signaled by a logical low.
    when(~io.aresetn) {
      hBlank := RegNext(false.B)
    }.otherwise {
      hBlank := RegNext(x < registerMap(R_HBLANK.U(REGISTERS_LOG2.W)))
    }
    when(~io.aresetn) {
      vBlank := RegNext(false.B)
    }.otherwise {
      vBlank := RegNext(y < registerMap(R_VBLANK.U(REGISTERS_LOG2.W)))
    }

    when(~io.aresetn) {
      vSync := RegNext(false.B)
    }.elsewhen(y === registerMap(R_VBACKPORCH.U(REGISTERS_LOG2.W))) {
      vSync := RegNext(~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1))
    }.elsewhen(y === registerMap(R_V.U(REGISTERS_LOG2.W))) {
      vSync := RegNext(~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1))
    }.elsewhen(y === registerMap(R_VFRONTPORCH.U(REGISTERS_LOG2.W))) {
      vSync := RegNext(registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1))
    }.elsewhen(y === registerMap(R_VBLANK.U(REGISTERS_LOG2.W))) {
      vSync := RegNext(~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1))
    }

    when(~io.aresetn | x === registerMap(R_HBACKPORCH.U(REGISTERS_LOG2.W))) {
      x := RegNext(0.U(12.W))
    }.otherwise {
      x := RegNext(x + 1.U(12.W))
    }

    when(~io.aresetn | y === registerMap(R_VBACKPORCH.U(REGISTERS_LOG2.W))) {
      y := RegNext(0.U(12.W))
    }.otherwise {
      y := Mux(x.orR, RegNext(y), RegNext(y + 1.U(12.W)))
    }
    io.x := x
    io.y := y
    io.ready := ready
    io.hBlank := hBlank
    io.vBlank := vBlank
    io.hSync := hSync
    io.vSync := vSync
  }
}
