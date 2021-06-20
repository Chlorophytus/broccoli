// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Multiplexes input color channels to one output
  *
  * @param numComponents Number of components to mux
  * @param use3b Use 3 bits per channel if true
  */
class ShaderSwizzle(
    numComponents: Int,
    use3b: Boolean
) extends Module {
  val log2Components = log2Ceil(numComponents)
  val io = if (use3b) {
    // Using 3 bits
    IO(new Bundle {
      val aresetn = Input(Bool())
      val from = Input(UInt(log2Components.W))
      val input = for (i <- 0 to numComponents) yield Input(UInt(3.W))
      val output = Output(UInt(3.W))
    })
  } else {
    // Using 2 bits
    IO(new Bundle {
      val aresetn = Input(Bool())
      val from = Input(UInt(log2Components.W))
      val input = for (i <- 0 to numComponents) yield Input(UInt(2.W))
      val output = Output(UInt(2.W))
    })
  }

  withReset(~io.aresetn) {
    io.output := MuxLookup(
      io.from,
      0.U,
      for (i <- 0 to numComponents) yield i.U(log2Components.W) -> io.input(i)
    )
  }
}
