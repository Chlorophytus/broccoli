// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface
import chiseltest.simulator.VerilatorSimulator
class VGAIntervalDriverSpec extends AnyFreeSpec with ChiselScalatestTester {
  "should handle properly the X and Y coords" in {
    test(new VGAIntervalDriver)
      .withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
        {
          // reset here
          dut.io.aresetn.poke(false.B)
          dut.io.aresetn.poke(true.B)
          dut.io.enable.poke(true.B)
          dut.clock.setTimeout(0)

          for (i <- (0 until 525)) {
            for (j <- (0 until 800)) {
              println(
                f"xy ${dut.io.x.peek().litValue} ${dut.io.y.peek().litValue} ${dut.io.hSync
                  .peek()
                  .litValue} ${dut.io.vSync.peek().litValue}"
              )
              dut.clock.step(1)
            }
          }
        }
      }
  }
}
