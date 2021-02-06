// See README.md for license details.
package broccoli

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class VGAIntervalDriverSpec extends FreeSpec with ChiselScalatestTester {
  "VGA should increment properly" in {
    test(new VGAIntervalDriver(new VGAIntervalConstraints {
      val width = 640
      val hFrontPorch = 664
      val hBlank = 720
      val hBackPorch = 800
      val hNegateSync = true

      val height = 480
      val vFrontPorch = 483
      val vBlank = 487
      val vBackPorch = 500
      val vNegateSync = false
    })) { dut =>
      // Knock out timeouts w/seemingly undocumented function call
      dut.clock.setTimeout(0)

      // reset here
      println("testing reset lo...")
      dut.io.aresetn.poke(false.B)
      println("testing reset hi...")
      dut.io.aresetn.poke(true.B)
      // now test

      println("testing X...")
      // hack mechanism: we can just run the test for one line of X
      for (x <- 0 to 799) {
        dut.io.currentX.expect((799 - x).U(12.W))
        dut.clock.step(1)
      }
      dut.io.hBlank.expect(true.B)
      dut.io.currentX.expect(799.U(12.W))
      println("testing X done")

      
      println("testing Y...")
      // and one line of Y to speed it up
      for (y <- 1 to 499) {
        dut.io.currentX.expect(799.U(12.W))
        dut.io.currentY.expect((499 - y).U(12.W))
        dut.clock.step(800)
      }
      println("testing Y done")
    }
  }
}
