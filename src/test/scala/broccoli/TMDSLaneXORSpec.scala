// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class TMDSLaneXORSpec extends AnyFreeSpec with ChiselScalatestTester {
  final val BITS_WIDTH = 8
  "TMDSLaneXOR should perform an exclusive or properly" in {
    test(new TMDSLaneXOR(BITS_WIDTH, false)) { dut =>
      {
        def debugXors(toWhat: Int) = {
          val whatPow = Math.pow(2, toWhat).intValue
          for (cnt <- 0 to (whatPow - 1)) {
            dut.io.input.poke(cnt.U(toWhat.W))
            dut.clock.step(1)
          }
        }
        debugXors(BITS_WIDTH)
      }
    }
  }
  "TMDSLaneXOR should perform an exclusive negated or properly" in {
    test(new TMDSLaneXOR(BITS_WIDTH, true)) { dut =>
      {
        def debugXnors(toWhat: Int) = {
          val whatPow = Math.pow(2, toWhat).intValue
          for (cnt <- 0 to (whatPow - 1)) {
            dut.io.input.poke(cnt.U(toWhat.W))
            dut.clock.step(1)
          }
        }
        debugXnors(BITS_WIDTH)
      }
    }
  }
}
