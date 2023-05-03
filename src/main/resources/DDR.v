module DDR
   (input clock,
    input aresetn,
    input [1:0] d,
    output q);
`ifndef VERILATOR
    ODDR #(
        .DDR_CLK_EDGE("SAME_EDGE"), // "OPPOSITE_EDGE" or "SAME_EDGE"
        .INIT(1'b0),    // Initial value of Q: 1'b0 or 1'b1
        .SRTYPE("ASYNC") // Set/Reset type: "SYNC" or "ASYNC"
    ) ODDR_inst (
        .Q(q),   // 1-bit DDR output
        .C(clock),   // 1-bit clock input
        .CE(1'b1), // 1-bit clock enable input
        .D1(d[1]), // 1-bit data input (positive edge)
        .D2(d[0]), // 1-bit data input (negative edge)
        .R(~aresetn),   // 1-bit reset
        .S(1'b0)    // 1-bit set
    );
`endif
endmodule
