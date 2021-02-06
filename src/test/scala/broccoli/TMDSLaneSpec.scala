// See README.md for license details.
package broccoli

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface
import scala.util.Random

class TMDSLaneSpec extends FreeSpec with ChiselScalatestTester {
  "TMDS should deviate properly" in {
    test(new TMDSLane) { dut =>
      // Knock out timeouts w/seemingly undocumented function call
      dut.clock.setTimeout(0)

      // reset here
      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
      dut.io.de.poke(true.B)
      dut.io.c.poke(0.U(2.W))
      // now test

      val cnts = new Array[BigInt](8192)

      for (cnt <- 0 to cnts.length - 1) {
        dut.io.d.poke(Random.nextInt(256).U(8.W))
        dut.clock.step()
        dut.clock.step()
        dut.clock.step()
        dut.clock.step()
        dut.clock.step()
        cnts(cnt) = dut.io.debugCnt.peek().litValue()
        println(f"TMDS[${cnt}] = ${cnts(cnt)}")
      }
      println(f"TMDS cnt mean is ${cnts.sum.toDouble / cnts.length}")
    }
  }
}
