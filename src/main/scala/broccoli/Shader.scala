// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class Shader(width: Int) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val address = Input(UInt(width.W))
    val data = Input(UInt(8.W))

    val hBlank = Input(Bool())
    val vBlank = Input(Bool())
    val currentX = Input(UInt(12.W))
    val currentY = Input(UInt(12.W))

    val pixelA = Output(UInt(8.W))
    val pixelB = Output(UInt(8.W))
    val pixelG = Output(UInt(8.W))
    val pixelR = Output(UInt(8.W))

    val debugProgramCounter = Output(UInt(width.W))
  })

  withReset(~io.aresetn) {
    val enable = Reg(Bool())
    val write = Reg(Bool())
    val registerFile = Reg(
      VecInit(
        Array(
          0.U(16.W), // GPR0
          0.U(16.W), // GPR1
          0.U(16.W), // GPR2
          0.U(16.W), // GPR3
          0.U(16.W), // GPR4
          0.U(16.W), // GPR5
          0.U(16.W), // GPR6
          0.U(16.W) // GPR7
        )
      )
    )
    // ========================================================================
    //  I have given you all the modules you need.
    // ========================================================================
    val alu = Module(new ShaderALU())
    val mem = Module(new ShaderMemory(width))
    // ========================================================================
    //  Register File
    // ========================================================================
    for (register <- registerFile) {
      when(~io.aresetn) {
        register := 0.U(16.W)
      }
    }
  }
}
