package eksam1.ast.scalar;

import eksam1.ast.VexNode;

public sealed interface VexScalarNode extends VexNode permits VexBinOp, VexDot, VexNum, VexProj, VexSVar {

}
