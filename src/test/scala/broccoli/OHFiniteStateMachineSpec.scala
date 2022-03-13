// See README.md for license details

package broccoli

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.internal.BackendInterface

class OHFiniteStateMachineSpec extends AnyFreeSpec with ChiselScalatestTester {
  final val BITS_WIDTH = 5
  "should work properly in a TMDSLane" in {
    test(new OHFiniteStateMachine(BITS_WIDTH)) { dut =>
      {
        // reset here
        dut.io.aresetn.poke(false.B)
        dut.io.aresetn.poke(true.B)
        dut.io.enable.poke(true.B)
        dut.io.output.expect(1.U(BITS_WIDTH.W), "state[0]")
        dut.clock.step(1)
        dut.io.output.expect(2.U(BITS_WIDTH.W), "state[1]")
        dut.clock.step(1)
        dut.io.output.expect(4.U(BITS_WIDTH.W), "state[2]")
        dut.clock.step(1)
        dut.io.output.expect(8.U(BITS_WIDTH.W), "state[3]")
        dut.clock.step(1)
        dut.io.output.expect(16.U(BITS_WIDTH.W), "state[4]")
        dut.clock.step(1)
        dut.io.output.expect(1.U(BITS_WIDTH.W), "state[0] again")
        dut.clock.step(1)
      }
    }
  }
}
