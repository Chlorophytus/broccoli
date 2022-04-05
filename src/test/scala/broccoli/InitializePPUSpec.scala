// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class InitializePPUSpec extends AnyFreeSpec with ChiselScalatestTester {
  "should reset the PPU" in {
    test(new InitializePPU) { dut =>
      {
        // reset here
        dut.io.aresetn.poke(false.B)
        dut.io.aresetn.poke(true.B)
        dut.io.enable.poke(true.B)

        println(
          f"at ${dut.io.registerAddress.peek().litValue}: ${dut.io.registerData.peek().litValue}"
        )

        while (dut.io.done.peek().litValue == 0) {
          dut.io.ready.poke(true.B)
          dut.clock.step(1)
          dut.io.ready.poke(false.B)
          dut.clock.step(4)
          println(
            f"at ${dut.io.registerAddress.peek().litValue}: ${dut.io.registerData.peek().litValue}"
          )
        }
      }
    }
  }
}
