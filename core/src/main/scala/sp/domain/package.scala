package sp

import org.json4s.JsonAST.JValue
import org.json4s._

/**
 * Created by kristofer on 15-05-27.
 */
package object domain {

  type SPAttributes = JObject
  //val SPAttributes = JObject
  type SPValue = JValue
  //val SPValue = JValue

  object SPAttributes {
    def apply[T](pair: (String, T)*)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]): SPAttributes = {
      val res = pair.map{
        case (key, value) => key -> SPValue(value)
      }
      JObject(res.toList)
    }
    def apply() = JObject()
    def apply(fs: List[JField]): JObject = JObject(fs.toList)
  }

  object SPValue {
    def apply[T](v: T)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]): SPValue = {
      Extraction.decompose(v)
    }
    def apply(s: String) = JString(s)
    def apply(i: Int) = JInt(i)
    def apply(b: Boolean) = JBool(b)
  }

}
