// See README.md for license details
package broccoli

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class PopulationCountSpec extends FreeSpec with ChiselScalatestTester {
  "PopulationCount should count ones properly" in {
    test(new PopulationCount(6)) { dut =>
      // reset here
      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
    }
  }
}
