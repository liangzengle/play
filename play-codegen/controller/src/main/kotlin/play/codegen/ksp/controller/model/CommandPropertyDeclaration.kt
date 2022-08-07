package play.codegen.ksp.controller.model

import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class CommandPropertyDeclaration(val underlying: KSPropertyDeclaration, val cmd: Byte, val dummy: Boolean) :
  KSPropertyDeclaration by underlying
