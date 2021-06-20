// See README.md for license details.

package broccoli

import chisel3._
import chisel3.util._

class Shader(numComponents: Int = 4, alphaComponent: Int = 3) extends Module {
  val log2Components = log2Ceil(numComponents)
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val enable = Input(Bool())
    val strobe = Input(Bool())
    val ready = Output(Bool())

    val componentsB =
      for (i <- 0 to numComponents)
        yield new ShaderComponentBundle(numComponents, false)

    val componentsF =
      for (i <- 0 to numComponents)
        yield new ShaderComponentBundle(numComponents, true)
  })

  withReset(~io.aresetn) {
    val state = Reg(UInt(10.W))
    // ========================================================================
    // State Machine
    // ========================================================================
    when(~io.aresetn) {
      state := 0.U(10.W)
    }.elsewhen(io.enable & io.strobe & ~state.orR()) {
      state := 1.U(10.W)
    }.elsewhen(io.enable & ~io.strobe) {
      state := state << 1.U(10.W)
    }
    // ========================================================================
    // Multiple components of the same kind are present
    // ========================================================================
    val componentsB =
      for (i <- 0 to numComponents)
        yield new ShaderComponentBundle(numComponents, false)
    val componentsF =
      for (i <- 0 to numComponents)
        yield new ShaderComponentBundle(numComponents, true)
    val swizzleB =
      for (i <- 0 to numComponents)
        yield Module(new ShaderSwizzle(numComponents, true))
    val swizzleF =
      for (i <- 0 to numComponents)
        yield Module(new ShaderSwizzle(numComponents, true))
    // NOTE: The synthesis tool will probably optimize out unused alpha mix
    val mix =
      for (i <- 0 to numComponents)
        yield Module(new ShaderMix)

    for (i <- 0 to numComponents) yield {
      for (j <- 0 to numComponents) {
        swizzleF(i).io.input(j) := componentsF(j).in
        swizzleB(i).io.input(j) := componentsB(j).in
      }
      // Foreground
      when(~io.aresetn) {
        componentsF(i).in := 0.U(3.W)
      }.elsewhen(io.enable & state(0)) {
        componentsF(i).in := io.componentsF(i).in
      }
      when(~io.aresetn) {
        componentsF(i).swizzle := 0.U(log2Components.W)
      }.elsewhen(io.enable & state(0)) {
        componentsF(i).swizzle := io.componentsF(i).swizzle
      }
      // Background
      swizzleB(i).io.aresetn := io.aresetn
      if (i != alphaComponent) {
        when(~io.aresetn) {
          componentsB(i).in := 0.U(3.W)
        }.elsewhen(io.enable & state(0)) {
          componentsB(i).in := io.componentsB(i).in
        }
        when(~io.aresetn) {
          componentsB(i).swizzle := 0.U(log2Components.W)
        }.elsewhen(io.enable & state(0)) {
          componentsB(i).swizzle := io.componentsB(i).swizzle
        }
        swizzleB(i).io.from := componentsB(i).swizzle
      } else {
        componentsB(i).in := 0.U(3.W)
        componentsB(i).swizzle := i.U(log2Components.W)
        swizzleB(i).io.from := componentsB(i).swizzle
      }
      // Mixing
      if (i != alphaComponent) {
        mix(i).io.aresetn := io.aresetn
        mix(i).io.enable := io.enable
        mix(i).io.strobe := state(4)
        mix(i).io.alphaInF := swizzleF(alphaComponent).io.output
        mix(i).io.colorInF := swizzleF(i).io.output
        mix(i).io.colorInB := swizzleB(i).io.output
      } else {
        // Tie low so we can try to optimize it away.
        mix(i).io.aresetn := false.B
        mix(i).io.enable := false.B
        mix(i).io.strobe := false.B
        mix(i).io.alphaInF := 0.U(3.W)
        mix(i).io.colorInF := 0.U(3.W)
        mix(i).io.colorInB := 0.U(3.W)
      }
      when(~io.aresetn) {
        componentsF(i).out := 0.U(3.W)
      }.elsewhen(io.enable & state(9)) {
        if (i != alphaComponent) {
          componentsF(i).out := mix(i).io.output
        } else {
          componentsF(i).out := swizzleF(i).io.output
        }
      }
      io.componentsF(i).out := componentsF(i).out
    }
    io.ready := ~state.orR()
  }
}
