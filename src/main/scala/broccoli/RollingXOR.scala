// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Accumulating XOR/XNOR
  *
  * @param useXNOR use XNOR if true, otherwise use XOR
  * @param width the width of the input
  */
class RollingXOR(useXNOR: Boolean, width: Int) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val wen = Input(Bool())
    val input = Input(UInt(width.W))
    val output = Input(UInt((width + 1).W))
  })

  def foldXOR(in: UInt, out: UInt, step: Int): UInt = step match {
    // Output the result when the last step is reached
    case `width` => out
    // First we set to 0 to start the fold
    case 0 => {
      out(0) := in(0)
      foldXOR(in, out, 1)
    }
    // All other cases the algorithm takes effect
    case default => {
      if (useXNOR) {
        out(step) := out(step - 1) ^ ~in(step)
      } else {
        out(step) := out(step - 1) ^ in(step)
      }
      foldXOR(in, out, step + 1)
    }
  }

  withReset(~io.aresetn) {
    val outputReg = RegInit(0.U((width + 1).W))
    val inputReg = RegInit(0.U(width.W))
    when(~io.aresetn) {
      outputReg := 0.U((width + 1).W)
    }.elsewhen(io.enable & ~io.wen) {
      foldXOR(inputReg, outputReg, 0)
    }

    when(~io.aresetn) {
      inputReg := 0.U(width.W)
    }.elsewhen(io.enable & io.wen) {
      inputReg := io.input
    }

    io.output := outputReg
  }
}
