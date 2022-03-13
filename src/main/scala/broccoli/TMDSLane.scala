// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS (Transition Minimized Differential Signal) lane
  *
  * useful for DVI video signalling
  */
class TMDSLane extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val enable = Input(Bool())

    val de = Input(Bool())
    val d = Input(UInt(8.W))
    val c = Input(UInt(2.W))

    val qq = Output(UInt(2.W))

    val debugCnt = Output(SInt(12.W))
    val debugQOut = Output(UInt(10.W))
  })
  withReset(~io.aresetn) {
    val mState = Module(new OHFiniteStateMachine(5))
    val mXOR = Module(new TMDSLaneXOR(8, false))
    val mXNOR = Module(new TMDSLaneXOR(8, true))
    val cnt = RegInit(0.S(12.W))
    val retrain = RegInit(true.B)
    val out = RegInit(0.U(5.W))
    val qM = RegInit(0.U(9.W))
    val d = RegInit(0.U(8.W))
    val qq = RegInit(0.U(2.W))
    val n1m = RegInit(0.S(4.W))
    val n0m = RegInit(0.S(4.W))
    val nextOut = RegInit(0.U(10.W))

    mState.io.aresetn := io.aresetn
    mState.io.enable := io.enable

    mXOR.io.aresetn := io.aresetn
    mXNOR.io.aresetn := io.aresetn
    mXOR.io.input := d
    mXNOR.io.input := d

    switch(mState.io.output) {
      is("b00001".U(5.W)) {
        retrain := ~io.de
        d := io.d
      }
      is("b00010".U(5.W)) {
        when(~retrain) {
          qM := Mux(
            (PopCount(d) > 4.U) || (PopCount(d) === 4.U && ~d(0)),
            mXNOR.io.output,
            mXOR.io.output
          )
          n0m := PopCount(~qM(7, 0)).asSInt
          n1m := PopCount(qM(7, 0)).asSInt
        }
      }
      is("b00100".U(5.W)) {
        when(retrain) {
          switch(io.c) {
            is(0.U(2.W)) { out := "b1101010100".U }
            is(1.U(2.W)) { out := "b0010101011".U }
            is(2.U(2.W)) { out := "b0101010100".U }
            is(3.U(2.W)) { out := "b1010101011".U }
          }
        }.elsewhen((cnt === 0.S(12.W)) || (n1m === n0m)) {
          out := Cat(~qM(8), qM(8), Mux(qM(8), qM(7, 0), ~qM(7, 0)))
        }.elsewhen(
          ((cnt > 0
            .S(12.W)) && (n1m > n0m)) || ((cnt < 0.S(12.W)) && (n0m > n1m))
        ) {
          out := Cat(true.B, qM(8), ~qM(7, 0))
        }.otherwise {
          out := Cat(false.B, qM(8), qM(7, 0))
        }
      }
      is("b01000".U(5.W)) {
        when(retrain) {
          cnt := 0.S(12.W)
        }.elsewhen((cnt === 0.S(12.W)) || (n1m === n0m)) {
          cnt := cnt + Mux(
            qM(8),
            n1m - n0m,
            n0m - n1m
          )
        }.elsewhen(
          ((cnt > 0
            .S(12.W)) && (n1m > n0m)) || ((cnt < 0.S(12.W)) && (n0m > n1m))
        ) {
          cnt := cnt + ((qM(8) * 2.S(10.W)) + (n0m - n1m))
        }.otherwise {
          cnt := cnt + (-(~qM(8) * 2.S(10.W)) + (n1m - n0m))
        }
      }
      is("b10000".U(5.W)) {
        nextOut := out
      }
    }
    switch(mState.io.output) {
      is("b00001".U(5.W)) {
        qq := nextOut(1, 0)
      }
      is("b00010".U(5.W)) {
        qq := nextOut(3, 2)
      }
      is("b00100".U(5.W)) {
        qq := nextOut(5, 4)
      }
      is("b01000".U(5.W)) {
        qq := nextOut(7, 6)
      }
      is("b10000".U(5.W)) {
        qq := nextOut(9, 8)
      }
    }
    io.qq := qq
    io.debugCnt := cnt
    io.debugQOut := nextOut
  }
}
