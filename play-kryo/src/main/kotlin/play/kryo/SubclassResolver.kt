/**
 * *****************************************************************************
 * Copyright 2012 Roman Levenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package play.kryo

import com.esotericsoftware.kryo.Registration
import com.esotericsoftware.kryo.util.DefaultClassResolver
import java.util.*

class SubclassResolver : DefaultClassResolver() {
  private var enabled = false

  private val unregisteredTypes = Collections.newSetFromMap(WeakHashMap<Class<*>, Boolean>())

  fun enable() {
    enabled = true
  }

  private fun findRegistered(clazz: Class<*>?): Optional<Registration> {
    if (clazz == null || unregisteredTypes.contains(clazz))
    // Hit the top, so give up
      return Optional.empty()
    else {
      val reg = classToRegistration.get(clazz)
      return if (reg == null) {
        val result = findRegistered(clazz.superclass).or {
          clazz.interfaces.fold(Optional.empty()) { res, interf ->
            res.or { findRegistered(interf) }
          }
        }
        if (result.isEmpty) {
          unregisteredTypes.add(clazz)
        }
        result
      } else {
        Optional.of(reg)
      }
    }
  }

  override fun getRegistration(type: Class<*>): Registration? {
    val found = super.getRegistration(type)
    if (enabled && found == null) {
      val opt = findRegistered(type)
      return if (opt.isPresent) {
        val registration = opt.get()
        classToRegistration.put(type, registration)
        registration
      } else {
        null
      }
    }
    return null
  }
}
