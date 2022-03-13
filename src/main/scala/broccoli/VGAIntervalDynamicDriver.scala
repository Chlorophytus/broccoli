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
    val ready = Output(Bool())

    val hSync = Output(Bool())
    val vSync = Output(Bool())

    val hBlank = Output(Bool())
    val vBlank = Output(Bool())

    val writeEnable = Input(Bool())
    val registerAddress = Input(UInt(REGISTERS_LOG2.W))
    val registerData = Input(UInt(12.W))

    val x = Output(UInt(12.W))
    val y = Output(UInt(12.W))
  })

  withReset(~io.aresetn) {
    val mState = Module(new OHFiniteStateMachine(5))
    val registerMap = VecInit.fill(REGISTERS_LOG2 * REGISTERS_LOG2)(0.U(12.W))
    val registerAddress = RegInit(0.U(REGISTERS_LOG2.W))
    val registerData = RegInit(0.U(12.W))
    val x = RegInit(0.U(12.W))
    val y = RegInit(0.U(12.W))
    val hBlank = RegInit(true.B)
    val vBlank = RegInit(true.B)
    val ready = RegInit(false.B)
    val hSync = RegInit(false.B)
    val vSync = RegInit(false.B)

    mState.io.aresetn := io.aresetn
    mState.io.enable := io.enable

    switch(mState.io.output) {
      is("b00001".U(5.W)) {
        // JUSTIFICATION FOR RESETTING XY:
        // This only happens when we get new EDID data
        // This event is very rare
        when(io.writeEnable) {
          x := 0.U(12.W)
          y := 0.U(12.W)
          registerAddress := io.registerAddress
          registerData := io.registerData
          registerMap(registerAddress) := registerData
          ready := false.B
        }.otherwise {
          ready := true.B
        }
      }
      is("b00010".U(5.W)) {}
      is("b00100".U(5.W)) {
        when(ready) {
          when(x === registerMap(R_HBACKPORCH.U(REGISTERS_LOG2.W))) {
            x := 0.U(12.W)
            y := y + 1.U(12.W)
            hSync := ~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0)
          }.otherwise {
            switch(x) {
              is(registerMap(R_H.U(REGISTERS_LOG2.W))) {
                hSync := ~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0)
              }
              is(registerMap(R_HFRONTPORCH.U(REGISTERS_LOG2.W))) {
                hSync := registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0)
              }
              is(registerMap(R_HBLANK.U(REGISTERS_LOG2.W))) {
                hSync := ~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(0)
              }
            }
            x := x + 1.U(12.W)
          }
        }
      }
      is("b01000".U(5.W)) {
        when(ready) {
          when(y === registerMap(R_VBACKPORCH.U(REGISTERS_LOG2.W))) {
            y := 0.U(12.W)
            vSync := ~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1)
          }.otherwise {
            switch(x) {
              is(registerMap(R_V.U(REGISTERS_LOG2.W))) {
                vSync := ~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1)
              }
              is(registerMap(R_VFRONTPORCH.U(REGISTERS_LOG2.W))) {
                vSync := registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1)
              }
              is(registerMap(R_VBLANK.U(REGISTERS_LOG2.W))) {
                vSync := ~registerMap(R_CTRLREG0.U(REGISTERS_LOG2.W))(1)
              }
            }
            y := y + 1.U(12.W)
          }
        }
      }
      is("b10000".U(5.W)) {
        when(ready) {
          // A BLANK event is signaled by a logical low.
          hBlank := x < registerMap(R_HBLANK.U(REGISTERS_LOG2.W))
          vBlank := y < registerMap(R_VBLANK.U(REGISTERS_LOG2.W))
        }
      }
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
