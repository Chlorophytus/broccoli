// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS encoder, requires a shift register to serialize data
  */
class TMDSLane extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val dataEnable = Input(Bool())
    val data = Input(UInt(8.W))
    val control = Input(UInt(2.W))
    val out = Output(UInt(10.W))
    val debugCounter = Output(SInt(12.W))
  })

  withReset(~io.aresetn) {
    val mXorNegative = Module(new TMDSLaneXOR(8, true))
    val mXorPositive = Module(new TMDSLaneXOR(8, false))
    val mState = Module(new OHFiniteStateMachine(10))
    val counter = RegInit(0.S(12.W))
    val popCountData = RegInit(0.U(4.W))
    val data = RegInit(0.U(8.W))
    val dataEnable = RegInit(false.B)
    val intermediateData = RegInit(0.U(9.W))
    val popCountIntermediateP = RegInit(0.S(6.W))
    val popCountIntermediateN = RegInit(0.S(6.W))
    val intermediateCond0 = RegInit(false.B)
    val intermediateCond1 = RegInit(false.B)
    val control = RegInit(0.U(2.W))
    val outClearing =
      RegInit(
        VecInit(
          "b1101010100".U(10.W), // 00
          "b0010101011".U(10.W), // 01
          "b0101010100".U(10.W), // 10
          "b1010101011".U(10.W) // 11
        )
      )
    val out = RegInit(0.U(10.W))
    //State
    mState.clock := clock
    mState.io.aresetn := io.aresetn
    mState.io.enable := io.enable
    // XNOR
    mXorNegative.clock := clock
    mXorNegative.io.aresetn := io.aresetn
    mXorNegative.io.input := data
    // XOR
    mXorPositive.clock := clock
    mXorPositive.io.aresetn := io.aresetn
    mXorPositive.io.input := data
    // Outputs
    io.out := out
    io.debugCounter := counter

    // lock on
    when(~io.aresetn) {
      data := 0.U(8.W)
    }.elsewhen(io.enable && mState.io.output(0)) {
      data := io.data
    }
    when(~io.aresetn) {
      dataEnable := 0.U(8.W)
    }.elsewhen(io.enable && mState.io.output(0)) {
      dataEnable := io.dataEnable
    }
    when(~io.aresetn) {
      control := 0.U(2.W)
    }.elsewhen(io.enable && mState.io.output(0)) {
      control := io.control
    }
    // get the POPCNT
    when(~io.aresetn) {
      popCountData := 0.U(4.W)
    }.elsewhen(io.enable && mState.io.output(1)) {
      popCountData := PopCount(data)
    }
    // XOR or XNOR?
    when(~io.aresetn) {
      intermediateData := 0.U(9.W)
    }.elsewhen(io.enable && mState.io.output(2)) {
      intermediateData := Mux(
        ((popCountData > 4.U(
          4.W
        )) || (popCountData === 4.U(4.W) && ~data(0))),
        mXorNegative.io.output,
        mXorPositive.io.output
      )
    }
    // then POPCNT of intermediates, and its inverse
    when(~io.aresetn) {
      popCountIntermediateP := 0.S(6.W)
    }.elsewhen(io.enable && mState.io.output(3)) {
      popCountIntermediateP := Cat(
        0.U(2.W),
        PopCount(intermediateData(7, 0))
      ).asSInt
    }
    when(~io.aresetn) {
      popCountIntermediateN := 0.S(6.W)
    }.elsewhen(io.enable && mState.io.output(3)) {
      popCountIntermediateN := Cat(
        0.U(2.W),
        PopCount(~intermediateData(7, 0))
      ).asSInt
    }
    // N1(q_m[0:7]) === N0(q_m[0:7]) or ~(|counter)
    when(~io.aresetn) {
      intermediateCond0 := false.B
    }.elsewhen(io.enable && mState.io.output(4)) {
      intermediateCond0 := (~(counter.asUInt.orR)) || (popCountIntermediateN === popCountIntermediateP)
    }
    // If intermediateCond0 false then:
    // ((counter > 0) and (N1(q_m[0:7]) > N0(q_m[0:7]))) or
    // ((counter < 0) and (N1(q_m[0:7]) < N0(q_m[0:7])))
    when(~io.aresetn) {
      intermediateCond1 := false.B
    }.elsewhen(io.enable && mState.io.output(5) && ~intermediateCond0) {
      intermediateCond1 := ((counter > 0.S(
        12.W
      )) && (popCountIntermediateP > popCountIntermediateN)) || ((counter < 0.S(
        12.W
      )) && (popCountIntermediateP < popCountIntermediateN))
    }
    // counter
    when(~io.aresetn || ~dataEnable) {
      counter := 0.S(12.W)
    }.elsewhen(
      io.enable && dataEnable && mState.io.output(7) && intermediateCond0
    ) {
      counter := counter + Mux(
        intermediateData(8),
        popCountIntermediateP - popCountIntermediateN,
        popCountIntermediateN - popCountIntermediateP
      )
    }.elsewhen(
      io.enable && dataEnable && mState.io.output(7) && ~intermediateCond0
    ) {
      counter := Mux(
        intermediateCond1,
        counter + (intermediateData(8) * 2.S(12.W)) + (popCountIntermediateN - popCountIntermediateP),
        counter - (~intermediateData(8) * 2.S(12.W)) + (popCountIntermediateP - popCountIntermediateN)
      )
    }
    // output
    when(~io.aresetn) {
      out := 0.U(10.W)
    }.elsewhen(
      io.enable && dataEnable && mState.io.output(9) && ~intermediateCond0
    ) {
      out := Mux(
        intermediateCond1,
        Cat(true.B, intermediateData(8), ~intermediateData(7, 0)),
        Cat(false.B, intermediateData(8), intermediateData(7, 0))
      )
    }.elsewhen(io.enable && dataEnable && mState.io.output(9)) {
      out := Cat(
        ~intermediateData(8),
        intermediateData(8),
        Mux(
          intermediateData(8),
          intermediateData(7, 0),
          ~intermediateData(7, 0)
        )
      )
    }.elsewhen(io.enable && ~dataEnable && mState.io.output(9)) {
      out := outClearing(control)
    }
  }
}
