package eksam1.ast.vector;

import eksam1.ast.VexNode;

public sealed interface VexVectorNode extends VexNode permits VexPlus, VexScale, VexVVar, VexVec {

}
