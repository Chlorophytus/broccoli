package broccoli

import chisel3._
import chisel3.util._

/** Full encoder lane for DVI.
  *
  * NOTE: This module should be clocked at 125.875MHz, 0° phase
  */
class TMDSLane extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val clockP = Input(Clock()) // pixel clock, 25.175MHz, 0° phase
    val clockD90 = Input(Clock()) // DDR clock, 125.875MHz, -90° phase

    // Input
    val tmdsIDataEnable = Input(Bool())
    val tmdsIData = Input(UInt(8.W))
    val tmdsIControl = Input(UInt(2.W))

    // Output
    val tmdsOData = Output(Bool())
  })

  withReset(!io.aresetn) {
    val mEncoder = Module(new TMDSEncoder)
    val mDDR = Module(new DDR)
    val rState = RegInit(1.U(5.W))
    val rSendToDDR = RegInit(0.U(2.W))

    mEncoder.clock := io.clockP
    mEncoder.io.aresetn := io.aresetn
    mEncoder.io.enable := io.enable
    mEncoder.io.tmdsIDataEnable := io.tmdsIDataEnable
    mEncoder.io.tmdsIData := io.tmdsIData
    mEncoder.io.tmdsIControl := io.tmdsIControl

    when(!io.aresetn) {
      rState := 1.U(5.W)
    }.elsewhen(io.enable) {
      rState := rState.rotateLeft(1)
    }

    when(!io.aresetn) {
      rSendToDDR := 0.U(2.W)
    }.elsewhen(rState(0)) {
      rSendToDDR := mEncoder.io.tmdsOData(9, 8)
    }.elsewhen(rState(1)) {
      rSendToDDR := mEncoder.io.tmdsOData(7, 6)
    }.elsewhen(rState(2)) {
      rSendToDDR := mEncoder.io.tmdsOData(5, 4)
    }.elsewhen(rState(3)) {
      rSendToDDR := mEncoder.io.tmdsOData(3, 2)
    }.elsewhen(rState(4)) {
      rSendToDDR := mEncoder.io.tmdsOData(1, 0)
    }

    mDDR.io.clock := io.clockD90
    mDDR.io.aresetn := io.aresetn
    mDDR.io.d := rSendToDDR
    io.tmdsOData := mDDR.io.q
  }
}
