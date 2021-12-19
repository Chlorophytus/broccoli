// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class PopulationCount(width: Int = 5) extends Module {
  val log2OutputWidth = log2Ceil(width)
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val input = Input(UInt(width.W))
    val output = Output(UInt(log2OutputWidth.W))
  })

  private def generateLookupEntry(Int generating) {
    var result = 0
    for (bit <- 0 to width) {
      if (generating & (1 << bit)) {
        result += 1
      }
    }
    is(generating.U(width.W)) {
      io.output := result.U(log2OutputWidth.W)
    }
  }

  withReset(~io.aresetn) {
    switch(io.input) {
      for(i <- 0 to (1 << width)) { yield generateLookupEntry(i) }
    }
  }
}
