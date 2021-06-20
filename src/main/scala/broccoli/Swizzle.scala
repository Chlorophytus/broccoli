// See README.md for license details.

package broccoli

import chisel3._

/** Multiplexes input color channels to one output
  * 
  * @param use3b Use 3 bits per channel if true
  */
class Swizzle(use3b: Boolean) extends Module {
  val io = if (use3b) {
    IO(new Bundle {
      val aresetn = Input(Bool())
      val from = Input(UInt(2.W))
      val swizzleInputA = Input(UInt(3.W))
      val swizzleInputB = Input(UInt(3.W))
      val swizzleInputG = Input(UInt(3.W))
      val swizzleInputR = Input(UInt(3.W))
      val swizzledResult = Output(UInt(3.W))
    })
  } else {
    IO(new Bundle {
      val aresetn = Input(Bool())
      val from = Input(UInt(2.W))
      val swizzleInputA = Input(UInt(2.W))
      val swizzleInputB = Input(UInt(2.W))
      val swizzleInputG = Input(UInt(2.W))
      val swizzleInputR = Input(UInt(2.W))
      val swizzledResult = Output(UInt(2.W))
    })
  }

  withReset(~io.aresetn) {
    io.swizzledResult := Mux(
      io.from(1),
      Mux(io.from(0), io.swizzleInputR, io.swizzleInputG),
      Mux(io.from(0), io.swizzleInputB, io.swizzleInputA)
    )
  }
}
