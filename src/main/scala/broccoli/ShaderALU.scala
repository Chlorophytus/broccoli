// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class ShaderALU extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val enable = Input(Bool())
    val strobe = Input(Bool())
    val data = Input(SInt(16.W))
    val constant = Input(SInt(16.W))
    val opcode = Input(UInt(8.W))
    val clearFlags = Input(UInt(8.W))

    val ready = Output(Bool())
    val result = Output(SInt(16.W))
    val flags = Output(UInt(8.W))
  })

  withReset(~io.aresetn) {
    val state = Reg(UInt(5.W))
    val opcode = Reg(UInt(8.W))
    val result = Reg(SInt(32.W))
    val resultOutput = Reg(SInt(16.W))

    val flagZero = Reg(Bool())
    val flagSign = Reg(Bool())
    val flagSatP = Reg(Bool())
    val flagSatN = Reg(Bool())
    val flagDivZ = Reg(Bool())

    // ========================================================================
    //  STATE MACHINE
    // ========================================================================
    when(~io.aresetn) {
      state := 0.U(5.W)
    }.elsewhen(io.enable & io.strobe & ~state.orR()) {
      state := 1.U(5.W)
    }.elsewhen(io.enable) {
      state := state << 1.U(5.W)
    }
    // ========================================================================
    // DATA AND CONSTANT
    // ========================================================================
    when(~io.aresetn) {
      constant := 0.S(16.W)
    }.elsewhen(io.enable & state(0)) {
      constant := io.constant
    }
    when(~io.aresetn) {
      data := 0.S(16.W)
    }.elsewhen(io.enable & state(0)) {
      data := io.data
    }
    // ========================================================================
    // OPCODES
    // ========================================================================
    when(~io.aresetn) {
      opcode := 0.U(4.W)
    }.elsewhen(io.enable & state(0)) {
      opcode := io.opcode
    }
    when(~io.aresetn) {
      result := 0.S(32.W)
    }.elsewhen(io.enable & state(1)) {
      switch(opcode) {
        is(0x01.U(8.W)) { result := Cat(0x0000.S(16.W), data) } // NOP OP
        is(0x02.U(8.W)) {
          result := Cat(0x0000.S(16.W), data) +% Cat(0x0000.S(16.W), constant)
        } // ADD OP
        is(0x04.U(8.W)) {
          result := Cat(0x0000.S(16.W), data) -% Cat(0x0000.S(16.W), constant)
        } // SUB OP
        is(0x08.U(8.W)) {
          result := Cat(0x0000.S(16.W), data) * Cat(0x0000.S(16.W), constant)
        } // MUL OP
        is(0x10.U(8.W)) {
          result := Mux(
            constant.orR(),
            Cat(0x0000.S(16.W), data) / Cat(
              0x0000.S(16.W),
              constant,
              0x0000.U(32.W)
            )
          )
        } // DIV OP
        is(0x20.U(8.W)) {
          result := Cat(0x0000.S(16.W), data) & Cat(0x0000.S(16.W), constant)
        } // AND OP
        is(0x40.U(8.W)) {
          result := Cat(0x0000.S(16.W), data) | Cat(0x0000.S(16.W), constant)
        } // ORR OP
        is(0x80.U(8.W)) {
          result := Cat(0x0000.S(16.W), data) ^ Cat(0x0000.S(16.W), constant)
        } // EOR OP
      }
    }
    // ========================================================================
    // SATURATING ARITHMETIC STAGE
    // ========================================================================
    when(~io.aresetn) {
      resultOutput := 0.S(16.W)
    }.elsewhen(io.enable & state(3)) {
      switch(Cat(result(31), result(30, 15).orR())) {
        is("b01".S(2.W)) { resultOutput := 0x7fff.S(16.W) }
        is("b11".S(2.W)) { resultOutput := -0x7fff.S(16.W) }
        is("b00".S(2.W)) { resultOutput := Cat(result(31), result(14, 0)) }
        is("b10".S(2.W)) { resultOutput := Cat(result(31), result(14, 0)) }
      }
    }
    // ========================================================================
    // FLAG REGISTERS
    // ========================================================================
    // zero flag
    when(~io.aresetn) {
      flagZero := false.B
    }.elsewhen(io.enable & state(4) & ~resultOutput.orR()) {
      flagZero := true.B
    }.elsewhen(io.enable & io.clearFlags(1)) {
      flagZero := false.B
    }
    // sign flag
    when(~io.aresetn) {
      flagSign := false.B
    }.elsewhen(io.enable & state(4) & resultOutput(15)) {
      flagSign := true.B
    }.elsewhen(io.enable & io.clearFlags(2)) {
      flagSign := false.B
    }
    // divz flag
    when(~io.aresetn) {
      flagDivZ := false.B
    }.elsewhen(io.enable & state(4) & ~constant.orR() & opcode(4)) {
      flagDivZ := true.B
    }.elsewhen(io.enable & io.clearFlags(3)) {
      flagDivZ := false.B
    }
    // satp flag
    when(~io.aresetn) {
      flagSatP := false.B
    }.elsewhen(io.enable & state(4) & (~result(31) & result(30, 15).orR())) {
      flagSatP := true.B
    }.elsewhen(io.enable & io.clearFlags(4)) {
      flagSatP := false.B
    }
    // satn flag
    when(~io.aresetn) {
      flagSatN := false.B
    }.elsewhen(io.enable & state(4) & (result(31) & result(30, 15).orR())) {
      flagSatN := true.B
    }.elsewhen(io.enable & io.clearFlags(5)) {
      flagSatN := false.B
    }

    io.result := resultOutput
    io.flags(0) := true.B
    io.flags(1) := flagZero
    io.flags(2) := flagSign
    io.flags(3) := flagDivZ
    io.flags(4) := flagSatP
    io.flags(5) := flagSatN
    io.flags(6) := false.B
    io.flags(7) := false.B
  }
}
