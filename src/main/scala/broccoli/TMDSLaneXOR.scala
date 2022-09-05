// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** TMDS lane XOR or XNOR module
  *
  * @param width the width of the INPUT, or the output's width minus 1
  * @param negate true = XNOR, false = XOR
  */
class TMDSLaneXOR(width: Int, negate: Boolean) extends Module {
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val input = Input(UInt(width.W)) // data input
    val output = Output(UInt((width + 1).W)) // intermediates
  })

  def calculateXors(n: Int) = {
    VecInit(
      ((n & 1).B +: Seq
        .iterate(1, width - 1)(_ + 1)
        .map(acc => {
          ((n & (1 << (acc - 1))) != 0).B ^ ((n & (1 << (acc - 0))) != 0).B
        })) :+
        true.B
    ).asUInt
  }

  def calculateXnors(n: Int) = {
    VecInit(
      ((n & 1).B +: Seq
        .iterate(1, width - 1)(_ + 1)
        .map(acc => {
          ((n & (1 << (acc - 1))) != 0).B ^ ((n & (1 << (acc - 0))) == 0).B
        })) :+
        false.B
    ).asUInt
  }

  withReset(~io.aresetn) {
    val lookup = VecInit(
      Seq
        .iterate(0, Math.pow(2, width).intValue)(_ + 1)
        .map(if (negate) calculateXnors(_) else calculateXors(_))
    )
    io.output := lookup(io.input)
  }
}
