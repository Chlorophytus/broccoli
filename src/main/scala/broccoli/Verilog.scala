package broccoli

import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import com.typesafe.scalalogging._

object Verilog extends App with LazyLogging {
  logger.info("Generating Verilog")

  (new circt.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new Broccoli))
  )
}
