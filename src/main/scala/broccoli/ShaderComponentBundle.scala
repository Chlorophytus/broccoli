// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

/** Contains contents for a component, which is usually A, B, G, or R.
  */
class ShaderComponentBundle(swizzleWidth: Int, hasOutput: Boolean = true)
    extends Bundle {
  val in = Input(UInt(3.W))
  val swizzle = Input(UInt(swizzleWidth.W))
  val out = if (hasOutput) {
    Output(UInt(3.W))
  } else {
    null
  }
}
