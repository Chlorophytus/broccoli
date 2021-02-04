package broccoli
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

object Verilog extends App {
  Console.println("Generating Verilog")

  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new Broccoli()))
  )
}
