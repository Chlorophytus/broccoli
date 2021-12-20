// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class PopulationCount(width: Int = 5) extends Module {
  final val log2OutputWidth = log2Ceil(width)
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val input = Input(UInt(width.W))
    val output = Output(UInt(log2OutputWidth.W))
  })

  withReset(~io.aresetn) {
    io.output := MuxLookup(io.input, 0.U(width.W),
      Seq.iterate(0, Math.pow(2, width).intValue)(_ + 1).map({gen =>
        gen.U(width.W) -> BigInt(gen).bitCount.U(log2OutputWidth.W)
      })
    )
  }
}
