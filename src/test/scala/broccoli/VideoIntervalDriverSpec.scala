package broccoli

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chisel3.experimental.BundleLiterals._
import com.typesafe.scalalogging._

class VideoIntervalDriverSpec
    extends AnyFreeSpec
    with ChiselScalatestTester
    with LazyLogging {
  "VideoIntervalDriver X should work properly at 640x480 60Hz" in {
    test(new VideoIntervalDriver(new VideoIntervalData {
      val width = 640
      val height = 480
      val vFrontPorch = 10
      val hFrontPorch = 16
      val vBlank = 2
      val hBlank = 96
      val vBackPorch = 33
      val hBackPorch = 48
      val vNegateSync = true
      val hNegateSync = true
    })) { dut =>
      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
      dut.io.enable.poke(true.B)

      for (i <- (0 until 800)) {
        dut.io.x.expect(i)
        dut.clock.step(1)
      }
    }
  }
  "VideoIntervalDriver Y should work properly at 640x480 60Hz" in {
    test(new VideoIntervalDriver(new VideoIntervalData {
      val width = 640
      val height = 480
      val vFrontPorch = 10
      val hFrontPorch = 16
      val vBlank = 2
      val hBlank = 96
      val vBackPorch = 33
      val hBackPorch = 48
      val vNegateSync = true
      val hNegateSync = true
    })) { dut =>
      dut.clock.setTimeout(0)

      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
      dut.io.enable.poke(true.B)

      logger.info("Test X steadiness")
      for (i <- (0 until 525)) {
        dut.io.x.expect(0)
        dut.clock.step(800)
      }

      logger.info("Test Y increment")
      for (i <- (0 until 525)) {
        dut.io.y.expect(i)
        dut.clock.step(800)
      }
    }
  }
  "VideoIntervalDriver X syncing should work properly at 640x480 60Hz" in {
    test(new VideoIntervalDriver(new VideoIntervalData {
      val width = 640
      val height = 480
      val vFrontPorch = 10
      val hFrontPorch = 16
      val vBlank = 2
      val hBlank = 96
      val vBackPorch = 33
      val hBackPorch = 48
      val vNegateSync = true
      val hNegateSync = true
    })) { dut =>
      dut.io.aresetn.poke(false.B)
      dut.io.aresetn.poke(true.B)
      dut.io.enable.poke(true.B)
      for (i <- (0 until 800)) {
        val cond1 = i < (640 + 16 - 1)
        val cond2 = i >= (640 + 16 + 96 - 1)
        // logger.info(f"X is $i, cond1 $cond1 cond2 $cond2")
        dut.io.hSync.expect(cond1 || cond2)
        dut.clock.step(1)
      }
    }
  }
}
