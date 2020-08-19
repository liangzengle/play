package play.codegen.ksp.controller.model

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class CommandFunctionDeclaration(val underlying: KSFunctionDeclaration, val cmd: Byte, val dummy: Boolean) :
  KSFunctionDeclaration by underlying

