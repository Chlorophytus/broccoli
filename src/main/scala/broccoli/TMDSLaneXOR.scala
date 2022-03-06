package broccoli

import chisel3._
import chisel3.util._

/** TMDS lane XOR or XNOR module
  *
  * @param negate true = XNOR, false = XOR
  */
class TMDSLaneXOR(negate: Boolean) extends Module {
  final val width = 8
  val io = IO(new Bundle {
    val aresetn = Input(Bool())

    val data = Input(UInt(width.W)) // data input
    val qm = Output(UInt((width + 1).W)) // intermediates
  })

  def calculateXors(n: Int) = {
    val xors = 0.U((width + 1).W)
    if (!negate) { xors(width) := true.B }
    xors(0) := (n & 1).B
    for (bit <- (1 until width)) {
      if (negate) {
        xors(bit) := ((n & (1 << (bit - 1))).B ^ ~(n & (1 << (bit - 0))).B)
      } else {
        xors(bit) := ((n & (1 << (bit - 1))).B ^ (n & (1 << (bit - 0))).B)
      }
    }
    xors(width) := ~negate.B
    xors
  }

  withReset(~io.aresetn) {
    val lookup = VecInit(
      Seq
        .iterate(0, Math.pow(2, width).intValue)(_ + 1)
        .map(calculateXors(_))
    )

    io.qm := lookup(io.data)
  }
}
