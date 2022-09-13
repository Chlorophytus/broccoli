// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface
import chiseltest.simulator.VerilatorSimulator
import scala.util._

class TMDSLaneSpec extends AnyFreeSpec with ChiselScalatestTester {
  "should keep cnt stable" in {
    test(new TMDSLane)
      .withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
        {
          // reset here
          dut.io.aresetn.poke(false.B)
          dut.io.aresetn.poke(true.B)
          dut.io.enable.poke(true.B)
          dut.io.dataEnable.poke(true.B)
          dut.clock.setTimeout(0)

          for (i <- (0 until 256)) {
            dut.io.data.poke(Random.nextInt(256))
            dut.clock.step(5)
            val printme = dut.io.out.peek().litValue.intValue.toBinaryString
            println(
              f"invert ${dut.io.debugInvertQM.peek().litValue} xor ${dut.io.debugUseXOR.peek().litValue} counter ${dut.io.debugDisparity.peek().litValue} output ${"0000000000".substring(printme.length)}${printme}"
            )
          }
        }
      }
  }
}
