// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class PopulationCountSpec extends AnyFreeSpec with ChiselScalatestTester {
  final val BITS_WIDTH = 6
  "PopulationCount should count ones properly" in {

    test(new PopulationCount(BITS_WIDTH)) { dut =>

      def debugBinaryByteCounts(toWhat: Int) {
        val whatPow = Math.pow(2, toWhat).intValue
        for (cnt <- 0 to (whatPow - 1)) {
          dut.io.input.poke(cnt.U(toWhat.W))
          dut.clock.step(1)
          // println(f"Population Count at ${dut.io.input.peek} -> ${dut.io.output.peek}")
          dut.io.output.expect(BigInt(cnt).bitCount.U(dut.log2OutputWidth.W))
          dut.clock.step(1)
        }
      }
      // reset here
      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
      dut.clock.step(1)
      debugBinaryByteCounts(BITS_WIDTH)
    }
  }
}
